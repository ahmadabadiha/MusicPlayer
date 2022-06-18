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
import com.example.musicplayer.broadcastreciever.NotificationBroadcastReceiver
import com.example.musicplayer.R
import com.example.musicplayer.model.AudioModel
import com.example.musicplayer.activities.PlayerActivity

class MusicService : Service() {
    companion object {
        private const val CHANNEL_ID = "01"
        private const val TAG = "ahmadabadi"
        private const val ACTION_PLAY_PAUSE = "action_play_pause"
        private const val ACTION_NEXT = "action_next"
        private const val ACTION_PREVIOUS = "action_previous"
        private const val ACTION_CANCEL = "action_cancel"
        @Volatile
        lateinit var mediaList: List<AudioModel>
    }

    private val binder = LocalBinder()
    var foregroundActivityBound = false

    @Volatile
    lateinit var mediaPlayer: MediaPlayer

    val index = MutableLiveData<Int>()
    var repeatAll = true
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    private var startId: Int? = null
    private lateinit var mediaSessionCompat: MediaSessionCompat

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun handleMessage(msg: Message) {
            val media = mediaList[index.value!!]
            val uri = Uri.parse(media.path)
            mediaPlayer = MediaPlayer.create(this@MusicService, uri)
            mediaPlayer.start()
            mediaPlayer.isLooping = false
            handleCompletion()
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
                    } else {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                        try{
                            stopForeground(true)
                        }catch (e: NullPointerException){
                        // This occurs when the activity is already running. So the client must close the activity first. So nothing in this scope.
                        }
                    }
                }
            }
        }
    }

    private fun handleIndexBound(i: Int): Int {
        if (i == mediaList.size) return 0
        return if (i == -1) mediaList.lastIndex
        else i
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
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startPlaying(index: Int) {
        Log.d(TAG, "startPlaying: " + ::mediaPlayer.isInitialized.toString() + this.index.value.toString())
        // handling rotation and opening player activity with a different song while the former is playing is implemented here too
        if(::mediaPlayer.isInitialized && this.index.value != index){
            mediaPlayer.stop()
            mediaPlayer.release()
            this.index.value = index
            serviceHandler?.obtainMessage()?.also { msg ->
                msg.arg1 = startId!!
                serviceHandler?.sendMessage(msg)
            }
        }
        if (!::mediaPlayer.isInitialized){
            this.index.value = index
            serviceHandler?.obtainMessage()?.also { msg ->
                msg.arg1 = startId!!
                serviceHandler?.sendMessage(msg)
            }
        }

    }

    private fun handleCompletion() {

        mediaPlayer.setOnCompletionListener {

            if (mediaPlayer.isLooping) {
                startMedia()
            } else {
                val temp = index.value!! + 1
                index.postValue(temp)
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
        val media = mediaList[index.value!!]
        val uri = Uri.parse(media.path)
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer.start()
        handleCompletion()
    }


    fun updateNotification() {
        val intent = Intent(this@MusicService, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MusicService, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val prevIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_PREVIOUS)
        val prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val playPauseIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_PLAY_PAUSE)
        val playPausePending = PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(this, NotificationBroadcastReceiver::class.java).setAction(ACTION_NEXT)
        val nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val cancelIntent = Intent(this, MusicService::class.java).setAction(ACTION_CANCEL)
        val cancelPending = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val currentSong = mediaList[handleIndexBound(index.value!!)]
        val bitmapImage: Bitmap?
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
