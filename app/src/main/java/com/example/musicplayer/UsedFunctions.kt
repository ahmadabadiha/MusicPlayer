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
            Log.d("ahmadabadi", "getAllAudioFromDevice: " + c.getString(0))

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
    Log.d("ahmadabadi", "getAllAudioFromDevice: " + tempAudioList.toString())

    return tempAudioList
}
/*
val tempAudioList: MutableList<AudioModel> = ArrayList()
val projection =
    arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST)
val c: Cursor? =
    context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)

if (c != null) {
    while (c.moveToNext()) {
        val mmr = android.media.MediaMetadataRetriever()
        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0))

        mmr.setDataSource(contentUri.path)

        val coverImage = mmr.embeddedPicture
        // Log.d("ali","in fun :" + c.getString(0))
        val audioModel = AudioModel(contentUri, c.getString(1), c.getString(2), c.getString(3), coverImage)
        tempAudioList.add(audioModel)
    }
    c.close()
}
return tempAudioList*/
