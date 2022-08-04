package com.nejidev.media;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";
    private Intent mIntent;

    private Messenger mMessenger, remoteMessenger;//远程Service端的信使对象和客户端本地的信使对象
    private int cmd;

    private Button mButtonPrev;
    private Button mButtonPlayPause;
    private Button mButtonNext;
    private Button mButtonMode;
    private SeekBar mSeekBarVolume;

    public static int CMD_INIT = 0;
    public static int CMD_IDLE = 1;
    public static int CMD_PLAY = 2;
    public static int CMD_PAUSE = 3;
    public static int CMD_SEEK = 4;
    public static int CMD_PREV = 5;
    public static int CMD_NEXT = 6;
    public static int CMD_FINISH = 7;
    public static int CMD_SET_MODE = 8;
    public static int CMD_SYNC_POS = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (null == mIntent) {
            mIntent = new Intent(this, MusicPlayerService.class);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mIntent);
        }

        bindService();
        checkPermission();

        mButtonPrev = findViewById(R.id.buttonPrev);
        mButtonPlayPause = findViewById(R.id.buttonPlayPause);
        mButtonNext = findViewById(R.id.buttonNext);
        mButtonMode = findViewById(R.id.buttonMode);

        String playMode = MusicPlayerApp.playerMode;
        if(playMode.equals("")){
            playMode = "All";
            MusicPlayerApp.getInstance().playerMode = playMode;
            MusicPlayerApp.getInstance().saveMode();
        }
        mButtonMode.setText(playMode);

        mSeekBarVolume = findViewById(R.id.seekBarVolume);
        mSeekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();

                Log.i(TAG, "onStopTrackingTouch progress:" + progress);

                Message message = Message.obtain();
                message.arg1 = CMD_SEEK;
                message.obj = progress;
                message.replyTo = mMessenger;

                try {
                    remoteMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        unBindService();
        mSeekBarVolume.setOnSeekBarChangeListener(null);
        super.onDestroy();
    }

    private void showPlayDirs()
    {
        MusicPlayerApp.mediaItems.clear();
        MusicPlayerApp.mediaItems.add(new MediaItem("/"));

        for(String dirPath : MusicPlayerApp.getInstance().mMediaDirs){
            Log.i(TAG, "showPlayDirs:" + dirPath);
            MusicPlayerApp.mediaItems.add(new MediaItem(dirPath));
        }

        MediaDirAdapter mediaDirAdapter = new MediaDirAdapter(MainActivity.this, R.layout.media_dir_view, MusicPlayerApp.mediaItems);
        ListView listview = findViewById(R.id.mediaDirListView);
        listview.setAdapter(mediaDirAdapter);
        listview.setOnItemClickListener(this);
    }

    private void showPlayFiles(String mediaDirPath)
    {
        File rootFile = new File(mediaDirPath);

        if(! rootFile.exists()){
            Log.i(TAG, mediaDirPath + ":file not exists");
            return;
        }

        File[] sonFiles = rootFile.listFiles();
        if(null == sonFiles || 0 == sonFiles.length){
            Log.i(TAG, mediaDirPath + ":list files empty");
            return;
        }

        MusicPlayerApp.mediaItems.clear();
        MusicPlayerApp.mediaItems.add(new MediaItem("/"));

        for(File sonFile : sonFiles) {
            Log.i(TAG, "sonFile:" + sonFile);
            if(MediaItem.checkMediaFile(sonFile.getPath())) {
                MusicPlayerApp.mediaItems.add(new MediaItem(sonFile.getPath()));
            }
        }

        MusicPlayerApp.getInstance().playerItemMax = MusicPlayerApp.mediaItems.size();

        MediaDirAdapter mediaDirAdapter = new MediaDirAdapter(MainActivity.this, R.layout.media_dir_view, MusicPlayerApp.mediaItems);
        ListView listview = findViewById(R.id.mediaDirListView);
        listview.setAdapter(mediaDirAdapter);
    }

    private void PlayFile(MediaItem mediaDir)
    {
        String mediaFilePath = mediaDir.getPath();
        Log.i(TAG, "PlayFile:" + mediaFilePath);

        if(! MediaItem.checkMediaFile(mediaDir.getPath())) {
            return;
        }

        MusicPlayerApp.playerName = mediaDir.getName();

        Message message = Message.obtain();
        message.arg1 = CMD_PLAY;
        message.obj = mediaFilePath;
        message.replyTo = mMessenger;

        try {
            remoteMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mButtonPlayPause.setText("Pause");
    }

    private void checkPermission(){
        //Android平台版本，如我的版本为Android 7.1.2
        Log.i(TAG,"Build.VERSION.RELEASE----->" + Build.VERSION.RELEASE);
        //当前手机版本-API版本号
        Log.i(TAG,"android.os.Build.VERSION.SDK_INT----->" + Build.VERSION.SDK_INT);
        //android 6.0 对应的 API版本号23
        Log.i(TAG,"Build.VERSION_CODES.M----->" + Build.VERSION_CODES.M);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//android 6.0以上
            int readPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            Log.i(TAG, "readPermission:" + readPermission);

            if (readPermission != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Log.i(TAG, "测试手机版本为：android 6.0以上--->申请读写权限被拒绝");
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                }
                else{
                    Log.i(TAG, "测试手机版本为：android 6.0以上--->未申请--->申请读写权限");
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                }
            }else{
                Log.i(TAG,"测试手机版本为：android 6.0以上--->已申请");
                MusicPlayerApp.getInstance().readMediaDirInfo();
                showPlayDirs();
            }
        }
        else {//android 6.0以下
            Log.i(TAG,"测试手机版本为：android 6.0以下");
            MusicPlayerApp.getInstance().readMediaDirInfo();
            showPlayDirs();
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            cmd = msg.arg1;//获取Service消息中的cmd
            Log.i(TAG, "handleMessage:" + cmd);

            if(CMD_SYNC_POS == cmd){
                int currentPosition = (int)msg.obj;
                Log.i(TAG, "currentPosition:" + currentPosition);

                mSeekBarVolume.setMax(100);
                mSeekBarVolume.setProgress(currentPosition);
            }
            if(CMD_FINISH == cmd){
            }
        }
    };

    private void bindService() {
        bindService(mIntent, mMusicPlayerServiceConn, Context.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        unbindService(mMusicPlayerServiceConn);
        mMusicPlayerService = null;
    }

    private MusicPlayerService mMusicPlayerService;
    private ServiceConnection mMusicPlayerServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            remoteMessenger = new Messenger(iBinder);//使用Service返回的IBinder对象初始化Service端信使对象
            mMessenger = new Messenger(handler);//初始化本地客户端信使对象

            Message message = Message.obtain();
            message.arg1 = CMD_INIT;
            message.replyTo = mMessenger;//将客户端的信使对象保存到message中，通过Service端的信使对象发送给Service

            try {
                remoteMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    public void searchMedia(View view) {
        final String mediaPath = Environment.getExternalStorageDirectory().getPath();
        Log.i(TAG, "getExternalStorageDirectory:" + mediaPath);

        handler.post(new Runnable() {
            private ArrayList<String> mMediaDirs = new ArrayList<String>();
            private void searchFiles(String path){
                File rootFile = new File(path);

                if(! rootFile.exists()){
                    Log.i(TAG, path + ":file not exists");
                    return;
                }

                File[] sonFiles = rootFile.listFiles();
                if(null == sonFiles || 0 == sonFiles.length){
                    Log.i(TAG, path + ":list files empty");
                    return;
                }

                for(File sonFile : sonFiles){
                    Log.i(TAG, "sonFile:" + sonFile);
                    if(sonFile.isDirectory()){
                        searchFiles(sonFile.getPath());
                    }
                    else {
                        String filePath = sonFile.getPath();

                        Log.i(TAG, "filePath:" + filePath);

                        if(MediaItem.checkMediaFile(filePath)){
                            if(mMediaDirs.isEmpty())
                            {
                                mMediaDirs.add(path);
                            }
                            else
                            {
                                boolean exists = false;
                                for(String existsPath : mMediaDirs){
                                    if(existsPath.equals(path)){
                                        exists = true;
                                        break;
                                    }
                                }
                                if(!exists) {
                                    mMediaDirs.add(path);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void run() {
                searchFiles(mediaPath);
                Log.i(TAG, "MusicPlayerApp.mediaItems size:" + mMediaDirs.size());

                MusicPlayerApp.getInstance().mMediaDirs = mMediaDirs;
                MusicPlayerApp.getInstance().saveMediaDirInfo();
                showPlayDirs();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.i(TAG, "onItemClick position:" + position);

        MediaItem mediaItem = MusicPlayerApp.mediaItems.get(position);
        String mediaDirPath = mediaItem.getPath();

        Log.i(TAG, "mediaDir mediaDirPath:" + mediaDirPath);

        if(mediaDirPath.equals("/")){
            showPlayDirs();
        }
        else if(MediaItem.checkMediaFile(mediaDirPath)){
            TextView playItemTextView = (TextView)view;
            playItemTextView.setTextColor(Color.argb(0xff, 0xd6,0x1b,0x5f));
            MusicPlayerApp.getInstance().playerItemPosition = position;
            PlayFile(mediaItem);
        }
        else {
            showPlayFiles(mediaDirPath);
        }
    }

    public void buttonPrevClick(View view) {
        int position = MusicPlayerApp.getInstance().playerItemPosition;
        position = (position>1) ? position - 1: 0;

        position = Math.max(position, 1);

        Log.i(TAG, "buttonPrevClick position:" + position + " playerItemMax:" + MusicPlayerApp.getInstance().playerItemMax);

        MediaItem mediaItem = MusicPlayerApp.mediaItems.get(position);
        PlayFile(mediaItem);

        MusicPlayerApp.getInstance().playerItemPosition = position;
    }

    public void buttonPlayPauseClick(View view) {
        int cmd = CMD_PLAY;

        if(mButtonPlayPause.getText().toString().equals("Pause")){
            cmd = CMD_PAUSE;
            mButtonPlayPause.setText("Play");
        }
        else{
            mButtonPlayPause.setText("Pause");
        }

        Message message = Message.obtain();
        message.arg1 = cmd;
        message.replyTo = mMessenger;

        try {
            remoteMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void buttonNextClick(View view) {
        int position = MusicPlayerApp.getInstance().playerItemPosition;
        position = (position < (MusicPlayerApp.getInstance().playerItemMax-1)) ?  position + 1 : 0;
        position = Math.max(position, 1);

        Log.i(TAG, "buttonNextClick position:" + position + " playerItemMax:" + MusicPlayerApp.getInstance().playerItemMax);

        MediaItem mediaItem = MusicPlayerApp.mediaItems.get(position);
        PlayFile(mediaItem);

        MusicPlayerApp.getInstance().playerItemPosition = position;
    }

    public void buttonModeClick(View view) {
        String playMode = MusicPlayerApp.playerMode;

        switch(playMode){
            case "All": playMode = "Rand"; break;
            case "Rand": playMode = "One"; break;
            case "One": playMode = "All"; break;
        }

        MusicPlayerApp.playerMode = playMode;
        MusicPlayerApp.saveMode();
        mButtonMode.setText(playMode);

        Message message = Message.obtain();
        message.arg1 = CMD_SET_MODE;
        message.obj = playMode;
        message.replyTo = mMessenger;

        try {
            remoteMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(KeyEvent.KEYCODE_BACK == keyCode){
            showPlayDirs();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
