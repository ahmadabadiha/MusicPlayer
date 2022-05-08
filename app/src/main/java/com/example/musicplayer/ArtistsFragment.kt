package com.example.musicplayer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.databinding.FragmentArtistsBinding

class ArtistsFragment : Fragment(R.layout.fragment_artists) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicAdapter: MusicAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArtistsBinding.bind(view)
        musicAdapter = MusicAdapter(::onClick)
        val list = sharedViewModel.audioList.map { audio ->
            audio.artist
        }
        musicAdapter.submitList(list.distinct())
        binding.recyclerView.adapter = musicAdapter
    }

    private fun onClick(input: Int) {
        sharedViewModel.audioList = sharedViewModel.audioList.filter {
            it.artist ==  musicAdapter.currentList[input]
        }
        findNavController().navigate(TabsFragmentDirections.actionTabsFragmentToFilteredSongsFragment(musicAdapter.currentList[input], null))
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
