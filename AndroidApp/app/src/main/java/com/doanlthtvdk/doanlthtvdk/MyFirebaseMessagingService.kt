package com.doanlthtvdk.doanlthtvdk

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.app.NotificationCompat

class MyFirebaseMessagingService : FirebaseMessagingService() {

  /**
   * Called when message is received.
   *
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  // [START receive_message]
  override fun onMessageReceived(remoteMessage: RemoteMessage?) {
    // [START_EXCLUDE]
    // There are two types of messages data messages and notification messages. Data messages
    // are handled
    // here in onMessageReceived whether the app is in the foreground or background. Data
    // messages are the type
    // traditionally used with GCM. Notification messages are only received here in
    // onMessageReceived when the app
    // is in the foreground. When the app is in the background an automatically generated
    // notification is displayed.
    // When the user taps on the notification they are returned to the app. Messages
    // containing both notification
    // and data payloads are treated as notification messages. The Firebase console always
    // sends notification
    // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
    // [END_EXCLUDE]

    Log.d(TAG, "Message data payload: " + remoteMessage!!.data)

    // Also if you intend on generating your own notifications as a result of a received FCM
    // message, here is where that should be initiated. See sendNotification method below.
    sendNotification(remoteMessage.data)
  }
  // [END receive_message]


  // [START on_new_token]

  /**
   * Called if InstanceID token is updated. This may occur if the security of
   * the previous token had been compromised. Note that this is called when the InstanceID token
   * is initially generated so this is where you would retrieve the token.
   */
  override fun onNewToken(token: String?) {
    Log.d(TAG, "Refreshed token: " + token!!)

    // If you want to send messages to this application instance or
    // manage this apps subscriptions on the server side, send the
    // Instance ID token to your app server.
    sendRegistrationToServer(token)
  }
  // [END on_new_token]

  /**
   * Persist token to third-party servers.
   *
   *
   * Modify this method to associate the user's FCM InstanceID token with any server-side account
   * maintained by your application.
   *
   * @param token The new token.
   */
  private fun sendRegistrationToServer(token: String?) {
    FirebaseDatabase.getInstance().getReference("tokens/" + token!!)
      .setValue(token)
      .addOnSuccessListener { Log.d(TAG, "Send token=$token success") }
      .addOnFailureListener { Log.d(TAG, "Send token=$token failure") }
  }

  /**
   * Create and show a simple notification containing the received FCM message.
   *
   * @param message FCM remote message  received.
   */
  private fun sendNotification(message: Map<String, String>) {
    val intent = Intent(this, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pendingIntent = PendingIntent.getActivity(
      this, 0 /* Request code */, intent,
      PendingIntent.FLAG_ONE_SHOT
    )

    val channelId = getString(R.string.default_notification_channel_id)
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationBuilder = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setContentTitle()
      .setContentText()
      .setAutoCancel(false)
      .setSound(defaultSoundUri)
      .setContentIntent(pendingIntent)

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
  }

  companion object {

    private val TAG = "MyFirebaseMsgService"
  }
}