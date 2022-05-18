package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import com.example.musicplayer.model.AudioModel

class SharedViewModel : ViewModel() {
    lateinit var audioList :List<AudioModel>
}