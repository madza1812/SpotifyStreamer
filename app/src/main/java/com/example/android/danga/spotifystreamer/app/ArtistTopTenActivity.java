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
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;


public class ArtistTopTenActivity extends AppCompatActivity{

    final String TAG = ArtistTopTenActivity.class.getSimpleName();

    private final String ACTIONBAR_BG_COLOR = "#E65100";

    private final String ARGUMENT_ACTION_LAUNCH_UI = "ui_launch_action_argument";
    private final String INTENT_ACTION_LAUNCH_UI = "ui_launch_action_intent";

    private final String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final static String KEY_TRACK_POSITION = "track_position";
    private final String KEY_ACTION = "action_argument";
    private final String KEY_ARTIST_NAME_ID = "id_name_artist";

    private final String MUSIC_PLAYER_FRAGMENT_POPUP_TAG = "player_ui_popup";
    private final String MUSIC_PLAYER_FRAGMENT_FULLSCREEN_TAG = "player_ui_fullscreen";
    private final String ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG = "artist_top_ten_large_screen";

    private final int MENU_ACTION_LAUNCH_UI = 100;
    private final int NULL_VALUE = 100;

    private ApplicationManager appManager;
    private Display display;
    private int currentOrientation;
    private boolean curLargeScreen;

    private PlayMusicService playMusicSrv;
    private static boolean srvBound = false;

    // Create the connection to PlayMusicService
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Service is Bound for ArtistDetail !");
            PlayMusicService.ServicePlayBinder mBinder =
                    (PlayMusicService.ServicePlayBinder) service;
            playMusicSrv = mBinder.getService();
            srvBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "Service is UNBOUND for ArtistDetail !");
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

        // Start bindingPlayMusicService and report to Application Manager
        this.appManager = (ApplicationManager) getApplication();
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        currentOrientation = display.getRotation();
        curLargeScreen = isLargeWidth();
        Log.v(TAG, "onCREATE srvBound is " + String.valueOf(srvBound));
        if (isLargeWidth()) {
            finish();
            return;
        } else {
            Log.v(TAG, "REGULAR SCREEN IS DETECTED IN ArtistTopTenActivity!");
            // Inflate the Top Ten Fragment
            setContentView(R.layout.activity_artist_top_ten);
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.artist_detail_fragment, new ArtistSearchFragment());
            }

            Intent intentReceived = getIntent();
            String artistName = "";

            // Set action bar title and background color
            if (intentReceived != null && intentReceived.hasExtra(KEY_ARTIST_NAME_ID))
                artistName = intentReceived.getStringArrayExtra(KEY_ARTIST_NAME_ID)[0];
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setSubtitle(artistName);
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(ACTIONBAR_BG_COLOR)));
            }

            // Binding to the PlayMusicService
            if (!srvBound) {
                Log.v(TAG, "onCREATE in REGULAR SCREEN srvBound is "+String.valueOf(srvBound));
                appManager.acquireBinding();
                // Start a binding to PlayMusicService
                Intent intentBinding = new Intent(getApplicationContext(), PlayMusicService.class);
                bindService(intentBinding, mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 11)
            invalidateOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "START onSaveInstanceState of ArtistTopTenActivity !");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing())
            appManager.setTopTenTracks(null);
        Log.v(TAG, "onDESTROY srvBound is " + String.valueOf(srvBound));
        display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int newOrientation = display.getRotation();
        ApplicationManager.ScreenRotation rotationTye =
                Util.getScreenRotationType(curLargeScreen, isLargeWidth());
        if (rotationTye == ApplicationManager.ScreenRotation.RegToLar
                || rotationTye == ApplicationManager.ScreenRotation.RegToReg) {
            Log.v(TAG, "onDESTROY UNBINDING THE SERVICE FOR ARTIST TOP TEN ACTIVITY !");
            // Unbinding the PlayMusicService
            this.unbindService(mServiceConnection);
            this.appManager.releaseBinding();
            srvBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (playMusicSrv.getMPState() == PlayMusicService.State.Started
                || playMusicSrv.getMPState() == PlayMusicService.State.Paused) {
            menu.add(0, MENU_ACTION_LAUNCH_UI, 0, "Player")
                    .setIcon(R.drawable.ic_now_playing_music_white)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        getMenuInflater().inflate(R.menu.menu_artist_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                Log.e(TAG, "Up is pressed!");
                if (Build.VERSION.SDK_INT >= 16) {
                    finish();
                    overridePendingTransition(R.animator.enter_back, R.animator.exit_back);
                } else {
                    NavUtils.navigateUpTo(this, new Intent(this, ArtistSearchActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                }
                return true;
            case MENU_ACTION_LAUNCH_UI:
                if (isLargeWidth()) {
                    showMPDialogFragment();
                } else {
                    Intent launchUiIntent = new Intent(getApplicationContext(), MusicPlayerActivity.class);
                    launchUiIntent.setAction(INTENT_ACTION_LAUNCH_UI);
                    launchUiIntent.putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList())
                            .putExtra(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());
                    launchDetailActivity(launchUiIntent);
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

    public void launchDetailActivity (Intent intent) {
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            if (Build.VERSION.SDK_INT >=16) {
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
    public void onBackPressed() {
        super.onBackPressed();
        if (Build.VERSION.SDK_INT >= 16)
            overridePendingTransition(R.animator.enter_back, R.animator.exit_back);
    }

    private boolean isLargeWidth(){
        return getResources().getBoolean(R.bool.large_screen);
    }
}
