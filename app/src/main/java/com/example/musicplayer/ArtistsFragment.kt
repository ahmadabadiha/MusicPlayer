package com.example.musicplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.databinding.FragmentAlbumsBinding
import com.example.musicplayer.databinding.FragmentArtistsBinding

class ArtistsFragment : Fragment(R.layout.fragment_artists) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArtistsBinding.bind(view)
        val myAdapter = MyAdapter(::onClick)
        val list = sharedViewModel.audioList.map { audio ->
            audio.artist
        }
        myAdapter.submitList(list.distinct())
        binding.recyclerView.adapter = myAdapter
    }
    private fun onClick(input: Int){
       // findNavController().navigate(TabsFragmentDirections.actionTabsFragmentToPlayerFragment())
    }
    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}