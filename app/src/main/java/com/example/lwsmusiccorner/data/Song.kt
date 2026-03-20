package com.example.lwsmusiccorner.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val year: Int,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null
)
