package com.example.musicplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.databinding.FragmentSongsBinding

class SongsFragment : Fragment(R.layout.fragment_songs) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSongsBinding.bind(view)
        val myAdapter = MyAdapter(::onClick)
        val list = sharedViewModel.audioList.map {audio->
           audio.title
        }
        myAdapter.submitList(list)
        binding.recyclerView.adapter = myAdapter
    }

    private fun onClick(input: Int){
        findNavController().navigate(TabsFragmentDirections.actionTabsFragmentToPlayerFragment(input))
    }
    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
