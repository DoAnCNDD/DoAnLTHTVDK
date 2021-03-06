package com.doanlthtvdk.doanlthtvdk

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.FirebaseDatabase

class MyService : Service() {
  private var receiver: Receiver? = null

  override fun onBind(intent: Intent?) = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0 /* Request code */,
      Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      },
      PendingIntent.FLAG_ONE_SHOT
    )

    val channelId = getString(R.string.default_notification_channel_id)

    val notification = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setContentTitle("DoAnLTHTVDK")
      .setContentText("Running...")
      .setAutoCancel(false)
      .setShowWhen(true)
      .setWhen(System.currentTimeMillis())
      .setContentIntent(pendingIntent)
      .build()
    startForeground(69, notification)

    receiver = Receiver().also {
      LocalBroadcastManager
        .getInstance(this)
        .registerReceiver(it, IntentFilter("SHOW_OVERLAY").apply { addAction("HIDE_OVERLAY") })
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    stopForeground(true)
    receiver?.let {
      LocalBroadcastManager
        .getInstance(this)
        .unregisterReceiver(it)
    }
  }

  class Receiver : BroadcastReceiver() {
    private var isShowing = false
    private var view: View? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onReceive(context: Context, intent: Intent) {
      Log.d(OnStopNotificationReceiver.TAG, "Receiver ${intent.action}")

      if (intent.action == "SHOW_OVERLAY") {
        if (isShowing) return

        isShowing = true

        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification}")
        val mediaPlayer = MediaPlayer().also { this@Receiver.mediaPlayer = it }
        mediaPlayer.setDataSource(context, uri)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
          mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING)
          mediaPlayer.isLooping = true
          mediaPlayer.setVolume(1f, 1f)
          mediaPlayer.prepare()
          mediaPlayer.start()
        }

        val params = WindowManager.LayoutParams(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
              or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
              or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
          PixelFormat.TRANSLUCENT
        )
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater
          .from(context)
          .inflate(R.layout.layout_warning, null)
          .also { this@Receiver.view = it }

        view.findViewById<View>(R.id.button).setOnClickListener {
          kotlin.runCatching { windowManager.removeViewImmediate(view) }
          isShowing = false
          mediaPlayer.stop()
          mediaPlayer.release()

          val id = intent.getStringExtra("id")
          val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
          notificationManager.cancel(id.toIntOrNull() ?: 0 /* ID of notification */)

          val database = FirebaseDatabase.getInstance()
          database
            .getReference("on_off_send")
            .setValue("0")
            .continueWithTask {
              database
                .getReference("histories/$id/verified")
                .setValue(true)
            }
            .addOnSuccessListener {}
        }
        windowManager.addView(
          view,
          params
        )
      } else if (intent.action == "HIDE_OVERLAY") {
        Log.d(OnStopNotificationReceiver.TAG, "Receiver ${intent.action} HIDE_OVERLAY")
        kotlin.runCatching {
          val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
          view?.let { windowManager.removeViewImmediate(it) }
          mediaPlayer?.stop()
          mediaPlayer?.release()
        }.onSuccess {
          Log.d(
            OnStopNotificationReceiver.TAG,
            "Receiver ${intent.action} HIDE_OVERLAY_SUCCESS"
          )
          isShowing = false
        }
      }
    }
  }
}