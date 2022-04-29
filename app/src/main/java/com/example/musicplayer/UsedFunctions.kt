package com.example.musicplayer

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

fun getAllAudioFromDevice(context: Context): List<AudioModel> {
    val tempAudioList: MutableList<AudioModel> = ArrayList()
    val uri: Uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI
    val projection =
        arrayOf(MediaStore.Audio.AudioColumns.DATA,MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.ArtistColumns.ARTIST)
    val c: Cursor? =
        context.contentResolver.query(uri, projection, null, null, null)

    if (c != null) {
        //Log.d("ali", "getAllAudioFromDevice: " + c.count)
        while (c.moveToNext()) {
            val mmr = android.media.MediaMetadataRetriever()

             mmr.setDataSource(c.getString(0))

            val coverImage = mmr.embeddedPicture
           // Log.d("ali","in fun :" + c.getString(0))

            val audioModel = AudioModel(c.getString(0), c.getString(1), c.getString(2),c.getString(3), coverImage)
            tempAudioList.add(audioModel)
        }
        c.close()
    }
    return tempAudioList
}
