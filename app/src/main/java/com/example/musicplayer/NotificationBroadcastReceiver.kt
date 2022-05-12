package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        Log.d("ahmadabadi", "onReceive: $action")
        if (action != null){
            val serviceIntent = Intent(context, MusicService::class.java)
            serviceIntent.action = action
            context?.startService(serviceIntent)
        }
    }
}
