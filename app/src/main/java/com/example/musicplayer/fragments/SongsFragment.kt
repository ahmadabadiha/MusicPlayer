package com.example.musicplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.musicplayer.*
import com.example.musicplayer.activities.PlayerActivity
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.viewmodel.SharedViewModel

class SongsFragment : Fragment(R.layout.fragment_songs) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSongsBinding.bind(view)
        val musicAdapter = MusicAdapter(::onClick)
        binding.recyclerView.adapter = musicAdapter
        var titlesList = sharedViewModel.audioList.map { audio ->
            audio.title
        }
        musicAdapter.submitList(titlesList)
    }

    private fun onClick(input: Int) {
        val intent = Intent(requireContext(), PlayerActivity::class.java)
        intent.putExtra("index", input)
        MusicService.mediaList = sharedViewModel.audioList
        startActivity(intent)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
