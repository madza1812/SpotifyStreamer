package com.example.android.danga.spotifystreamer.app;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.WindowManager;


public class MusicPlayerActivity extends AppCompatActivity {

    public final static String TAG = MusicPlayerActivity.class.getSimpleName();

    private final String ACTIONBAR_BG_COLOR = "#E65100";
    
    private final String MUSIC_PLAYER_FRAGMENT_POPUP_TAG = "player_ui_popup";
    private final String MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG = "player_ui_fullscreen";
    private final String ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG = "artist_top_ten_large_screen";

    private ApplicationManager appManager;
    private Display display;
    private int currentOrientation;
    private boolean curLargeScreen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCREATE");
        // Begin Fragment Testing
        FragmentManager fm = getFragmentManager();
        Log.v(TAG, "Number of entries in backstack: " + fm.getBackStackEntryCount());
        if (isLargeWidth()) {
            ArtistSearchFragment asf = (ArtistSearchFragment) fm.findFragmentById(R.id.artist_search_fragment);
            ArtistTopTenFragment attf = (ArtistTopTenFragment) fm.findFragmentByTag(ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG);
            MusicPlayerFragment mpf = (MusicPlayerFragment) fm.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
            Log.v(TAG, "ArtistSearchFragment == NULL is : " + String.valueOf(asf == null));
            Log.v(TAG, "ArtistTopTenFragment == NULL is : " + String.valueOf(attf == null));
            Log.v(TAG, "MusicPlayerFragment == NULL is : " + String.valueOf( mpf == null));
        } else {
            ArtistSearchFragment asf = (ArtistSearchFragment) fm.findFragmentById(R.id.artist_search_fragment);
            ArtistTopTenFragment attf = (ArtistTopTenFragment) fm.findFragmentById(R.id.artist_detail_fragment);
            MusicPlayerFragment mpf = (MusicPlayerFragment) fm.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG);
            Log.v(TAG, "ArtistSearchFragment == NULL is : " + String.valueOf(asf == null));
            Log.v(TAG, "ArtistTopTenFragment == NULL is : " + String.valueOf(attf == null));
            Log.v(TAG, "MusicPlayerFragment == NULL is : " + String.valueOf( mpf == null));
        }
        // End Fragment Testing

        this.appManager = (ApplicationManager) getApplication();
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        currentOrientation = display.getRotation();
        curLargeScreen = isLargeWidth();

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

        // Get Screen Orientation
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int newOrientation = display.getRotation();
        // End Testing
        if (currentOrientation != newOrientation)
            appManager.setScreenRotation(Util.getScreenRotationType(curLargeScreen, isLargeWidth()));

        // Begin Fragment Testing
        FragmentManager fm = getFragmentManager();
        Log.v(TAG, "Number of entries in backstack: " + fm.getBackStackEntryCount());
        if (isLargeWidth()) {
            ArtistSearchFragment asf = (ArtistSearchFragment) fm.findFragmentById(R.id.artist_search_fragment);
            ArtistTopTenFragment attf = (ArtistTopTenFragment) fm.findFragmentByTag(ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG);
            MusicPlayerFragment mpf = (MusicPlayerFragment) fm.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
            Log.v(TAG, "ArtistSearchFragment == NULL is : " + String.valueOf(asf == null));
            Log.v(TAG, "ArtistTopTenFragment == NULL is : " + String.valueOf(attf == null));
            Log.v(TAG, "MusicPlayerFragment == NULL is : " + String.valueOf( mpf==null));
        } else {
            ArtistSearchFragment asf = (ArtistSearchFragment) fm.findFragmentById(R.id.artist_search_fragment);
            ArtistTopTenFragment attf = (ArtistTopTenFragment) fm.findFragmentById(R.id.artist_detail_fragment);
            MusicPlayerFragment mpf = (MusicPlayerFragment) fm.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG);
            Log.v(TAG, "ArtistSearchFragment == NULL is : " + String.valueOf(asf == null));
            Log.v(TAG, "ArtistTopTenFragment == NULL is : " + String.valueOf(attf == null));
            Log.v(TAG, "MusicPlayerFragment == NULL is : " + String.valueOf( mpf==null));
        }
        // End Fragment Testing
        super.onSaveInstanceState(outState);
    }

    private boolean isLargeWidth(){
        return getResources().getBoolean(R.bool.large_screen);
    }
}
