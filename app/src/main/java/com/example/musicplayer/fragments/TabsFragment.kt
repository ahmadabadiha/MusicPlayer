package com.example.musicplayer.fragments

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentTabsBinding
import com.example.musicplayer.model.AudioModel
import com.example.musicplayer.viewmodel.SharedViewModel
import com.example.musicplayer.viewpageradapter.ViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator


class TabsFragment : Fragment(R.layout.fragment_tabs) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentTabsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTabsBinding.bind(view)
        sharedViewModel.audioList = getAllAudioFromDevice(requireContext())
        binding.viewPager.apply {
            adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "All songs"
                1 -> tab.text = "Albums"
                2 -> tab.text = "Artists"
            }
        }.attach()
    }

    private fun getAllAudioFromDevice(context: Context): List<AudioModel> {
        val tempAudioList: MutableList<AudioModel> = ArrayList()
        val projection =
            arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST)
        val c: Cursor? =
            context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)
        val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        if (c != null) {
            while (c.moveToNext()) {
                val mmr = android.media.MediaMetadataRetriever()
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
                mmr.setDataSource(c.getString(0))
                val coverImage = mmr.embeddedPicture
                val audioModel = AudioModel(
                    c.getString(0),
                    c.getString(1) ?: "unknown",
                    c.getString(2) ?: "unknown",
                    c.getString(3) ?: "unknown",
                    coverImage
                )
                tempAudioList.add(audioModel)
            }
            c.close()
        }
        return tempAudioList
    }
}

