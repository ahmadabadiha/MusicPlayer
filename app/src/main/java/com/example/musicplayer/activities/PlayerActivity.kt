package com.example.musicplayer.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.animation.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentPlayerBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity(), ServiceConnection {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var t: Thread
    private lateinit var musicService: MusicService
    private var mBound = false

    companion object {
        private const val TAG = "ahmadabadiha"
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.LocalBinder
        musicService = binder.getService()
        musicService.foregroundActivityBound = true
        Log.d(TAG, "onServiceConnected")
        mBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected")
        mBound = false
    }

    override fun onStart() {
        super.onStart()
        serviceBind()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startAnim()
        seekbarChange()

        val index = intent.getIntExtra("index", -1)

        lifecycleScope.launch {

            while (mBound == false) {
                delay (10)
            }

            if (index != -1) musicService.startPlaying(index)

            var temp1 = false
            while (true) {
                try {
                    initSetOnclickListeners()
                    initObserveLiveData()
                    binding.songTime.text = computeTime(musicService.mediaPlayer.duration)
                    binding.seekBar.max = musicService.mediaPlayer.duration
                    updateSeekbar()
                    temp1 = true
                } catch (e: UninitializedPropertyAccessException) {
                    delay(10)
                }catch (e: IllegalStateException){
                    delay(10)
                }
                if (temp1) break
            }
        }
    }

    private fun seekbarChange() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService.mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
    }

    private suspend fun updateSeekbar() {
        while (true) {
            val mediaPosition = musicService.mediaPlayer.currentPosition
            binding.seekBar.progress = mediaPosition
            binding.timePast.text = computeTime(mediaPosition)
            delay(1000)
        }
    }


    private fun serviceBind() {
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, this, Context.BIND_AUTO_CREATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
        }
    }

    private fun startAnim() {
        val r = Runnable {
            val rotateAnim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                repeatCount = Animation.INFINITE
                duration = 300000
            }
            binding.coverImage.startAnimation(rotateAnim)

            val alphaAnimation = AlphaAnimation(1f, 0f).apply {
                repeatCount = Animation.INFINITE
                duration = 3000
                interpolator = AnticipateInterpolator()
                repeatMode = Animation.REVERSE
            }
            binding.centerIcon.startAnimation(alphaAnimation)
        }
        t = Thread(r)
        t.start()
    }

    private fun initSetOnclickListeners() {
        binding.playIcon.setOnClickListener {
            // todo handle illegal state exception
            if (musicService.mediaPlayer.isPlaying) {
                binding.playIcon.setImageResource(R.drawable.play)
                musicService.mediaPlayer.pause()
                binding.play.setShapeType(0)
            } else {
                binding.playIcon.setImageResource(R.drawable.pause)
                musicService.mediaPlayer.start()
                binding.play.setShapeType(1)

            }
        }

        binding.nextIcon.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.prepare()
            val temp = musicService.index.value!!
            musicService.index.value = handleIndexBound(temp+1)
            Log.d(
                "ahmadabadi", "next clicked " + musicService.index.value.toString()
            )
            binding.playIcon.setImageResource(R.drawable.pause)
            binding.play.setShapeType(1)
            musicService.startMedia()
        }
        binding.prevIcon.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.prepare()
            val temp = musicService.index.value!!
            musicService.index.value = handleIndexBound(temp-1)
            Log.d("ahmadabadi", "previous clicked " + musicService.index.value.toString())
            binding.playIcon.setImageResource(R.drawable.pause)
            binding.play.setShapeType(1)
            musicService.startMedia()
        }

        binding.shuffleIcon.setOnClickListener {
            MusicService.mediaList = MusicService.mediaList.shuffled()
            Toast.makeText(this, "List of songs shuffled now!", Toast.LENGTH_SHORT).show()
        }

        binding.repeatOneIcon.setOnClickListener {
            val temp = !musicService.mediaPlayer.isLooping
            musicService.mediaPlayer.isLooping = temp
            if (temp) {
                binding.repeatOne.setShapeType(1)
            } else binding.repeatOne.setShapeType(0)
            Log.d("ahmadabadi", "repeat one clicked " + temp)
        }
        binding.repeatAllIcon.setOnClickListener {
            val temp = !musicService.repeatAll
            musicService.repeatAll = temp
            if (temp) binding.repeatAll.setShapeType(1)
            else binding.repeatAll.setShapeType(0)
            Log.d("ahmadabadi", "repeat all clicked " + temp)

        }
        binding.centerIcon.setOnClickListener {
            startMusicIconAnim()
        }
    }

    private fun startMusicIconAnim() {
        if (!binding.circularFlow.isGone) {
            val alphaAnimation = AlphaAnimation(1f, 0f).apply {
                duration = 400
            }
            binding.prev.startAnimation(alphaAnimation)
            binding.next.startAnimation(alphaAnimation)
            binding.repeatAll.startAnimation(alphaAnimation)
            binding.shuffleIcon.startAnimation(alphaAnimation)
            binding.repeatOne.startAnimation(alphaAnimation)
            ValueAnimator.ofInt(96, -10).apply {
                duration = 500
                start()
                addUpdateListener {
                    binding.circularFlow.updateRadius(binding.prev, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.next, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.shuffle, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.repeatAll, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.repeatOne, this.animatedValue as Int)
                }

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        binding.circularFlow.isGone = !binding.circularFlow.isGone
                    }
                })
            }
        } else {
            binding.circularFlow.isGone = !binding.circularFlow.isGone

            val alphaAnimation = AlphaAnimation(0f, 1f).apply {
                duration = 700
                interpolator = AccelerateInterpolator()
            }
            binding.prev.startAnimation(alphaAnimation)
            binding.next.startAnimation(alphaAnimation)
            binding.repeatAll.startAnimation(alphaAnimation)
            binding.shuffleIcon.startAnimation(alphaAnimation)
            binding.repeatOne.startAnimation(alphaAnimation)
            ValueAnimator.ofInt(-10, 96).apply {
                duration = 500
                start()
                addUpdateListener {
                    binding.circularFlow.updateRadius(binding.prev, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.next, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.shuffle, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.repeatAll, this.animatedValue as Int)
                    binding.circularFlow.updateRadius(binding.repeatOne, this.animatedValue as Int)
                }
            }

        }

    }

    private fun handleIndexBound(i: Int): Int {
        if (i == MusicService.mediaList.size) return 0
        if (i == -1) return MusicService.mediaList.lastIndex
        else return i
    }

    private fun initObserveLiveData() {

        musicService.index.observe(this) {
            Log.d("ahmadabadi", "index observed: index = " + it.toString() + " " + MusicService.mediaList[handleIndexBound(it)].title)

            val song = MusicService.mediaList[handleIndexBound(it)]
            try {
                binding.songTime.text = computeTime(musicService.mediaPlayer.duration)
                binding.seekBar.max = musicService.mediaPlayer.duration
            }catch (e: Exception){
                //todo
            }
            binding.songName.text = song.title
            binding.artist.text = song.artist
            binding.album.text = song.album
            if (song.coverImage != null) {
                val bitmapImage = BitmapFactory.decodeByteArray(song.coverImage, 0, song.coverImage.size)
                binding.coverImage.setImageBitmap(bitmapImage)
                binding.backgroundImage.setImageBitmap(bitmapImage)
            } else {
                binding.coverImage.setImageResource(R.drawable.music)
                binding.backgroundImage.setImageResource(R.drawable.music)
            }
            musicService.updateNotification()
            //todo a bug when it's the last song
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
        super.onStop()
        musicService.foregroundActivityBound = false
        unbindService(this)
    }

    override fun onDestroy() {
        t.interrupt()
        super.onDestroy()
    }
}



