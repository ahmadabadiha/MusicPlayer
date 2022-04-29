package com.example.musicplayer


import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData

class MusicService : Service() {

    private val binder = LocalBinder()
    lateinit var mediaList: List<AudioModel>
    lateinit var mediaPlayer: MediaPlayer
    val index = MutableLiveData<Int>()
    val isLooping = MutableLiveData<Boolean>(false)
    val repeatAll = MutableLiveData<Boolean>(true)


    inner class LocalBinder : Binder() {

        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        return START_REDELIVER_INTENT
    }

    fun pauseMedia() {

    }

    override fun onCreate() {
/*        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }*/

    }


    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    fun startPlaying(mediaList: List<AudioModel>, index: Int) {
        this.mediaList = mediaList
        this.index.value = index
        val media = mediaList[index]
        val uri = Uri.parse(media.path)
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer.isLooping = false
        mediaPlayer.start()
        handleCompletion()
    }

    private fun handleCompletion() {

        mediaPlayer.setOnCompletionListener {
            if (mediaPlayer.isLooping) {
                startMedia()
            } else {

                index.value = index.value?.plus(1)
                Log.d("ali", "completed is looping playerViewModel.index" + index.value.toString())
                Log.d("ali", "completed is loopingplayerViewModel.mediaList.size" + mediaList.size.toString())

                if (index.value != mediaList.size && repeatAll.value!!) {
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

    fun startMedia() {

        if (index.value == mediaList.size) index.value = 0
        if (index.value == -1) index.value = mediaList.size - 1
        val media = mediaList[index.value!!]
        val uri = Uri.parse(media.path)
        //binding.songName.text = media.title
        Log.d("ali", "startMedia: " + media.title)
        mediaPlayer = MediaPlayer.create(this, uri)
        // binding.songTime.text = computeTime(mediaPlayer.duration)
        handleCompletion()
        mediaPlayer.isLooping = isLooping.value ?: false
        mediaPlayer.start()

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
        val notification: Notification = Builder(this)
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
}*/
private var thisBinder: YourBinder? = null

fun oncreate() {
    thisBinder = YourBinder() //don't forget this.
}


class YourBinder : Binder()

fun onBind(intent: Intent?): IBinder? {
    return thisBinder
}