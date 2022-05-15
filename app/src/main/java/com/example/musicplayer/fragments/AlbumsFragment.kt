package com.example.musicplayer.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.*
import com.example.musicplayer.databinding.FragmentAlbumsBinding
import com.example.musicplayer.viewmodel.SharedViewModel

class AlbumsFragment : Fragment(R.layout.fragment_albums) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicAdapter: MusicAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlbumsBinding.bind(view)
        musicAdapter = MusicAdapter(::onClick)
        val list = sharedViewModel.audioList.map { audio ->
            audio.album
        }
        musicAdapter.submitList(list.distinct())
        binding.recyclerView.adapter = musicAdapter
    }

    private fun onClick(input: Int) {

        findNavController().navigate(
            TabsFragmentDirections.actionTabsFragmentToFilteredSongsFragment(
                null,
                musicAdapter.currentList[input]
            )
        )
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
