package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import com.example.musicplayer.activities.AudioModel

class SharedViewModel : ViewModel() {
    var audioList = emptyList<AudioModel>()
}