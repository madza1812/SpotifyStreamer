package com.example.android.danga.spotifystreamer.app;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by An on 7/4/2015.
 */
public class ApplicationManager extends Application {

    private static ApplicationManager appManager;
    private Intent intentService;
    private final String APPLICATION_START = "com.example.android.danga.spotifystreamer.app.ApplicationManager.APPLICATION_START";
    private final String LOG_TAG = ApplicationManager.class.getSimpleName();
    private AtomicInteger clientCount = new AtomicInteger();

    public ApplicationManager getAppManager() {
        return appManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Start the PlayMusicService
        intentService = new Intent(this, PlayMusicService.class);
        intentService.setAction(APPLICATION_START);
        Log.v(LOG_TAG, "Starting the PlayMusicService from Application Manager !");
        this.startService(intentService);
    }

    public void acquireBinding() {
        clientCount.incrementAndGet();
        Log.v(LOG_TAG, "Client Count is: " + clientCount);
    }
    public void doStopService() {
        Log.v(LOG_TAG, "in releaseBinding Client Count is: " + clientCount);
        //if (clientCount.get() == 0 || clientCount.decrementAndGet() == 0) {
        Log.v(LOG_TAG, "GONNA STOP THE SERVICE !");
        // Stop PlayMusicService
        intentService = new Intent(this, PlayMusicService.class);
        stopService(intentService);

    }
    public void releaseBinding() {
        clientCount.decrementAndGet();
        Log.v(LOG_TAG, "in releaseBinding Client Count is: " + clientCount);
    }
}
