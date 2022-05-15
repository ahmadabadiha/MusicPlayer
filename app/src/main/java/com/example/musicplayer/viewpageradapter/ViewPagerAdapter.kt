package com.example.musicplayer.viewpageradapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.musicplayer.fragments.AlbumsFragment
import com.example.musicplayer.fragments.ArtistsFragment
import com.example.musicplayer.fragments.SongsFragment

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SongsFragment()
            1 -> AlbumsFragment()
            2 -> ArtistsFragment()
            else -> Fragment()

        }
    }
}