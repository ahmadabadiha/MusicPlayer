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
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.musicplayer.databinding.FragmentPlayerBinding
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

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            Log.d("ahmadabadi", "onServiceConnected")
            //mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("ahmadabadi", "onServiceDisconnected")

            //mBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(context, MusicService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                /*context?.applicationContext?*/
                requireActivity().applicationContext.startForegroundService(intent)
            }
            //requireContext().applicationContext
            requireActivity().applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            Log.d("ahmadabadi", "sdgfdhgfdgfd")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)
        if (savedInstanceState == null) {
            playerViewModel.mediaList = sharedViewModel.audioList
        }


        val media = playerViewModel.mediaList[playerViewModel.index]


        musicService.startPlaying(playerViewModel.mediaList, playerViewModel.index)

        musicService.index.observeForever {
            val song = playerViewModel.mediaList[it]
            if (song.coverImage != null) {
                val bitmapImage = BitmapFactory.decodeByteArray(song.coverImage, 0, song.coverImage.size)
                binding.coverImage.setImageBitmap(bitmapImage)
            } else binding.coverImage.setImageResource(R.drawable.ic_baseline_music_note_24)
            binding.songName.text = song.title
            binding.artist.text = song.artist
            binding.album.text = song.album
        }


        /*     val uri = Uri.parse(media.path)
             mediaPlayer = MediaPlayer.create(requireContext(), uri)
             binding.songName.text = media.title
             binding.songTime.text = computeTime(mediaPlayer.duration)
             mediaPlayer.start()
     */

        val rotateAnim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnim.repeatCount = Animation.INFINITE
        rotateAnim.duration = 100000
        rotateAnim.start()
        binding.coverImage.startAnimation(rotateAnim)

        initSetOnclickListeners()
        initObserveLiveData()
        handleCompletion()

        val updateSongTime = object : Runnable {
            override fun run() {

                val mediaPosition = musicService.mediaPlayer.currentPosition
                binding.timePast.text = computeTime(mediaPosition)
                binding.seekBar.progress = (mediaPosition / mediaPlayer.duration).toInt()

                Handler(Looper.getMainLooper()).postDelayed(this, 1000)

            }
        }
        t = Thread(updateSongTime)
        t.start()

        /*   val t = thread {
               val duration = mediaPlayer.duration
               var currentPosition = 0
               while (currentPosition<duration){
                   sleep(500)
                   currentPosition = mediaPlayer.currentPosition
                   binding.seekBar.apply {
                       progress = currentPosition
                       max = duration
                   }
               }
           }
           t.start()*/

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
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

        binding.next.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.release()
            musicService.index.value = musicService.index.value?.plus(1)
            musicService.startMedia()
        }
        binding.prev.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.release()
            musicService.index.value = musicService.index.value?.minus(1)
            Log.d(
                "ali", "initSetOnclickListeners: " + playerViewModel.index.toString()
            )
            musicService.startMedia()
        }

        binding.shuffle.setOnClickListener {
            musicService.mediaList = musicService.mediaList.shuffled()
            Toast.makeText(requireContext(), "List of songs shuffled now!", Toast.LENGTH_SHORT).show()
        }

        binding.repeatOne.setOnClickListener {
            musicService.isLooping.value = !musicService.isLooping.value!!
            /* if (mediaPlayer.isLooping) binding.repeatOne.setShapeType(1)
             else binding.repeatOne.setShapeType(0)*/
        }

        binding.repeatAll.setOnClickListener {
            musicService.repeatAll.value = !musicService.repeatAll.value!!

        }
    }

    private fun initObserveLiveData() {
        musicService.isLooping.observeForever {
            musicService.mediaPlayer.isLooping = it
            if (it) binding.repeatOne.setShapeType(1)
            else binding.repeatOne.setShapeType(0)
        }

        musicService.repeatAll.observeForever {
            musicService.repeatAll.value = it
            if (it) binding.repeatAll.setShapeType(0)
            else binding.repeatAll.setShapeType(1)
        }

    }

    private fun handleCompletion() {
        Log.d("ali", "15646")

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
        t.interrupt()
        super.onStop()
    }

    override fun onDestroy() {

        _binding = null
        super.onDestroy()
    }


}
