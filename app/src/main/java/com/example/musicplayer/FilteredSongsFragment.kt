package com.example.musicplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.musicplayer.databinding.FragmentAlbumsBinding
import com.example.musicplayer.databinding.FragmentArtistsBinding
import com.example.musicplayer.databinding.FragmentFilteredSongsBinding

class FilteredSongsFragment : Fragment(R.layout.fragment_filtered_songs) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentFilteredSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicAdapter: MusicAdapter
    private val args: FilteredSongsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFilteredSongsBinding.bind(view)
        musicAdapter = MusicAdapter(::onClick)
        val list = sharedViewModel.audioList.map { audio ->
            audio.title
        }
        musicAdapter.submitList(list.distinct())
        binding.recyclerView.adapter = musicAdapter
    }

    private fun onClick(input: Int) {

        findNavController().navigate(
            FilteredSongsFragmentDirections.actionFilteredSongsFragmentToPlayerFragment(input)
        )
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}