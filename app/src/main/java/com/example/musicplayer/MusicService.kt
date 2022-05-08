package com.example.musicplayer


import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationCompat.getOnlyAlertOnce
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

class MusicService : Service() {
    companion object {
        private const val notificationId = 10
        private const val CHANNEL_ID = "01"
        private const val TAG = "ahmadabadi"

    }

    private val binder = LocalBinder()
    lateinit var mediaList: List<AudioModel>

    @Volatile
    lateinit var mediaPlayer: MediaPlayer

    val index = MutableLiveData<Int>()
    var repeatAll = true
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    private lateinit var message: Message
    private var startId: Int? = null

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
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MusicService, 0, intent, PendingIntent.FLAG_IMMUTABLE)

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

            /*    with(NotificationManagerCompat.from(this@MusicService)) {
                    // notificationId is a unique int for each notification that you must define
                    notify(notificationId, notification)
                }*/
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
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

        this.startId = startId
        return START_STICKY
    }

    fun pauseMedia() {
        mediaPlayer.pause()
    }

    override fun onCreate() {
        HandlerThread("ServiceStartArguments", THREAD_PRIORITY_AUDIO).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }

    }


    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
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

    fun startPlaying(mediaList: List<AudioModel>, index: Int) {

        this.mediaList = mediaList
        this.index.value = index
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

                index.postValue(index.value?.plus(1))
                Log.d("ali", "completed is looping playerViewModel.index" + index.value.toString())
                Log.d("ali", "completed is loopingplayerViewModel.mediaList.size" + mediaList.size.toString())

                if (index.value != mediaList.size && repeatAll) {
                    Log.d("ali", "completed is repeat all")
                    mediaPlayer.stop()

                    startMedia()
                } else {
                    Log.d("ali", "completed is looping finish")
                    mediaPlayer.release()
                }
            }
        }
        updateNotification()

    }

    fun startMedia() {

        if (index.value == mediaList.size) index.postValue(0)
        if (index.value == -1) index.postValue(mediaList.size - 1)
        val media = mediaList[index.value ?: 0]
        val uri = Uri.parse(media.path)
        Log.d("ali", "startMedia: " + media.title)
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer.start()
        handleCompletion()
    }

    fun updateLooping(b: Boolean) {

    }

    fun updateNotification() {
        val intent = Intent(this@MusicService, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MusicService, 0, intent, PendingIntent.FLAG_IMMUTABLE)

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

    }

}

/*

class LocalService : Service() {
    private var mNM: NotificationManager? = null

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private val NOTIFICATION: Int = R.string.local_service_started

    *
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.

    inner class LocalBinder : Binder() {
        val service: LocalService
            get() = this@LocalService
    }

    override fun onCreate() {
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("LocalService", "Received start id $startId: $intent")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Cancel the persistent notification.
        mNM!!.cancel(NOTIFICATION)

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private val mBinder: IBinder = LocalBinder()

    *
     * Show a notification while this service is running.

    private fun showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        val text = getText(R.string.local_service_started)

        // The PendingIntent to launch our activity if the user selects this notification
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, LocalServiceActivities.Controller::class.java), 0
        )

        // Set the info for the views that show in the notification panel.
        val notification: Notification = Notification.Builder(this,"gh")
            .setSmallIcon(R.drawable.stat_sample) // the status icon
            .setTicker(text) // the status text
            .setWhen(System.currentTimeMillis()) // the time stamp
            .setContentTitle(getText(R.string.local_service_label)) // the label of the entry
            .setContentText(text) // the contents of the entry
            .setContentIntent(contentIntent) // The intent to send when the entry is clicked
            .build()

        // Send the notification.
        mNM!!.notify(NOTIFICATION, notification)
    }
}
*/
