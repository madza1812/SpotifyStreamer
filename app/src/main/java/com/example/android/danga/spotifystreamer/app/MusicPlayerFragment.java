package com.example.android.danga.spotifystreamer.app;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * A placeholder fragment containing a simple view.
 */
public class MusicPlayerFragment extends DialogFragment {

    public final String TAG = MusicPlayerFragment.class.getSimpleName();

    private final String ACTION_LAUNCH_UI = "com.example.android.danga.spotifystreamer.app.LAUNCH_UI";
    private final String ACTION_START_MP = "com.example.android.danga.spotifystreamer.app.START";
    private final String ARGUMENT_ACTION_LAUNCH_UI = "ui_launch_action_argument";
    private final String ARGUMENT_ACTION_LAUNCH_UI_ROTATION = "rotation_ui_launch_action_argument";
    private final String ARGUMENT_ACTION_START_NEW_PLAYLIST = "new_playlist_start_action_argument";
    private final String INTENT_ACTION_LAUNCH_UI = "ui_launch_action_intent";
    private final String INTENT_ACTION_LAUNCH_UI_ROTATION = "rotation_ui_launch_action_intent";
    private final String INTENT_ACTION_START_NEW_PLAYLIST = "new_playlist_start_action_intent";

    private final String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final String KEY_TRACK_POSITION = "track_position";
    private  final String KEY_MESSENGER = "messenger";

    private final int MP_PLAY = 0;
    private final int MP_PAUSE = 1;
    private final int MP_NEXT = 2;
    private final int MP_PREVIOUS = 3;

    private final String ZERO_TIME = "00:00";
    private final String TOTAL_TIME = "00:30";
    private final long PREVIEW_DURATION= 30000; // in milliseconds
    private final long DELAY_PROGRESS = 50; // in milliseconds

    private ArrayList<TrackParcel> topTrackList = new ArrayList<>();
    private int trackPosition;

    private ApplicationManager appManager;

    private View rootView;
    private PlayMusicService playMusicSrv;
    Context mContext;

    private static Handler mHandler = new Handler();
    private Runnable mRunnable;

    private static boolean srvBound = false;
    private final int NULL_VALUE = 100;



    //private Context context;

    @Bind(R.id.ui_artist_name_textview) TextView artistName;
    @Bind(R.id.ui_album_name_textview) TextView albumName;
    @Bind(R.id.ui_album_thumbnail_imageview) ImageView albumThumbnail;
    @Bind(R.id.ui_track_name_textview) TextView trackName;
    @Bind(R.id.ui_play_pause_button) ImageButton playPauseBtn;
    @Bind(R.id.ui_progress_seekbar) SeekBar seekBar;
    @Bind(R.id.ui_current_duartion_textview) TextView currentDuration;
    @Bind(R.id.ui_total_duration_textview) TextView totalDuration;
    @Bind(R.id.ui_previous_button) ImageButton previousBtn;
    @Bind(R.id.ui_next_button) ImageButton nextBtn;

    public MusicPlayerFragment() {
        mContext = getActivity();
    }
    public static MusicPlayerFragment newInstance () {
        return new MusicPlayerFragment();
    }

    // Connect to PlayMusicService
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "The Service is CONNECTED !");
            Log.v(TAG, "IBinder service is: " + service.toString());
            PlayMusicService.ServicePlayBinder mBinder =
                    (PlayMusicService.ServicePlayBinder) service;
            playMusicSrv = mBinder.getService();
            srvBound = true;
            // Get the Track and Playlist Information
            topTrackList = playMusicSrv.getPlayingList();
            trackPosition = playMusicSrv.getCurrentTrack();
            if (playMusicSrv.getMPState() == PlayMusicService.State.Started) {
                Log.v(TAG, "ROTATION and SENDING MESSAGE TO UPDATE THE UI ! PLAYING !");
                playMusicSrv.updateToUIFromMP(MP_PLAY);
            }
            if (playMusicSrv.getMPState() == PlayMusicService.State.Paused) {
                Log.v(TAG, "ROTATION and SENDING MESSAGE TO UPDATE THE UI ! PAUSED !");
                playMusicSrv.updateToUIFromMP(MP_PAUSE);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "Service is DISCONNECTED !");
            srvBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "MUSICPLAYERFRAGMENT onCREATE !");

        final String MUSIC_PLAYER_FRAGMENT_TAG = "player_ui_popup";

        final String KEY_ACTION = "action_argument";

        setHasOptionsMenu(true);

        this.appManager = (ApplicationManager) getActivity().getApplication();

        if (savedInstanceState == null) {
            // Processing intents from the Parent activity (ArtistTopTenActivity)
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                String action = intent.getAction();
                switch (action) {
                    // Start New Playlist in Regular Screen Mode
                    case INTENT_ACTION_START_NEW_PLAYLIST:
                        Log.v(TAG, "INTENT_ACTION_START_NEW_PLAYLIST");
                        topTrackList = intent.getParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST);
                        trackPosition = intent.getIntExtra(KEY_TRACK_POSITION, NULL_VALUE);
                        startAndBindPlayMusicService(INTENT_ACTION_START_NEW_PLAYLIST, topTrackList, trackPosition);
                        break;
                    // Launch UI from Notification or from NowPlaying in Regular Screen Mode
                    case INTENT_ACTION_LAUNCH_UI:
                        Log.v(TAG, "INTENT_ACTION_LAUNCH_UI");
                        topTrackList = intent.getParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST);
                        trackPosition = intent.getIntExtra(KEY_TRACK_POSITION, NULL_VALUE);
                        startAndBindPlayMusicService(INTENT_ACTION_LAUNCH_UI, topTrackList, trackPosition);
                        break;
                    default:
                        break;
                }
            }
            // Get Playlist and Selected Artist from the Argument
            if (isLargeWidth()) {
                Bundle bundle = getArguments();
                if (bundle != null) {
                    String action = bundle.getString(KEY_ACTION);
                    switch (action) {
                        case ARGUMENT_ACTION_START_NEW_PLAYLIST:
                            Log.v(TAG, "ARGUMENT_ACTION_START_NEW_PLAYLIST");
                            topTrackList = bundle.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST);
                            trackPosition = bundle.getInt(KEY_TRACK_POSITION, NULL_VALUE);
                            startAndBindPlayMusicService(ARGUMENT_ACTION_START_NEW_PLAYLIST, topTrackList, trackPosition);
                            break;
                        case ARGUMENT_ACTION_LAUNCH_UI:
                            Log.v(TAG, "ARGUMENT_ACTION_LAUNCH_UI");
                            topTrackList = bundle.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST);
                            trackPosition = bundle.getInt(KEY_TRACK_POSITION, NULL_VALUE);
                            startAndBindPlayMusicService(ARGUMENT_ACTION_LAUNCH_UI, topTrackList, trackPosition);
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            Log.v(TAG, "On Rotation !");
            topTrackList = savedInstanceState.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST);
            trackPosition = savedInstanceState.getInt(KEY_TRACK_POSITION);
            startAndBindPlayMusicService(INTENT_ACTION_LAUNCH_UI_ROTATION, topTrackList, trackPosition);
        }
    }

    public void startAndBindPlayMusicService(String action, ArrayList<TrackParcel> topTenList, int trackPos) {

        // Initialize intent
        Intent playMusicIntent = new Intent(getActivity().getApplicationContext(), PlayMusicService.class);

        // Initialize the Handler for Messenger to Communicate with the PlayMusicService
        Log.v(TAG, "Setting up Message Handler ! With Activity: " + getActivity().toString());
        Handler mIncomingHandler = new Handler(new IncomingHandler(getActivity()));
        Log.v(TAG, "Handler: " + mIncomingHandler.toString());
        playMusicIntent.putExtra(KEY_MESSENGER, new Messenger(mIncomingHandler))
                .putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST, topTenList)
                .putExtra(KEY_TRACK_POSITION, trackPos);

        // BIND (AND START)the service based on action
        switch (action){
            // Start New Playlist in Regular Screen Mode
            case INTENT_ACTION_START_NEW_PLAYLIST:
                playMusicIntent.setAction(ACTION_START_MP);
                getActivity().startService(playMusicIntent);
                getActivity().bindService(playMusicIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                appManager.acquireBinding();
                break;
            // Launch UI from Notification or from NowPlaying in Regular Screen Mode
            case INTENT_ACTION_LAUNCH_UI:
                playMusicIntent.setAction(ACTION_LAUNCH_UI);
                getActivity().startService(playMusicIntent);
                getActivity().bindService(playMusicIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                appManager.acquireBinding();
                break;
            // Launch UI after rotation from LarToReg
            case INTENT_ACTION_LAUNCH_UI_ROTATION:
                playMusicIntent.setAction(ACTION_LAUNCH_UI);
                getActivity().startService(playMusicIntent);
                getActivity().bindService(playMusicIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                appManager.acquireBinding();
                break;
            // Start New Playlist in Large Screen Mode
            case ARGUMENT_ACTION_START_NEW_PLAYLIST:
                playMusicIntent.setAction(ACTION_START_MP);
                getActivity().startService(playMusicIntent);
                getActivity().bindService(playMusicIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                appManager.acquireBinding();
                break;
            // Launch UI from NowPlaying in Large Screen Mode
            case ARGUMENT_ACTION_LAUNCH_UI:
                playMusicIntent.setAction(ACTION_LAUNCH_UI);
                getActivity().startService(playMusicIntent);
                getActivity().bindService(playMusicIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                appManager.acquireBinding();
                break;
            // Launch UI after rotation from RegToLar
            case ARGUMENT_ACTION_LAUNCH_UI_ROTATION:
                playMusicIntent.setAction(ACTION_LAUNCH_UI);
                getActivity().startService(playMusicIntent);
                getActivity().bindService(playMusicIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                appManager.acquireBinding();
                break;
            default:
                break;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "inside onCreateView ! PlayMusicService is NULL: " + String.valueOf(playMusicSrv == null));
        // Inflate the layout
        rootView = inflater.inflate(R.layout.fragment_player_ui, container, false);
        ButterKnife.bind(this, rootView);

        // Setup Like-ActionBar
        Toolbar popupActionBar = (Toolbar)rootView.findViewById(R.id.toolbar);
        //((AppCompatActivity) getActivity()).setSupportActionBar(popupActionBar);
        popupActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        ImageButton upButton = (ImageButton) popupActionBar.findViewById(R.id.upButton);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        popupActionBar.inflateMenu(R.menu.menu_player);
        popupActionBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        dismiss();
                        getActivity().finish();
                        break;
                    case R.id.action_share:
                        ShareActionProvider mShareActionProvider;
                        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
                        if (mShareActionProvider != null) {
                            mShareActionProvider.setShareIntent(doShare());
                        } else
                            Log.v(TAG, "Share Action Provider is null !");
                        break;
                }
                return true;
            }
        });
        // End setup Like-ActionBar

        if (savedInstanceState != null) {
            topTrackList = savedInstanceState.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST);
            trackPosition = savedInstanceState.getInt(KEY_TRACK_POSITION);
        }
        // Update Views of Track Name, Artist Name, Album Thumbnail, Durations...
        if (isLargeWidth()) {
            if (trackPosition != NULL_VALUE) {
                Log.v(TAG, "Updating the views !");
                updateMPViewForPopUpDialog(getActivity().getApplicationContext());
            } else {
                Log.v(TAG, "Track Position is NOT CORRECT !");
            }
        } else {
            updateMPView(getActivity().getApplicationContext());
        }
        // Setup Buttons Listeners (Play/Pause, Next and Previous)
        updateButtonLiseners(getActivity().getApplicationContext());
        Log.v(TAG, "onCreateView of Fragment is finished!");
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (!isLargeWidth()) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        Log.v(TAG, "Music Fragment is gonna start");
        super.onStart();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
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
            getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        }
        Log.v(TAG, "Params Width: " + params.width);
        Log.v(TAG, "Params Height: " + params.height);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "START onSaveInstanceState of PlayerFRAGMENT !");
        outState.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, playMusicSrv.getPlayingList());
        outState.putInt(KEY_TRACK_POSITION, playMusicSrv.getCurrentTrack());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        ShareActionProvider mShareActionProvider;

        inflater.inflate(R.menu.menu_player, menu);
        MenuItem item = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(doShare());
        } else
            Log.v(TAG, "Share Action Provider is null !");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this.getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Intent doShare() {
        Log.v(TAG, "doShare: extURl = " + topTrackList.get(trackPosition).getExtUrl());
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, topTrackList.get(trackPosition).getExtUrl());
        return shareIntent;
    }

    // Setup the Player UI View for Fullscreen.
    public void updateMPView(Context context) {

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int imInDp;
        imInDp = (int )(context.getResources().getConfiguration().screenWidthDp -
                (getResources().getDimension(R.dimen.activity_horizontal_margin)*2));
        int imSizeInPx = (int) (imInDp * displayMetrics.densityDpi / 160f);

        artistName.setText(topTrackList.get(trackPosition).getArtistNames());
        albumName.setText(topTrackList.get(trackPosition).getAlbumName());

        if (!topTrackList.get(trackPosition).getAlbumThumbnailUrl().isEmpty())
            Picasso.with(context)
                    .load(topTrackList.get(trackPosition).getAlbumThumbnailUrl())
                    .resize(imSizeInPx, imSizeInPx)
                    .centerInside()
                    .placeholder(R.drawable.loading_icon)
                    .error(R.drawable.ic_photo_black_48dp)
                    .into(albumThumbnail);
        else
            Picasso.with(getActivity()
                    .getApplicationContext())
                    .load(R.drawable.ic_photo_black_48dp)
                    .resize(imSizeInPx, imSizeInPx)
                    .centerInside()
                    .placeholder(R.drawable.loading_icon)
                    .into(albumThumbnail);

        trackName.setText(topTrackList.get(trackPosition).getName());
        playPauseBtn.setImageResource(R.drawable.ic_pause);
        seekBar.setProgress(0);
        currentDuration.setText(ZERO_TIME);
        totalDuration.setText(TOTAL_TIME);
    }

    // Setup Player UI View for Pop Dialog
    public void updateMPViewForPopUpDialog (Context context) {

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int imInDp;
        imInDp = (int )(context.getResources().getConfiguration().screenWidthDp -
                (getResources().getDimension(R.dimen.activity_horizontal_margin)*4));
        int imSizeInPx = (int) (imInDp * displayMetrics.densityDpi / 160f);

        // Setup the Like-ActionBar for popup Media Player UI
        Toolbar popUpActionBar = (Toolbar) rootView.findViewById(R.id.toolbar);
        popUpActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        artistName.setText(topTrackList.get(trackPosition).getArtistNames());
        albumName.setText(topTrackList.get(trackPosition).getAlbumName());

        if (!topTrackList.get(trackPosition).getAlbumThumbnailUrl().isEmpty())
            Picasso.with(context)
                    .load(topTrackList.get(trackPosition).getAlbumThumbnailUrl())
                    .resize(imSizeInPx, imSizeInPx)
                    .centerInside()
                    .error(R.drawable.ic_photo_black_48dp)
                    .placeholder(R.drawable.loading_icon)
                    .into(albumThumbnail);
        else
            Picasso.with(context)
                    .load(R.drawable.ic_photo_black_48dp)
                    .resize(imSizeInPx, imSizeInPx)
                    .centerInside()
                    .placeholder(R.drawable.loading_icon)
                    .into(albumThumbnail);

        trackName.setText(topTrackList.get(trackPosition).getName());
        playPauseBtn.setImageResource(R.drawable.ic_pause);
        seekBar.setProgress(0);
        currentDuration.setText(ZERO_TIME);
        totalDuration.setText(TOTAL_TIME);
    }

    // Set Button Listeners
    public void updateButtonLiseners(final Context context) {
        // On Click Play/Pause Button
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playMusicSrv.getMPState()) {
                    case Started:
                        playPauseBtn.setImageResource(R.drawable.ic_play);
                        playMusicSrv.pauseMusic();
                        break;
                    case Paused:
                        playPauseBtn.setImageResource(R.drawable.ic_pause);
                        playMusicSrv.resumeMusic();
                        break;
                    default:
                        Log.v(TAG, "MP STATE either Started or Paused!");
                        break;
                }
            }
        });

        //On Click Previous Button
        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusicSrv.previousMusic();
                trackPosition--;
                if (trackPosition < 0)
                    trackPosition = topTrackList.size() - 1;
                // Update Views
                updateMPView(context);
            }
        });

        //On Click Next Button
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusicSrv.nextMusic();
                trackPosition++;
                if (trackPosition == topTrackList.size())
                    trackPosition = 0;
                // Update Views
                updateMPView(context);
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    private boolean isLargeWidth(){
            return getActivity().getResources().getBoolean(R.bool.large_screen);
    }

    @Override
    public void onDestroy() {
        if(mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
            //mHandler = null;
        }
        Log.v(TAG, "MusicPlayerFragment is gonna be DESTROYED ! UNBINDING THE SERVICE !");
        if (srvBound) {
            // Unbind Service
            getActivity().unbindService(mServiceConnection);
            srvBound = false;
        }
        this.appManager.releaseBinding();
        super.onDestroy();
    }

    class IncomingHandler implements Handler.Callback {
        private Context context;
        public IncomingHandler (Context context) {
            this.context = context;
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.v(TAG, "Message is received with WHAT is: " + String.valueOf(msg.what));
            Log.v(TAG, "Message is handled by Incoming Handler: " + this.toString());
            ImageButton playPauseBtn = (ImageButton) rootView.findViewById(R.id.ui_play_pause_button);
            switch (msg.what){
                case MP_NEXT:
                    Log.v(TAG, "ACTION NEXT RECEIVED !");
                    // Only update the View when current track position is different
                    if (trackPosition != playMusicSrv.getCurrentTrack()) {
                        Log.v(TAG, "VIEW WILL BE UPDATED !");
                        trackPosition = playMusicSrv.getCurrentTrack();
                        topTrackList = playMusicSrv.getPlayingList();
                        // Update Views when connected to PlayMusicService
                        if (srvBound)
                            if (isLargeWidth())
                                updateMPViewForPopUpDialog(context);
                            else
                                updateMPView(context);
                    }
                    break;
                case MP_PREVIOUS:
                    Log.v(TAG, "ACTION PREVIOUS RECEIVED !");
                    // Only update the View when current track position is different
                    if (trackPosition != playMusicSrv.getCurrentTrack()) {
                        trackPosition = playMusicSrv.getCurrentTrack();
                        topTrackList = playMusicSrv.getPlayingList();
                        // Update Views when Connected to PlayMusicService
                        if (srvBound)
                            if (isLargeWidth())
                                updateMPViewForPopUpDialog(context);
                            else
                                updateMPView(context);
                    }
                    break;

                case MP_PAUSE:
                    Log.v(TAG, "ACTION PAUSE RECEIVED !");
                    // Update the View on Pause
                    if (srvBound) {
                        if (trackPosition != playMusicSrv.getCurrentTrack()) {
                            trackPosition = playMusicSrv.getCurrentTrack();
                            topTrackList = playMusicSrv.getPlayingList();
                            if (isLargeWidth())
                                updateMPViewForPopUpDialog(context);
                            else
                                updateMPView(context);
                        }
                        playPauseBtn.setImageResource(R.drawable.ic_play);
                        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.ui_progress_seekbar);
                        TextView curDurationTV = (TextView) rootView.findViewById(R.id.ui_current_duartion_textview);
                        long currentDuration = playMusicSrv.getMediaPlayer().getCurrentPosition();
                        // update duration textview
                        curDurationTV.setText(Util.msToFormatTime(currentDuration));
                        // Update SeekBar progress
                        int progress = (int) ((currentDuration * 100) / PREVIEW_DURATION);
                        seekBar.setProgress(progress);
                        seekBar.setMax(100);
                    }
                    break;

                case MP_PLAY:
                    Log.v(TAG, "ACTION PLAY RECEIVED ! With Current Duration = Zero is: "+
                            (playMusicSrv.getMediaPlayer().getCurrentPosition() == 0) + "\n"+
                            "srvBound = " + String.valueOf(srvBound));
                    // Update the SeekBar and the Duration TextViews here only when connected to PlayMusicService
                    if (srvBound) {
                        Log.v(TAG, "srvBound is TRUE ! With Current Duration is: \n"+
                                playMusicSrv.getMediaPlayer().getCurrentPosition());
                        //if (trackPosition != playMusicSrv.getCurrentTrack()) {
                            Log.v(TAG, "Gonna Update the View of Music Player!");
                            trackPosition = playMusicSrv.getCurrentTrack();
                            topTrackList = playMusicSrv.getPlayingList();
                            if (isLargeWidth()) {
                                Log.v(TAG,"Update Music Player for Pop Up !");
                                updateMPViewForPopUpDialog(context);
                            }
                            else {
                                Log.v(TAG, "Update Music Player for normal view !");
                                updateMPView(context);
                            }
                        //}
                        // Update Play/Pause
                        Log.v(TAG, "Updating the seek bar and duration !");
                        playPauseBtn.setImageResource(R.drawable.ic_pause);
                        mRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (playMusicSrv.getMPState() == PlayMusicService.State.Started) {
                                    SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.ui_progress_seekbar);
                                    TextView curDurationTV = (TextView) rootView.findViewById(R.id.ui_current_duartion_textview);
                                    long currentDuration = playMusicSrv.getMediaPlayer().getCurrentPosition();
                                    // update duration textview
                                    curDurationTV.setText(Util.msToFormatTime(currentDuration));
                                    // Update SeekBar progress
                                    int progress = (int) ((currentDuration * 100) / PREVIEW_DURATION);
                                    seekBar.setProgress(progress);
                                    seekBar.setMax(100);
                                    // Schedule to post the progression every 100 ms
                                    mHandler.postDelayed(this, DELAY_PROGRESS);
                                } else {
                                    mHandler.removeCallbacks(mRunnable);
                                }
                            }
                        };
                        mHandler.postDelayed(mRunnable, DELAY_PROGRESS);

                        // Scrubbing the SeekBar's Handler Listener
                        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.ui_progress_seekbar);
                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                                mHandler.removeCallbacks(mRunnable);
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                mHandler.removeCallbacks(mRunnable);
                                int newSeekbarPos = seekBar.getProgress();
                                int newPosition = newSeekbarPos * ((int) PREVIEW_DURATION) / 100;
                                currentDuration.setText(Util.msToFormatTime(newPosition));
                                // update MediaPlay
                                Log.v(TAG, "NEW POSITION: " + String.valueOf(newPosition));
                                playMusicSrv.seekTo(newPosition);
                            }
                        });
                    }
                    break;
            }

            return true;
        }
    }

}
