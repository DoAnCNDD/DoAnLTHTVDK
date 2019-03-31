package com.doanlthtvdk.doanlthtvdk;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Objects;

public class App extends Application {
    public static final String TAG = "App_DoAnLTHTVDK";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            final String channelId = getString(R.string.default_notification_channel_id);
            final String channelName = getString(R.string.default_notification_channel_name);
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(
                    new NotificationChannel(channelId,
                            channelName,
                            NotificationManager.IMPORTANCE_HIGH
                    )
            );
        }

        FirebaseInstanceId
                .getInstance()
                .getInstanceId()
                .continueWithTask(task -> {
                    final String token = Objects.requireNonNull(task.getResult()).getToken();
                    return FirebaseDatabase
                            .getInstance()
                            .getReference("tokens/" + token)
                            .setValue(token)
                            .continueWith(__ -> token);
                })
                .addOnSuccessListener(token -> Log.d(TAG, "Send token=" + token + " success"))
                .addOnFailureListener(e -> Log.d(TAG, "Send token failure: " + e));
    }
}
