package com.example.musicplayer.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.lifecycle.MutableLiveData
import com.example.musicplayer.activities.MainActivity
import com.example.musicplayer.broadcastreciever.NotificationBroadcastReceiver
import com.example.musicplayer.R
import com.example.musicplayer.activities.AudioModel
import com.example.musicplayer.activities.PlayerActivity

class MusicService : Service() {
    companion object {
        private const val notificationId = 10
        private const val CHANNEL_ID = "01"
        private const val TAG = "ahmadabadi"
        private const val ACTION_PLAY_PAUSE = "action_play_pause"
        private const val ACTION_NEXT = "action_next"
        private const val ACTION_PREVIOUS = "action_previous"
        private const val ACTION_CANCEL = "action_cancel"
        lateinit var mediaList: List<AudioModel>
    }

    private val binder = LocalBinder()
    //lateinit var mediaList: List<AudioModel>
    var foregroundActivityBound = false

    @Volatile
    lateinit var mediaPlayer: MediaPlayer

    val index = MutableLiveData<Int>()
    var repeatAll = true
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    private lateinit var message: Message
    private var startId: Int? = null
    private lateinit var mediaSessionCompat: MediaSessionCompat

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun handleMessage(msg: Message) {
            val media = mediaList[index.value ?: 0]
            val uri = Uri.parse(media.path)
            mediaPlayer = MediaPlayer.create(this@MusicService, uri)
            mediaPlayer.start()
            mediaPlayer.isLooping = false
            handleCompletion()
            val intent = Intent(this@MusicService, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            // updateNotification()
            /* val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MusicService, 0, intent, PendingIntent.FLAG_IMMUTABLE)

             val notification = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
                 .setSmallIcon(R.drawable.play)
                 .setContentTitle(mediaList[index.value!!].title)
                 .setContentText(mediaList[index.value!!].artist)
                 .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                 .setVisibility(VISIBILITY_PUBLIC)
                 .setContentIntent(pendingIntent)
                 .setOnlyAlertOnce(true)
                 .build()
             startForeground(111, notification)
 */
            // stopSelf(msg.arg1)
        }
    }

    inner class LocalBinder : Binder() {

        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {

        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        handleAction(intent)
        this.startId = startId
        return START_STICKY
    }

    private fun handleAction(intent: Intent) {
        if (intent.action != null) {
            Log.d(TAG, "handleAction: ")
            when (intent.action) {
                ACTION_PLAY_PAUSE -> {
                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                    else mediaPlayer.start()
                }
                ACTION_NEXT -> {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                    val temp = index.value!!
                    index.postValue(handleIndexBound(temp + 1))
                    startMedia()
                }
                ACTION_PREVIOUS -> {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                    val temp = index.value!!
                    index.postValue(handleIndexBound(temp - 1))
                    startMedia()
                }
                ACTION_CANCEL -> {
                    if (foregroundActivityBound) {
                        mediaPlayer.stop()
                       // index.value = index.value?.minus(1)
                    } else {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                        stopForeground(true)
                    }
                }
            }
        }
    }

    private fun handleIndexBound(i: Int): Int {
        if (i == mediaList.size) return 0
        if (i == -1) return mediaList.lastIndex
        else return i
    }

    fun pauseMedia() {
        mediaPlayer.pause()
    }

    override fun onCreate() {
        mediaSessionCompat = MediaSessionCompat(this, "Music Service")

        HandlerThread("ServiceStartArguments", THREAD_PRIORITY_AUDIO).apply {
            start()
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }

    }


    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "music play channel"
            val descriptionText = "notification when music is playing"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startPlaying(index: Int) {
        this.index.postValue(index)
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId!!
            serviceHandler?.sendMessage(msg)
        }

    }

    private fun handleCompletion() {

        mediaPlayer.setOnCompletionListener {

            if (mediaPlayer.isLooping) {
                startMedia()
            } else {
                val temp = index.value!! + 1
                index.postValue(temp)
                Log.d("ali", "completed is looping playerViewModel.index" + index.value.toString())
                Log.d("ali", "completed is loopingplayerViewModel.mediaList.size" + mediaList.size.toString())

                if (index.value != mediaList.size) {
                    Log.d("ali", "completed is repeat all")

                    startMedia()//todo move to observe index
                    updateNotification()//todo move to observe index
                } else if (index.value == mediaList.size && repeatAll) {
                    index.value = 0
                    startMedia()//todo move to observe index
                    updateNotification()//todo move to observe index
                } else {
                    Log.d("ali", "completed is looping finish")
                    mediaPlayer.release()
                }
            }
        }

    }

    fun startMedia() {

        if (index.value == mediaList.size) index.postValue(0)
        if (index.value == -1) index.postValue(mediaList.size - 1)
        val media = mediaList[index.value ?: 0]
        val uri = Uri.parse(media.path)
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer.start()
        handleCompletion()
    }


    fun updateNotification() {
        val intent = Intent(this@MusicService, PlayerActivity::class.java).apply {
            // todo flags??
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MusicService, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val prevIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_PREVIOUS)
        val prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, 0)

        val playPauseIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_PLAY_PAUSE)
        val playPausePending = PendingIntent.getBroadcast(this, 0, playPauseIntent, 0)

        val nextIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_NEXT)
        val nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, 0)

        val cancelIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_CANCEL)
        val cancelPending = PendingIntent.getBroadcast(this, 0, cancelIntent, 0)

        val currentSong = mediaList[index.value ?: 0]// todo why elvis
        var bitmapImage: Bitmap? = null
        if (currentSong.coverImage != null) {
            bitmapImage = BitmapFactory.decodeByteArray(currentSong.coverImage, 0, currentSong.coverImage.size)
        } else bitmapImage = BitmapFactory.decodeResource(resources, R.drawable.music)

        val notification = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
            .setSmallIcon(R.drawable.play)
            .setLargeIcon(bitmapImage!!)
            .setContentTitle(currentSong.title)
            .setContentText(currentSong.artist)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_baseline_skip_previous_24, "prev", prevPending)
            .addAction(R.drawable.play, "play pause", playPausePending)
            .addAction(R.drawable.ic_baseline_skip_next_24, "next", nextPending)
            .addAction(R.drawable.ic_baseline_close_24, "cancel", cancelPending)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken))
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .build()
        startForeground(111, notification)
    }

}
