package com.example.musicplayer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class AudioModel(val path: String,val title: String, val album: String, val artist: String, val coverImage: ByteArray?)


@Parcelize
data class ParcelableAudioModel(val path: String,val title: String, val album: String, val artist: String) : Parcelable