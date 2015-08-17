package com.example.android.danga.spotifystreamer.app;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;


public class MusicPlayerActivity extends AppCompatActivity {

    public final static String TAG = MusicPlayerActivity.class.getSimpleName();

    private final String ACTIONBAR_BG_COLOR = "#E65100";

    private final String MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG = "player_ui_fullscreen";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCREATE");

        if (isLargeWidth()) {
            Log.v(TAG, "LARGE SCREEN IS DETECTED IN PLAYER UI ACTIVITY!");
            finish();
            return;
        } else {
            Log.v(TAG, "REGULAR SCREEN IS DETECTED IN PLAYER UI ACTIVITY!");
            setContentView(R.layout.activity_player);
            showFullScreenDialog();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(ACTIONBAR_BG_COLOR)));
        }
    }

    public void showFullScreenDialog() {
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        DialogFragment playerUiFrag = (MusicPlayerFragment)
                mFragmentManager.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG);
         if (playerUiFrag != null) {
             Log.v(TAG, "Found MusicPlayerFragment with ID !");
             ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
             ft.replace(R.id.player_ui_fragment, playerUiFrag, MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG)
             .addToBackStack(null)
             .commit();

        } else {
            Log.v(TAG, "Create New Music Player Fragment !");
            playerUiFrag = MusicPlayerFragment.newInstance();
             ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
             ft.add(R.id.player_ui_fragment, playerUiFrag, MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG)
                     .addToBackStack(null)
                     .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case android.R.id.home:
                Log.v(TAG, "Up is pressed !");
                if (Build.VERSION.SDK_INT >= 16) {
                    finish();
                    overridePendingTransition(R.animator.enter_back, R.animator.exit_back);
                } else {
                    NavUtils.navigateUpTo(this, new Intent(this, ArtistSearchActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (Build.VERSION.SDK_INT >= 16)
            overridePendingTransition(R.animator.enter_back, R.animator.exit_back);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "START onSaveInstanceState of MusicPlayerActivity !");
        super.onSaveInstanceState(outState);
    }

    private boolean isLargeWidth(){
        return getResources().getBoolean(R.bool.large_screen);
    }
}
