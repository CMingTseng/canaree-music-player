package dev.olog.lib.image.loader.fetcher

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dev.olog.domain.MediaId
import dev.olog.domain.MediaIdCategory
import dev.olog.domain.entity.LastFmArtist
import dev.olog.domain.gateway.ImageRetrieverGateway
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GlideArtistFetcherTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val context = mock<Context>()
    private val artistId = 10L
    private val mediaId = MediaId.Category(MediaIdCategory.ARTISTS, artistId)
    private val gateway = mock<ImageRetrieverGateway>()
    private val sut = GlideArtistFetcher(context, mediaId, gateway, mock())

    @Test
    fun testExecute() = coroutineRule.runBlockingTest {
        // given
        val expectedImage = "image"
        val lastFmArtist = LastFmArtist(
            id = artistId,
            image = expectedImage,
            mbid = "",
            wiki = ""
        )
        whenever(gateway.getArtist(artistId)).thenReturn(lastFmArtist)

        // when
        val image = sut.execute()

        // then
        verify(gateway).getArtist(artistId)
        assertEquals(
            expectedImage,
            image
        )
    }

    @Test
    fun testExecuteLastFmPlaceholder() = coroutineRule.runBlockingTest {
        // given
        val artistImage = GlideArtistFetcher.LAST_FM_PLACEHOLDER
        val lastFmArtist = LastFmArtist(
            id = artistId,
            image = artistImage,
            mbid = "",
            wiki = ""
        )
        whenever(gateway.getArtist(artistId)).thenReturn(lastFmArtist)

        // when
        val image = sut.execute()

        // then
        verify(gateway).getArtist(artistId)
        assertEquals(
            "",
            image
        )
    }

    @Test
    fun testExecuteDeezerPlaceholder() = coroutineRule.runBlockingTest {
        // given
        val artistImage = GlideArtistFetcher.DEEZER_PLACEHOLDER
        val lastFmArtist = LastFmArtist(
            id = artistId,
            image = artistImage,
            mbid = "",
            wiki = ""
        )
        whenever(gateway.getArtist(artistId)).thenReturn(lastFmArtist)

        // when
        val image = sut.execute()

        // then
        verify(gateway).getArtist(artistId)
        assertEquals(
            "",
            image
        )
    }

    @Test
    fun testMustFetchTrue() = coroutineRule.runBlockingTest {
        // given
        whenever(gateway.mustFetchArtist(artistId)).thenReturn(true)

        // when
        val actual = sut.mustFetch()

        // then
        verify(gateway).mustFetchArtist(artistId)
        assertEquals(
            true,
            actual
        )
    }

    @Test
    fun testMustFetchFalse() = coroutineRule.runBlockingTest {
        // given
        whenever(gateway.mustFetchArtist(artistId)).thenReturn(false)

        // when
        val actual = sut.mustFetch()

        // then
        verify(gateway).mustFetchArtist(artistId)
        assertEquals(
            false,
            actual
        )
    }

}