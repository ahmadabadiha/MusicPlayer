package com.example.musicplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.databinding.FragmentPlayerBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity(), ServiceConnection {
    //private val sharedViewModel: SharedViewModel by activityViewModels()
    //private val playerViewModel: PlayerViewModel by viewModels()
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var t: Thread
    private var repeatAll = true
    private lateinit var musicService: MusicService
    private var mBound = false

    companion object {
        private const val TAG = "ahmadabadiha"
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.LocalBinder
        musicService = binder.getService()
        Log.d(TAG, "onServiceConnected")
        mBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected")
        mBound = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        serviceBind()

        val r = Runnable { startRotationAnim() }
        t = Thread(r)
        t.start()
        val index = intent.getIntExtra("index", 0)


        lifecycleScope.launch {

            while (mBound == false) {
                delay(10)
            }

            musicService.startPlaying(Medias.mediaList, index)

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
                }
                if (temp1) break
            }

        }

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
            //try {

            val mediaPosition = musicService.mediaPlayer.currentPosition
            binding.seekBar.progress = mediaPosition
            binding.timePast.text = computeTime(mediaPosition)
            // }catch (e: IllegalStateException){
            //    binding.seekBar.progress = binding.seekBar.max
            //}
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

    private fun startRotationAnim() {
        val rotateAnim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnim.repeatCount = Animation.INFINITE
        rotateAnim.duration = 300000
        rotateAnim.start()
        binding.coverImage.startAnimation(rotateAnim)
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
            musicService.index.postValue(handleIndexBound(temp + 1))
            Log.d(
                "ahmadabadi", "next clicked " + musicService.index.value.toString()
            )
            Log.d(
                "ahmadabadi", "next clicked " + musicService.index.hasObservers().toString()
            )
            binding.playIcon.setImageResource(R.drawable.pause)
            binding.play.setShapeType(1)
            musicService.startMedia()
        }
        binding.prevIcon.setOnClickListener {
            musicService.mediaPlayer.stop()
            musicService.mediaPlayer.prepare()
            val temp = musicService.index.value!!
            musicService.index.postValue(handleIndexBound(temp - 1))

            //Log.d("ahmadabadi", "previous clicked " + musicService.index.value.toString())
            binding.playIcon.setImageResource(R.drawable.pause)
            binding.play.setShapeType(1)
            musicService.startMedia()
        }

        binding.shuffle.setOnClickListener {
            musicService.mediaList = musicService.mediaList.shuffled()
            Toast.makeText(this, "List of songs shuffled now!", Toast.LENGTH_SHORT).show()
        }

        binding.repeatOne.setOnClickListener {
            val temp = !musicService.mediaPlayer.isLooping
            musicService.mediaPlayer.isLooping = temp
            if (temp) {
                binding.repeatOne.setShapeType(1)
            } else binding.repeatOne.setShapeType(0)
            Log.d("ahmadabadi", "repeat one clicked " + temp)
            /* if (mediaPlayer.isLooping) binding.repeatOne.setShapeType(1)
             else binding.repeatOne.setShapeType(0)*/
        }
        binding.repeatAll.setOnClickListener {
            val temp = !musicService.repeatAll
            musicService.repeatAll = temp
            if (temp) binding.repeatAll.setShapeType(1)
            else binding.repeatAll.setShapeType(0)
            Log.d("ahmadabadi", "repeat all clicked " + temp)

        }
    }

    private fun handleIndexBound(i: Int): Int {

        if (i == musicService.mediaList.size) return 0
        if (i == -1) return musicService.mediaList.lastIndex
        else return i
    }

    private fun initObserveLiveData() {

        musicService.index.observeForever {
            Log.d("ahmadabadi", "index observed: index = " + it.toString() + " " + musicService.mediaList[it].title)

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
            try {
                binding.songTime.text = computeTime(musicService.mediaPlayer.duration)
            } catch (e: UninitializedPropertyAccessException) {

            }
            musicService.updateNotification()
        }
    }

    private fun computeTime(l: Int): String {
        return String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(l.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(l.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l.toLong()))
        )
    }

    override fun onDestroy() {
        t.interrupt()
        super.onDestroy()
    }
}



