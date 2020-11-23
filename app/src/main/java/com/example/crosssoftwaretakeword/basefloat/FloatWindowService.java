package com.example.crosssoftwaretakeword.basefloat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.crosssoftwaretakeword.MainActivity;
import com.example.crosssoftwaretakeword.R;


public class FloatWindowService extends Service {
    private static final String TAG = "FloatWindowService";

    private static final String NOTIFICATION_CHANNEL_ID = "FloatWindowService";
    public static final int MANAGER_NOTIFICATION_ID = 0x1001;

    private FloatTakeWordView floatBase;

    public FloatWindowService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        show();
        return super.onStartCommand(intent, flags, startId);
    }

    private void show(){
        if(floatBase != null){
            return ;
        }

        floatBase = new FloatTakeWordView(this);
        floatBase.show();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        addForegroundNotification();
    }

    @Override
    public void onDestroy() {
        if(floatBase != null){
            floatBase.remove();
            floatBase.destroy();
            floatBase = null;
        }
        super.onDestroy();
    }

    private void addForegroundNotification() {
        createNotificationChannel();

        String contentTitle = getPackageName();
        String contentText = "正在获取等待截屏";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent msgIntent = getActivityIntent();
        PendingIntent mainPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                msgIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = mBuilder.setContentIntent(mainPendingIntent)
                .setAutoCancel(false).build();

        startForeground(MANAGER_NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Name";
            String description = "Description";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Intent getActivityIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }
}
