package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log


fun getAllAudioFromDevice(context: Context): List<AudioModel> {
    val tempAudioList: MutableList<AudioModel> = ArrayList()
    val projection =
        arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST)
    val c: Cursor? =
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)
    val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    if (c != null) {
        while (c.moveToNext()) {
            val mmr = android.media.MediaMetadataRetriever()
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
            mmr.setDataSource(c.getString(0))
            val coverImage = mmr.embeddedPicture
            val audioModel = AudioModel(
                c.getString(0),
                c.getString(1) ?: "unknown",
                c.getString(2) ?: "unknown",
                c.getString(3) ?: "unknown",
                coverImage
            )
            tempAudioList.add(audioModel)
        }
        c.close()
    }
    return tempAudioList
}