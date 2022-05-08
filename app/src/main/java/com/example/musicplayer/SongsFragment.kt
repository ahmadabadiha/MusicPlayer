package com.example.musicplayer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.musicplayer.databinding.FragmentSongsBinding
import java.lang.IllegalStateException

class SongsFragment : Fragment(R.layout.fragment_songs) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSongsBinding.bind(view)
        val musicAdapter = MusicAdapter(::onClick)
        binding.recyclerView.adapter = musicAdapter
        var titlesList = listOf<String>()

        titlesList = sharedViewModel.audioList.map { audio ->
            audio.title
        }
        musicAdapter.submitList(titlesList)
    }

    private fun onClick(input: Int) {
        findNavController().navigate(TabsFragmentDirections.actionTabsFragmentToPlayerFragment(input))
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
