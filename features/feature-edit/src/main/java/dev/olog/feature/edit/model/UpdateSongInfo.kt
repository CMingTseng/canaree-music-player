package dev.olog.feature.edit.model

import dev.olog.lib.audio.tagger.model.Tags

data class UpdateSongInfo(
    val trackId: Long,
    val path: String,
    val tags: Tags,
    val isPodcast: Boolean
)