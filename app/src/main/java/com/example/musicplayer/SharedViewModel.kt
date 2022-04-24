package com.example.musicplayer

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    //private lateinit var _audioList: List<AudioModel>
    var audioList = emptyList<AudioModel>()


}