package com.example.musicplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.musicplayer.databinding.FragmentAlbumsBinding
import com.example.musicplayer.databinding.FragmentSongsBinding

class AlbumsFragment : Fragment(R.layout.fragment_albums) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlbumsBinding.bind(view)
        val myAdapter = MyAdapter(::onClick)
        val list = sharedViewModel.audioList.map { audio ->
            Log.d("ali", "onViewCreated: " + audio.album)
            audio.album
        }
        myAdapter.submitList(list.distinct())
        Log.d("ali", "onViewCreated: " + list.distinct().toString())
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
