package com.nejidev.media;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerApp extends Application {
    private static final String TAG = "MusicPlayerApp";
    private static MusicPlayerApp mMusicPlayerApp;
    public static List<String> mMediaDirs = new ArrayList<String>();

    public static int playerItemMax = 0;
    public static int playerItemPosition = 0;
    public static String playerMode = "";
    public static String playerName = "";
    public static List<MediaItem> mediaItems = new ArrayList<MediaItem>();

    public static void readMediaDirInfo(){
        SharedPreferences sharedPreferences = getInstance().getApplicationContext().getSharedPreferences("MediaDirInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String dirs = sharedPreferences.getString("dirs", "");

        Log.i(TAG, "readMediaDirInfo dirs:" + dirs);

        if(null != dirs){
            for(String dirPath : dirs.split("#")){
                File dirFile = new File(dirPath);
                if(dirFile.exists()){
                    mMediaDirs.add(dirPath);
                }
            }
        }
    }

    public static void saveMediaDirInfo(){
        SharedPreferences sharedPreferences = getInstance().getApplicationContext().getSharedPreferences("MediaDirInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String dirs = "";
        for(String dir : mMediaDirs){
            dirs = dirs + dir + "#";
        }
        editor.putString("dirs", dirs);
        editor.commit();

        Log.i(TAG, "saveMediaDirInfo dirs:" + dirs);
    }

    public static void readMode(){
        SharedPreferences sharedPreferences = getInstance().getApplicationContext().getSharedPreferences("MediaDirInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String mode = sharedPreferences.getString("mode", "");

        Log.i(TAG, "readMediaDirInfo mode:" + mode);

        MusicPlayerApp.playerMode = mode;
    }

    public static void saveMode(){
        SharedPreferences sharedPreferences = getInstance().getApplicationContext().getSharedPreferences("MediaDirInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("mode", MusicPlayerApp.playerMode);
        editor.commit();

        Log.i(TAG, "saveMediaDirInfo mode:" + MusicPlayerApp.playerMode);
    }

    public static MusicPlayerApp getInstance(){
        return mMusicPlayerApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMusicPlayerApp = this;
        readMode();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
