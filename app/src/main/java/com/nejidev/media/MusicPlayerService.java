package com.nejidev.media;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import static com.nejidev.media.MainActivity.CMD_PLAY;

public class MusicPlayerService extends Service implements Runnable , MediaPlayer.OnCompletionListener {
    private static final String TAG = "MusicPlayerService";
    private final IBinder mBinder = new LocalBinder();
    private Messenger mMessenger, clientMessenger;//Service的信使对象和客户端的信使对象
    private int cmd;
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private int mediaDuration = 0;
    private boolean mediaPlayerThreadRuing = false;
    private Thread mediaPlayerThread = new Thread(this);

    @Override
    public void run() {
        String channelId = "message";
        String channelName = "message";
        NotificationChannel notificationChannel;
        NotificationCompat.Builder builder;

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        //点击后启动activity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        while(true) {
            while (mediaPlayerThreadRuing) {
                int currentPosition = mMediaPlayer.getCurrentPosition();

                int progress = currentPosition * 100 / mediaDuration;
                Message message = Message.obtain();
                message.arg1 = MainActivity.CMD_SYNC_POS;
                message.obj = progress;
                message.replyTo = mMessenger;

                Log.i(TAG, "currentPosition:" + progress);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//android 8.0以上
                    builder = new NotificationCompat.Builder(this, channelId);
                    builder.setContentIntent(pendingIntent);

                    builder.setSmallIcon(R.mipmap.ic_launcher);
                    builder.setTicker("MediaPlayer");
                    builder.setWhen(System.currentTimeMillis());
                    builder.setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE);
                    builder.setSound(null);
                    builder.setVibrate(new long[]{0});
                    //创建一个message通道，名字为消息
                    notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
                    notificationChannel.enableLights(false);
                    notificationChannel.enableVibration(false);
                    notificationChannel.setVibrationPattern(new long[]{0});
                    notificationChannel.setSound(null, null);

                    notificationManager.createNotificationChannel(notificationChannel);

                    builder.setChannelId(channelId);

                    builder.setContentTitle(MusicPlayerApp.playerName);
                    builder.setContentText(progress + "%");
                    builder.setProgress(100, progress, false);
                    notificationManager.notify(0, builder.build());
                    //发送通知
                    notificationManager.notify(0, builder.build());
                }
                else{
                    //低于 android 8 的版本
                    builder = new NotificationCompat.Builder(this);
                    builder.setContentIntent(pendingIntent);

                    builder.setSmallIcon(R.mipmap.ic_launcher);
                    builder.setTicker("MediaPlayer");
                    builder.setWhen(System.currentTimeMillis());
                    builder.setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE);
                    builder.setSound(null);
                    builder.setVibrate(new long[]{0});

                    builder.setContentTitle(MusicPlayerApp.playerName);
                    builder.setContentText(progress + "%");
                    builder.setProgress(100, progress, false);

                    //发送通知
                    notificationManager.notify(0, builder.build());
                }

                try {
                    clientMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaDuration = 0;
        mediaPlayerThreadRuing = false;

        Message message = Message.obtain();
        message.arg1 = MainActivity.CMD_FINISH;
        message.replyTo = mMessenger;

        Log.i(TAG, "onCompletion CMD_FINISH");

        try {
            clientMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public class LocalBinder extends Binder {
        public MusicPlayerService getService(){
            return MusicPlayerService.this;
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            clientMessenger = msg.replyTo;//获取Message中的客户端信使对象
            cmd = msg.arg1; //获取Message中的cmd

            Log.i(TAG, "cmd:" + cmd);
            if(cmd == MainActivity.CMD_PLAY){
                String mediaFilePath = (String)msg.obj;

                if(null == mediaFilePath){
                    Log.i(TAG, "mediaFilePath is null");
                    if(0 < mMediaPlayer.getDuration() && ! mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                    }
                    return ;
                }
                Log.i(TAG, "mediaFilePath:" + mediaFilePath);

                if(mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(mediaFilePath);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaDuration = mMediaPlayer.getDuration();

                Log.i(TAG, "mediaDuration:" + mediaDuration);

                mediaPlayerThreadRuing = true;

                mMediaPlayer.start();
            }
            if(cmd == MainActivity.CMD_SEEK){
                int seekVal = (int)msg.obj;
                Log.i(TAG, "seekVal:" + seekVal);
                Log.i(TAG, "mediaDuration:" + mediaDuration);

                if(mMediaPlayer.isPlaying()) {
                    int pos = mediaDuration * seekVal / 100;
                    mMediaPlayer.seekTo(pos);
                }
            }
            if(cmd == MainActivity.CMD_PAUSE){
                if(mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
            }
            if(cmd == MainActivity.CMD_SET_MODE){
                String playMode = (String) msg.obj;
                if(playMode.equals("One")) {
                    mMediaPlayer.setLooping(true);
                }
                else{
                    mMediaPlayer.setLooping(false);
                }
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mMessenger = new Messenger(handler);//初始化Service信使
        mediaPlayerThread.start();
        mMediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand flags:" +flags + " startId:"+startId);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind");
        super.onRebind(intent);
    }
}
