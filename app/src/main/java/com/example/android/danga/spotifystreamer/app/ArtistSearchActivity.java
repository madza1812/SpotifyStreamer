package com.example.android.danga.spotifystreamer.app;

import android.app.ActivityOptions;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import java.util.ArrayList;


public class ArtistSearchActivity extends AppCompatActivity implements ActivityCommunicator {

    private final String TAG = ArtistSearchActivity.class.getSimpleName();

    private final String ACTIONBAR_BG_COLOR = "#E65100";

    private final String ARGUMENT_ACTION_LAUNCH_UI = "ui_launch_action_argument";
    private final String INTENT_ACTION_RESTORE_TOP_TEN = "top_ten_restore_action_intent";
    private final String INTENT_ACTION_LAUNCH_UI = "ui_launch_action_intent";
    private final String INTENT_ACTION_LAUNCH_UI_ROTATION = "rotation_ui_launch_action_intent";

    private final String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final String KEY_TRACK_POSITION = "track_position";
    private final String KEY_ACTION = "action_argument";

    private final String MUSIC_PLAYER_FRAGMENT_POPUP_TAG = "player_ui_popup";
    private final String MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG = "player_ui_fullscreen";
    private final String ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG = "artist_top_ten_large_screen";

    private final String NOW_PLAYING_MENU_TITTLE = "Now Playing";
    private final int MENU_ACTION_LAUNCH_UI = 100;
    private final int NULL_VALUE = 100;

    private ApplicationManager appManager;
    private static int currentOrientation;
    private Display display;
    private boolean uiToggle = false;
    private boolean curLargeScreen;


    private ArrayList<TrackParcel> topSavedTracks;
    private int currentTrackPosition;

    public FragmentCommunicator mFragComm;

    PlayMusicService playMusicSrv;
    private boolean srvBound = false;

    // Create the connection to PlayMusicService
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Service is Bound for ArtistSearchActivity !");
            PlayMusicService.ServicePlayBinder mBinder =
                    (PlayMusicService.ServicePlayBinder) service;
            playMusicSrv = mBinder.getService();
            srvBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "Service is UNBOUND for ArtistSearchActivity !");
            srvBound = false;
        }
    };

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

        // FragmentManager.enableDebugLogging(true);
        // Start bindingPlayMusicService and report to Application Manager
        this.appManager = (ApplicationManager) this.getApplication();
        appManager.acquireBinding();
        if (appManager.getActivityPosition()==2) {
            uiToggle = true;
            //appManager.setActivityPosition(0);
        }
        // Start a binding to PlayMusicService
        Intent intent = new Intent(this, PlayMusicService.class);
        this.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        // Get Screen Orientation
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        currentOrientation = display.getRotation();
        curLargeScreen = isLargeWidth();

        Log.v(TAG, "Screen size: height = " + this.getResources().getConfiguration().screenHeightDp +
        ", width = " + this.getResources().getConfiguration().screenWidthDp);

        Log.v(TAG, "onCREATE Screen Rotation: " + appManager.getScreenRotation());
        // Restore From Rotation (LarToReg)
        if (savedInstanceState != null){
            Log.v(TAG, "Recovery from SCREEN ROTATION !");
            if (appManager.getScreenRotation() == ApplicationManager.ScreenRotation.LarToReg){
                Log.v(TAG, "Handle LarToReg Screen Rotation !");
                // Start ArtistTopTenActivity
                Intent detailIntent = new Intent(this, ArtistTopTenActivity.class);
                Intent playerIntent = new Intent(this, MusicPlayerActivity.class);
                if (uiToggle) {
                    Log.v(TAG, "recoverFromLarToReg: prepare to launch ArtistTopTenActivity with Argument Action Launch UI !");
                    // Start ArtistTopTenActivity with argument action launch ui
                    if (appManager.getTopTenTracks() != null) {
                        detailIntent.setAction(INTENT_ACTION_RESTORE_TOP_TEN);
                        detailIntent.putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST,
                                savedInstanceState.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST));
                        if (savedInstanceState.getInt(KEY_TRACK_POSITION, NULL_VALUE) != NULL_VALUE)
                            detailIntent.putExtra(KEY_TRACK_POSITION,
                                    savedInstanceState.getInt(KEY_TRACK_POSITION));
                        detailIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        if (intent.resolveActivity(this.getPackageManager()) != null) {
                            startActivity(detailIntent);
                        }
                    }

                    // Start MusicPlayerActivity
                    Log.v(TAG, "LAUNCH INTENT OF MUSICPLAYERACTIVITY !");
                    playerIntent.setAction(INTENT_ACTION_LAUNCH_UI_ROTATION);
                    playerIntent.putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST,
                            savedInstanceState.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST));
                    if (savedInstanceState.getInt(KEY_TRACK_POSITION, NULL_VALUE) != NULL_VALUE){
                        playerIntent.putExtra(KEY_TRACK_POSITION,
                                savedInstanceState.getInt(KEY_TRACK_POSITION));
                    }
                    if (intent.resolveActivity(this.getPackageManager()) != null) {
                        startActivity(playerIntent);
                    }
                } else {
                    Log.v(TAG, "recoverFromLarToReg: prepare to launch ArtistTopTenActivity with Intent_Action_Restore_TopTen!");
                    detailIntent.setAction(INTENT_ACTION_RESTORE_TOP_TEN);
                    detailIntent.putExtra(KEY_TOP_TEN_TRACKS_LIST, appManager.getTopTenTracks());
                    launchDetailActivity(detailIntent);
                }

                //launchDetailActivity(detailIntent);
            } else if (appManager.getScreenRotation() == ApplicationManager.ScreenRotation.RegToLar){
                // Fragment will handle this !
                Log.v(TAG, "Handle RegToLar Screen Rotation !");
            }
            else if (appManager.getScreenRotation() == ApplicationManager.ScreenRotation.RegToReg){
                // Fragment will handle this !
                Log.v(TAG, "Handle RegToReg Screen Rotation !");
            }
            else if (appManager.getScreenRotation() == ApplicationManager.ScreenRotation.LarToLar){
                // Fragment will handle this !
                Log.v(TAG, "Handle LarToLar Screen Rotation !");
            }
        }
        if ( isLargeWidth()) {
            setContentView(R.layout.activity_artist_search);

        } else {
            setContentView(R.layout.activity_artist_search);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(ACTIONBAR_BG_COLOR)));
        }
    }

    public void launchDetailActivity (Intent intent) {
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            if (Build.VERSION.SDK_INT >= 16) {
                startActivity(intent,
                        ActivityOptions.makeCustomAnimation(this,
                                R.animator.enter,
                                R.animator.exit).toBundle());
            } else {
                startActivity(intent);
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        Log.v(TAG, "Fragment:" + fragment.toString() +"is attached to this activity: " + this.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.invalidateOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "START onSaveInstanceState of ArtistSearchActivity !");
        Log.v(TAG, "Large Screen = " + String.valueOf(isLargeWidth()));
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
        // Get Screen Orientation
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int newOrientation = display.getRotation();
        // End Testing
        if (currentOrientation != newOrientation)
            appManager.setScreenRotation(Util.getScreenRotationType(curLargeScreen, isLargeWidth()));
        ApplicationManager.ScreenRotation rotationType =
                Util.getScreenRotationType(curLargeScreen, isLargeWidth());
        if( rotationType == ApplicationManager.ScreenRotation.LarToReg) {
            outState.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList());
            outState.putInt(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());
        }
        if (rotationType == ApplicationManager.ScreenRotation.RegToLar) {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());
            bundle.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList());
            mFragComm.passToFragment(bundle);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if ( srvBound && (playMusicSrv.getMPState() == PlayMusicService.State.Started
                || playMusicSrv.getMPState() == PlayMusicService.State.Paused)) {
            menu.add(0, MENU_ACTION_LAUNCH_UI, 0, NOW_PLAYING_MENU_TITTLE)
                    .setIcon(R.drawable.ic_now_playing_music_white)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case MENU_ACTION_LAUNCH_UI:
                if (isLargeWidth()) {
                    showMPDialogFragment();
                } else {
                    Intent launchUiIntent = new Intent(this.getApplicationContext(), MusicPlayerActivity.class);
                    launchUiIntent.setAction(INTENT_ACTION_LAUNCH_UI);
                    launchUiIntent.putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList())
                            .putExtra(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());
                    if (launchUiIntent.resolveActivity(this.getPackageManager()) != null)
                        startActivity(launchUiIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showMPDialogFragment() {
        FragmentManager mFragmentManager = getFragmentManager();
        DialogFragment playerUiFrag = (MusicPlayerFragment)
                mFragmentManager.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
        if (playerUiFrag == null) {
            playerUiFrag = MusicPlayerFragment.newInstance();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList());
            bundle.putString(KEY_ACTION, ARGUMENT_ACTION_LAUNCH_UI);
            bundle.putInt(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());
            playerUiFrag.setArguments(bundle);
            playerUiFrag.show(mFragmentManager, MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
        }
    }

    @Override
    protected void onDestroy() {
        // Get Screen Orientation
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int newOrientation = display.getRotation();
        // Not Screen Rotation
        if (isFinishing() && newOrientation == currentOrientation) {
            Log.v(TAG, "onDESTROY : activity is FINISHING and STOPPING THE SERVICE !");
            // Unbinding the PlayMusicService
            if (playMusicSrv.getMPState() == PlayMusicService.State.Started
                    || playMusicSrv.getMPState() == PlayMusicService.State.Paused) {
                playMusicSrv.stopMusic();
            }
            this.unbindService(mServiceConnection);
            this.appManager.doStopService();
        } else {
            Log.v(TAG, "onDESTROY : activity is NOT FINISHING just UNBINDING THE SERVICE !");
            this.unbindService(mServiceConnection);
            this.appManager.releaseBinding();
        }
        super.onDestroy();
    }

    private boolean isLargeWidth(){
        return getResources().getBoolean(R.bool.large_screen);
    }
    @Override
    public void passToAcivity(Bundle bundle) {
        // Obtain Bundle data from fragments and save for restoring during rotation.
        topSavedTracks = bundle.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST);
        currentTrackPosition = bundle.getInt(KEY_TRACK_POSITION, NULL_VALUE);
        if (topSavedTracks != null)
            Log.v(TAG, "TOP SAVED TRACKS is NOT NULL !");
        if (currentTrackPosition != NULL_VALUE)
            Log.v(TAG, "CURRENT TRACK POSITION is NOT NULL !");
    }
}
