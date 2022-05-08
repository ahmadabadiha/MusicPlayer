package com.example.musicplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.databinding.FragmentPlayerBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class PlayerFragment : Fragment(R.layout.fragment_player) {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var t: Thread
    private var repeatAll = true
    private lateinit var musicService: MusicService
    private var mBound = false
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            Log.d("ahmadabadi", "onServiceConnected")
            mBound = true

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("ahmadabadi", "onServiceDisconnected")

            mBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(context, MusicService::class.java).also { intent ->

            requireActivity().applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().applicationContext.startForegroundService(intent)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)
        if (savedInstanceState == null) {
            playerViewModel.mediaList = sharedViewModel.audioList
        }
        val rotateAnim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnim.repeatCount = Animation.INFINITE
        rotateAnim.duration = 300000
        rotateAnim.start()
        binding.coverImage.startAnimation(rotateAnim)

        lifecycleScope.launch {

            while (mBound == false) {
                delay(10)
            }
            musicService.startPlaying(playerViewModel.mediaList, playerViewModel.index)

            var temp1 = false
            while (true) {
                try {
                    initSetOnclickListeners()
                    initObserveLiveData()
                    binding.seekBar.max = musicService.mediaPlayer.duration
                    updateSeekbar()
                    temp1 = true
                } catch (e: UninitializedPropertyAccessException) {
                    delay(10)
                }
                if (temp1) break
            }

        }

        /* val updateSongTime = object : Runnable {
             var temp2 = false
             override fun run() {
                 Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                 while (true) {
                     try {
                         val mediaPosition = musicService.mediaPlayer.currentPosition
                         binding.timePast.text = computeTime(mediaPosition)
                         binding.seekBar.progress = (mediaPosition / musicService.mediaPlayer.duration).toInt()
                         temp2 = true

                     } catch (e: UninitializedPropertyAccessException) {
                         Thread.sleep(5)
                     }
                     if (temp2) break
                 }
             }

         }
         t = Thread(updateSongTime)
         t.start()*/
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService.mediaPlayer.seekTo(progress)
                }
            }


            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

                if (p0 != null) {
                    val x = (p0.progress / 100 * musicService.mediaPlayer.duration).toInt()
                    musicService.mediaPlayer.seekTo(x)
                    Log.d("ali", "update song" + p0.progress.toString())
                }
                //  mediaPlayer.seekTo(binding.seekBar.progress)
                //  Log.d("ali", "update song" + binding.seekBar.progress.toString())
            }
        })
        /* lifecycleScope.launch {
             updateSeekbar()
         }*/
    }

    private suspend fun updateSeekbar() {
        while (true) {
            val mediaPosition = musicService.mediaPlayer.currentPosition
            binding.seekBar.progress = mediaPosition
                //(mediaPosition / musicService.mediaPlayer.duration).toInt()
            binding.timePast.setText(computeTime(mediaPosition))
            Log.d("ahmadabadi", "updateSeekbar: " + computeTime(mediaPosition))
            delay(1000)
        }
    }

    private fun startMedia() {

        if (playerViewModel.index == playerViewModel.mediaList.size) playerViewModel.index = 0
        if (playerViewModel.index == -1) playerViewModel.index = playerViewModel.mediaList.size - 1

        val media = playerViewModel.mediaList[playerViewModel.index]
        if (media.coverImage != null) {
            val bitmapImage = BitmapFactory.decodeByteArray(media.coverImage, 0, media.coverImage.size)
            binding.coverImage.setImageBitmap(bitmapImage)
        } else binding.coverImage.setImageResource(R.drawable.ic_baseline_music_note_24)
        val uri = Uri.parse(media.path)
        binding.songName.text = media.title
        Log.d("ali", "startMedia: " + media.title)
        mediaPlayer = MediaPlayer.create(requireContext(), uri)
        binding.songTime.text = computeTime(mediaPlayer.duration)
        handleCompletion()
        mediaPlayer.isLooping = playerViewModel.isLooping.value ?: false
        mediaPlayer.start()

    }

    private fun initSetOnclickListeners() {
        binding.playIcon.setOnClickListener {
            //todo handle next song when paused
            // todo handle illegal state exception
            if (musicService.mediaPlayer.isPlaying) {
                it.setBackgroundResource(R.drawable.play)
                musicService.mediaPlayer.pause()
                binding.play.setShapeType(0)
            } else {
                it.setBackgroundResource(R.drawable.pause)
                musicService.mediaPlayer.start()
                binding.play.setShapeType(1)

            }
        }

        binding.nextIcon.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.prepare()
            val temp = musicService.index.value!!
            musicService.index.postValue(handleIndexBound(temp + 1))
            Log.d(
                "ahmadabadi", "next clicked " + musicService.index.value.toString()
            )
            Log.d(
                "ahmadabadi", "next clicked " + musicService.index.hasObservers().toString()
            )
            binding.playIcon.setBackgroundResource(R.drawable.pause)
            binding.play.setShapeType(1)
            musicService.startMedia()
        }
        binding.prevIcon.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.prepare()
            val temp = musicService.index.value!!
            musicService.index.postValue(handleIndexBound(temp - 1))

            Log.d(
                "ahmadabadi", "previous clicked " + musicService.index.value.toString()
            )
            binding.playIcon.setBackgroundResource(R.drawable.pause)
            binding.play.setShapeType(1)
            musicService.startMedia()
        }

        binding.shuffle.setOnClickListener {
            musicService.mediaList = musicService.mediaList.shuffled()
            Toast.makeText(requireContext(), "List of songs shuffled now!", Toast.LENGTH_SHORT).show()
        }

        binding.repeatOne.setOnClickListener {
            val temp = !musicService.mediaPlayer.isLooping
            musicService.mediaPlayer.isLooping = temp
            if (temp) {
                binding.repeatOne.setShapeType(1)
            } else binding.repeatOne.setShapeType(0)
            Log.d(
                "ahmadabadi", "repeat one clicked " + temp
            )
            /* if (mediaPlayer.isLooping) binding.repeatOne.setShapeType(1)
             else binding.repeatOne.setShapeType(0)*/
        }
        binding.repeatAll.setOnClickListener {
            val temp = !musicService.repeatAll
            musicService.repeatAll = temp
            if (temp) binding.repeatAll.setShapeType(1)
            else binding.repeatAll.setShapeType(0)
        }
    }

    private fun handleIndexBound(i: Int): Int {

        if (i == musicService.mediaList.size) return 0
        if (i == -1) return musicService.mediaList.lastIndex
        else return i
    }

    private fun initObserveLiveData() {
        musicService.index.observeForever {
            val song = musicService.mediaList[it]
            if (song.coverImage != null) {
                val bitmapImage = BitmapFactory.decodeByteArray(song.coverImage, 0, song.coverImage.size)
                binding.coverImage.setImageBitmap(bitmapImage)
                binding.backgroundImage.setImageBitmap(bitmapImage)
            } else {
                binding.coverImage.setImageResource(R.drawable.music)
                binding.backgroundImage.setImageResource(R.drawable.music)

            }
            binding.songName.text = song.title
            binding.artist.text = song.artist
            binding.album.text = song.album
            binding.songTime.setText(computeTime(musicService.mediaPlayer.duration))
            musicService.updateNotification()
            Log.d("ahmadabadi", "index observed: index = " + it.toString() + " " + musicService.mediaList[it].title)
        }
    }

    private fun handleCompletion() {
        mediaPlayer.setOnCompletionListener {
            if (mediaPlayer.isLooping) {
                Log.d("ali", "completed is looping")
                startMedia()
            } else {

                playerViewModel.index++
                Log.d("ali", "completed is looping playerViewModel.index" + playerViewModel.index.toString())
                Log.d("ali", "completed is loopingplayerViewModel.mediaList.size" + playerViewModel.mediaList.size.toString())

                if (playerViewModel.index != playerViewModel.mediaList.size && repeatAll) {
                    Log.d("ali", "completed is repeat all")
                    mediaPlayer.stop()
                    mediaPlayer.release()
                    startMedia()
                } else {
                    Log.d("ali", "completed is looping finish")
                    mediaPlayer.release()
                }
            }
        }
    }

    private fun computeTime(l: Int): String {
        return String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(l.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(l.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l.toLong()))
        )
    }


    override fun onStop() {
        requireActivity().unbindService(connection)
        musicService.stopForeground(true)
        //todo handle background service and cancelling notification
        t.interrupt()
        super.onStop()
    }

    override fun onDestroy() {

        _binding = null
        super.onDestroy()
    }


}
