package dev.olog.data.repository.track

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.olog.domain.MediaId
import dev.olog.domain.entity.track.Artist
import dev.olog.domain.entity.track.Folder
import dev.olog.domain.entity.track.Song
import dev.olog.domain.gateway.track.FolderGateway
import dev.olog.domain.gateway.track.TrackGateway
import dev.olog.domain.prefs.BlacklistPreferences
import dev.olog.domain.prefs.SortPreferences
import dev.olog.domain.schedulers.Schedulers
import dev.olog.data.db.FolderMostPlayedDao
import dev.olog.data.mapper.toArtist
import dev.olog.data.mapper.toSong
import dev.olog.data.model.db.FolderMostPlayedEntity
import dev.olog.data.queries.FolderQueries
import dev.olog.data.repository.BaseRepository
import dev.olog.data.repository.ContentUri
import dev.olog.data.utils.getString
import dev.olog.data.utils.queryAll
import dev.olog.shared.android.utils.assertBackgroundThread
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

internal class FolderRepository @Inject constructor(
    @ApplicationContext context: Context,
    sortPrefs: SortPreferences,
    blacklistPrefs: BlacklistPreferences,
    private val trackGateway: TrackGateway,
    private val mostPlayedDao: FolderMostPlayedDao,
    private val schedulers: Schedulers
) : BaseRepository<Folder, String>(context, schedulers), FolderGateway {

    private val queries = FolderQueries(contentResolver, blacklistPrefs, sortPrefs)

    init {
        firstQuery()
    }

    override fun registerMainContentUri(): ContentUri {
        return ContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
    }

    @Suppress("DEPRECATION")
    private fun extractFolders(cursor: Cursor): List<Folder> {
        assertBackgroundThread()
        val pathList = contentResolver.queryAll(cursor) {
            val data = it.getString(MediaStore.Audio.Media.DATA)
            data.substring(0, data.lastIndexOf(File.separator)) // path
        }
        return pathList.asSequence()
            .groupBy { it }
            .entries
            .map { (path, list) ->
                val dirName = path.substring(path.lastIndexOf(File.separator) + 1)
                Folder(
                    title = dirName.capitalize(),
                    path = path,
                    size = list.size
                )
            }.sortedBy { it.title }
    }

    override fun queryAll(): List<Folder> {
        assertBackgroundThread()
        val cursor = queries.getAll(false)
        return extractFolders(cursor)
    }

    override fun getByParam(param: String): Folder? {
        assertBackgroundThread()
        return channel.valueOrNull?.find { it.id == param }
    }

    override fun observeByParam(param: String): Flow<Folder?> {
        return channel.asFlow()
            .map { list -> list.find { it.id == param } }
            .distinctUntilChanged()
            .flowOn(schedulers.cpu)
    }

    override fun getTrackListByParam(param: String): List<Song> {
        assertBackgroundThread()
        val folder = getByParam(param) ?: return emptyList()
        val cursor = queries.getSongList(folder.path)
        return contentResolver.queryAll(cursor) { it.toSong() }
    }

    override fun observeTrackListByParam(param: String): Flow<List<Song>> {
        val contentUri = ContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
        return observeByParamInternal(contentUri) { getTrackListByParam(param) }
            .flowOn(schedulers.cpu)
    }

    override fun getAllBlacklistedIncluded(): List<Folder> {
        assertBackgroundThread()
        val cursor = queries.getAll(true)
        return extractFolders(cursor)
    }

    override fun observeMostPlayed(mediaId: MediaId.Category): Flow<List<Song>> {
        return observeByParam(mediaId.categoryId).take(1).map { it!! }
            .flatMapLatest { mostPlayedDao.observeAll(it.path, trackGateway) }
            .distinctUntilChanged()
            .flowOn(schedulers.cpu)
    }

    override suspend fun insertMostPlayed(mediaId: MediaId.Track) {
        assertBackgroundThread()
        val folder = getByParam(mediaId.categoryId)!!
        mostPlayedDao.insert(
            FolderMostPlayedEntity(
                id = 0,
                songId = mediaId.id.toLong(),
                folderPath = folder.path
            )
        )
    }

    override fun observeSiblings(param: String): Flow<List<Folder>> {
        return observeAll()
            .map { list -> list.filter { it.id != param } }
            .distinctUntilChanged()
            .flowOn(schedulers.cpu)
    }

    override fun observeRelatedArtists(param: String): Flow<List<Artist>> {
        val contentUri = ContentUri(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true)

        return observeByParam(param).take(1).map { it!! }
            .flatMapLatest {
                observeByParamInternal(contentUri) {
                    extractArtists(queries.getRelatedArtists(it.path))
                }
            }
            .distinctUntilChanged()
            .flowOn(schedulers.cpu)
    }

    override fun observeRecentlyAdded(param: String): Flow<List<Song>> {
        val contentUri = ContentUri(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true)
        return observeByParam(param).take(1).map { it!! }
            .flatMapLatest {
                observeByParamInternal(contentUri) {
                    val cursor = queries.getRecentlyAdded(it.path)
                    contentResolver.queryAll(cursor) { it.toSong() }
                }
            }
    }

    private fun extractArtists(cursor: Cursor): List<Artist> {
        assertBackgroundThread()
        return contentResolver.queryAll(cursor) { it.toArtist() }
            .groupBy { it.id }
            .map { (_, list) ->
                val artist = list[0]
                artist.copy(songs = list.size)
            }
    }
}