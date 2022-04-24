package com.example.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class PlayerViewModel(private val state: SavedStateHandle) : ViewModel() {
    var index = state.get<Int>("index") ?: 0

    lateinit var mediaList: List<AudioModel>

    val isLooping = MutableLiveData<Boolean>(false)

    val repeatAll = MutableLiveData<Boolean>(true)


}