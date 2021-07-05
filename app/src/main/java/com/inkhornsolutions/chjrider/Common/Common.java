package com.inkhornsolutions.chjrider.Common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.inkhornsolutions.chjrider.R;

import java.util.Random;


public class Common {

    public static void showNotification(Context context, int id, String title, String body, Intent intent) {

        PendingIntent pendingIntent = null;

        if (intent != null){
            Log.d("TAG", "Intent is not null");

            pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String NOTIFICATION_CHANNEL_ID = "1001";
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        "Food Box", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("food Box");
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                notificationChannel.enableVibration(true);

                notificationManager.createNotificationChannel(notificationChannel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(false)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_background));

            if (pendingIntent != null){
                builder.setContentIntent(pendingIntent);
            }
            Notification notification = builder.build();
            notificationManager.notify(id, notification);
        }
    }

    public static String createUniqueTripIdNumber(long timeOffset) {

        Random random = new Random();
        Long current = System.currentTimeMillis() + timeOffset;
        Long unique = current + random.nextLong();
        if (unique < 0) unique *= (-1);

        return String.valueOf(unique);
    }
}
