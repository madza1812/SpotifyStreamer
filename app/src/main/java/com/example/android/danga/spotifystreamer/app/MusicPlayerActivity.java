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

    private final String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final String KEY_TRACK_POSITION = "track_position";
    private final String KEY_ACTION = "action_argument";

    private final String MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG = "player_ui_fullscreen";
    private final String MUSIC_PLAYER_FRAGMENT_POPUP_TAG = "player_ui_popup";

    private final String ACTION_LAUNCH_UI = "com.example.android.danga.spotifystreamer.app.LAUNCH_UI";

    private ApplicationManager appManager;

    private ArrayList<TrackParcel> topTenTracks;

    private final int NULL_VALUE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCREATE");

        this.appManager = (ApplicationManager) getApplication();
        if (isLargeWidth()) {
            Log.v(TAG, "LARGE SCREEN IS DETECTED IN PLAYER UI ACTIVITY!");
            /*setTheme(R.style.PopupTheme);
            //this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
            setContentView(R.layout.activity_player);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = this.getWindow().getAttributes();
            Display display = this.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            boolean landscape =
                    (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
            if (isLargeWidth()) {
                if (landscape) {
                    display.getSize(size);
                    params.width = (int) (size.x * 0.7f);
                    params.height = (int) (size.y * 0.8f);
                } else {
                    display.getSize(size);
                    params.width = size.x;
                    params.height = (int) (size.y * 0.7f);
                }
            }
            params.alpha = 1.0f;
            params.dimAmount = 0.5f;
            this.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
            showDialog();*/
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
    public void showDialog() {
        FragmentManager mFragmentManager = getFragmentManager();
        DialogFragment playerUiFrag = (MusicPlayerFragment)
                mFragmentManager.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
        if (playerUiFrag == null) {
            playerUiFrag = MusicPlayerFragment.newInstance();
            playerUiFrag.show(mFragmentManager, MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
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
