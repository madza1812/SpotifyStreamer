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
    private final String INTENT_ACTION_LAUNCH_UI = "ui_launch_action_intent";

    private final String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final String KEY_TRACK_POSITION = "track_position";
    private final String KEY_ACTION = "action_argument";

    private final String MUSIC_PLAYER_FRAGMENT_POPUP_TAG = "player_ui_popup";

    private final String NOW_PLAYING_MENU_TITTLE = "Now Playing";
    private final int MENU_ACTION_LAUNCH_UI = 100;

    private ApplicationManager appManager;
    private Display display;

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

        // FragmentManager.enableDebugLogging(true);
        // Start bindingPlayMusicService and report to Application Manager
        this.appManager = (ApplicationManager) this.getApplication();
        appManager.acquireBinding();

        // Start a binding to PlayMusicService
        Intent intent = new Intent(this, PlayMusicService.class);
        this.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        Log.v(TAG, "Screen size: height = " + this.getResources().getConfiguration().screenHeightDp +
        ", width = " + this.getResources().getConfiguration().screenWidthDp);

        // Restore From Rotation
        if (savedInstanceState != null){
            Log.v(TAG, "Recovery from SCREEN ROTATION !");
        }
        if ( isLargeWidth()) {
            // Tablet screen
            setContentView(R.layout.activity_artist_search);

        } else {
            setContentView(R.layout.activity_artist_search);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(ACTIONBAR_BG_COLOR)));
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
        outState.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList());
        outState.putInt(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());

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
        if (isFinishing()) {
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
    }
}
