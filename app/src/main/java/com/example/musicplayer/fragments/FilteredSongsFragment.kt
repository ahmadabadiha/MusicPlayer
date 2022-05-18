package com.example.musicplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.example.musicplayer.*
import com.example.musicplayer.model.AudioModel
import com.example.musicplayer.activities.PlayerActivity
import com.example.musicplayer.databinding.FragmentFilteredSongsBinding
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.viewmodel.SharedViewModel

class FilteredSongsFragment : Fragment(R.layout.fragment_filtered_songs) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentFilteredSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicAdapter: MusicAdapter
    private val args: FilteredSongsFragmentArgs by navArgs()
    private lateinit var mediaList: List<AudioModel>
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFilteredSongsBinding.bind(view)
        musicAdapter = MusicAdapter(::onClick)
        binding.recyclerView.adapter = musicAdapter
        var list = listOf<String>()
        if (args.album != null) {
            mediaList = sharedViewModel.audioList.filter {
                it.album == args.album
            }
            list = mediaList.map { audio ->
                audio.title
            }
        }
        if (args.artist != null) {
            mediaList = sharedViewModel.audioList.filter {
                it.artist == args.artist
            }
            list = mediaList.map { audio ->
                audio.title
            }
        }

        musicAdapter.submitList(list.distinct())
    }

    private fun onClick(input: Int) {
        MusicService.mediaList = mediaList
        val intent = Intent(requireContext(), PlayerActivity::class.java)
        intent.putExtra("index", input)
        startActivity(intent)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}