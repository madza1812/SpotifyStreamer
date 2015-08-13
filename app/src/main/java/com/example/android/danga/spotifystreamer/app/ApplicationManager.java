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
    private int activityPosition = 0; //Use to determine the activity for restore after rotation. 0:SEARCH, 1:TOPTEN, 2:PLAYER;
    enum ScreenRotation {
        RegToReg,
        RegToLar,
        LarToLar,
        LarToReg
    };
    private ScreenRotation mScreenRotation;
    private ArrayList<TrackParcel> topTenTracks;
    private int trackPosition;

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

    public int getTrackPosition() {
        return trackPosition;
    }

    public void setTrackPosition(int trackPosition) {
        this.trackPosition = trackPosition;
    }

    public ArrayList<TrackParcel> getTopTenTracks() {
        return topTenTracks;
    }

    public void setTopTenTracks(ArrayList<TrackParcel> topTenTracks) {
        this.topTenTracks = topTenTracks;
    }

    public ScreenRotation getScreenRotation() {
        return mScreenRotation;
    }

    public void setScreenRotation(ScreenRotation mScreenRotation) {
        this.mScreenRotation = mScreenRotation;
    }

    public int getActivityPosition() {
        Log.v(LOG_TAG, "Activity POSITION = " + String.valueOf(activityPosition));
        return activityPosition;
    }

    public void setActivityPosition(int activityPosition) {
        this.activityPosition = activityPosition;
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
