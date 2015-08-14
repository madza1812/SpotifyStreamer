package com.example.android.danga.spotifystreamer.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * Created by An on 6/30/2015.
 */
public class PlayMusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaController.MediaPlayerControl, AudioManager.OnAudioFocusChangeListener {

    public final String TAG = PlayMusicService.class.getSimpleName();

    private final String ACTION_START_MP = "com.example.android.danga.spotifystreamer.app.START";
    private final String ACTION_PLAY_PAUSE_MP = "com.example.android.danga.spotifystreamer.app.PLAY_PAUSE";
    private final String ACTION_PREVIOUS_MP = "com.example.android.danga.spotifystreamer.app.PREVIOUS";
    private final String ACTION_NEXT_MP = "com.example.android.danga.spotifystreamer.app.NEXT";
    private final String ACTION_LAUNCH_UI = "com.example.android.danga.spotifystreamer.app.LAUNCH_UI";
    private final String INTENT_ACTION_LAUNCH_UI = "ui_launch_action_intent";
    private final String ACTION_LAUNCH_POP_UP_UI = "ui_pop_up_launch_action";

    private final static String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final static String KEY_TRACK_POSITION = "track_position";
    private final static String KEY_MESSENGER = "messenger";
    private final String KEY_ASA_MESSENGER = "messenger_asa";

    private final int MSG_PLAY = 0;
    private final int MSG_PAUSE = 1;
    private final int MSG_NEXT = 2;
    private final int MSG_PREVIOUS = 3;

    private final int MSG_LAUNCH_POP_UP_UI = 4;

    private final String NOTIF_ICON_PLAY = "Play";
    private final String NOTIF_ICON_PAUSE = "Pause";
    private final String NOTIF_ICON_PREVIOUS = "Previous";
    private final String NOTIF_ICON_NEXT = "Next";
    private final String NOTIF_START = "start";
    private final String NOTIF_UPDATE = "update";

    private static MediaPlayer mMediaPlayer = null; // current MP
    private static MediaController mMediaController = null;
    private AudioManager mAudioManager;
    private static int curTrackPos; // current track
    private static ArrayList<TrackParcel> topTrackList = null; // current playlist

    private Messenger uiMessenger = null;
    private Messenger asaMessenger = null;

    private StartSendNotification startSendNotification;
    private String[] notificationParams = new String[4]; // in format [albumUrl, Pause/Play, Ongoing,start or update]

    private static int currentScrubPosition = 0; // in millisecond
    private final int NOTIF_ID_FOREGROUND = 200;
    private final int NULL_VALUE = 100;

    private static boolean foregroundMode = false;

    // Media Player States
    enum State {
        Idle,
        Initialized,
        Preparing,
        Started,
        Paused,
        Stopped,
        Completed
    };
    private static State mState = State.Idle;

    // Service binder
    private final IBinder mIbinder = new ServicePlayBinder();

    public class ServicePlayBinder extends Binder {
        PlayMusicService getService() {
            return PlayMusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIbinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "PlayMusicService is created !");
        Log.v(TAG, "onCreate Service ! Initialize MediaPlayer");
        curTrackPos = NULL_VALUE;
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC
                ,AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Log.v(TAG, "AUDIO FOCUS REQUEST GRANTED !");
        initMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Start binding PlayMusicService !");
        if (intent != null && ACTION_START_MP.equals(intent.getAction())) {
            ArrayList<TrackParcel> newPlaylist = intent.getParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST);
            int newPos = intent.getIntExtra(KEY_TRACK_POSITION, NULL_VALUE);
            uiMessenger = (Messenger) intent.getExtras().get(KEY_MESSENGER);
            Log.v(TAG, "onStartCommand: ACTION_START_MP, uiMessenger = " + uiMessenger.toString());
            if (newPos == NULL_VALUE) {
                Log.v(TAG, "Passing POSITION is NULL !");
                stopSelf();
            }
            // Check if it's the first time
            // When the service is started the first time.
            if (curTrackPos == NULL_VALUE || topTrackList == null) {
                Log.v(TAG, "FIRST TIME !");
                curTrackPos = newPos;
                topTrackList = newPlaylist;
                startPlayingMusic();
            }
            // Check if different track
            else if (!newPlaylist.get(newPos).getPreviewUrl().equals(topTrackList.get(curTrackPos).getPreviewUrl())) {
                Log.v(TAG, "DIFFERENT TRACK !");
                curTrackPos = newPos;
                topTrackList = newPlaylist;
                startPlayingMusic();
            }
            // The same track is paused.
            else if (mState == State.Paused) {
                Log.v(TAG, "SAME TRACK and RESUME !");
                resumeMusic();
            }
        }
        else if (intent != null && ACTION_LAUNCH_UI.equals(intent.getAction())) {
            Log.v(TAG, "ACTION LAUNCH UI: new Handler !");
            uiMessenger = (Messenger) intent.getExtras().get(KEY_MESSENGER);
            Log.v(TAG, "onStartCommand: ACTION_LAUNCH_UI, uiMessenger = " + uiMessenger.toString());
        }
        if (intent != null && ACTION_PLAY_PAUSE_MP.equals(intent.getAction())){
            Log.v(TAG, "Intent Notification PLAY/PAUSE !");
            if (mState == State.Paused)
                resumeMusic();
            else if (mState == State.Started)
                pauseMusic();
        }
        if (intent != null && ACTION_PREVIOUS_MP.equals(intent.getAction()))
            //Log.v(TAG, "Intent Notification PREVIOUS !");
            previousMusic();
        if (intent != null && ACTION_NEXT_MP.equals(intent.getAction()))
            //Log.v(TAG, "Intent Notification NEXT !");
            nextMusic();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Stop and release MediaPlayer
        try {
            stopForeground(true);
            mMediaPlayer.release();
        } finally {
            mMediaPlayer = null;
        }
        super.onDestroy();
    }

    public void initMediaPlayer() {
        // Initialize the Player
        Log.v(TAG, " New PlayMusicService is created !");
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setVolume(50, 50);
            // Initialize the MediaController
            mMediaController = new MediaController(this, false);
            mMediaController.setPrevNextListeners(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextMusic();
                }
            }, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    previousMusic();
                }
            });
            mMediaController.hide();
        }
    }

    public void startPlayingMusic() {
        Log.v(TAG, "Start playing Music !");
        if (isNetworkAvailable()) {
            String url = topTrackList.get(curTrackPos).getPreviewUrl();
            mMediaPlayer.reset();
            initializeMP(url);
            prepareMP();
        } else {
            Util.displayToast(getApplicationContext(), "Lost Connection !");
        }
    }

    public void initializeMP(String url) {
        try {
            mMediaPlayer.setDataSource(url);
            mState = State.Initialized;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException for setDataSrouce: " + e.getMessage());
            e.printStackTrace();
            mMediaPlayer.reset();
            mState = State.Idle;
            Util.displayToast(getApplicationContext(), "MediaPlayer encounter errors!");
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException for setDataSource: " + e.getMessage());
            e.printStackTrace();
            mMediaPlayer.reset();
            mState = State.Idle;
            Util.displayToast(getApplicationContext(), "MediaPlayer encounter errors!");
        } catch (IOException e) {
            Log.e(TAG, "IOEception for setDataSource: " + e.getMessage());
            e.printStackTrace();
            mMediaPlayer.reset();
            mState = State.Idle;
            Util.displayToast(getApplicationContext(), "MediaPlayer encounter errors!");
        }
    }

    public void prepareMP() {
        try {
            mMediaPlayer.prepareAsync();
            mState = State.Preparing;
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException for setDataSource: " + e.getMessage());
            e.printStackTrace();
            mMediaPlayer.reset();
            mState = State.Idle;
            Util.displayToast(getApplicationContext(), "MediaPlayer encounter errors!");
        }
    }

    public void pauseMusic() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            currentScrubPosition = mMediaPlayer.getCurrentPosition();
            mState = State.Paused;
            updateToUIFromMP(MSG_PAUSE);
            // Update the foregroundMode and update setOngoing(false);
            notificationParams = new String[] {topTrackList.get(curTrackPos).getAlbumThumbnailUrl(),NOTIF_ICON_PLAY, NOTIF_UPDATE};
            startSendNotification = new StartSendNotification(this);
            startSendNotification.execute(notificationParams);
            // Stop the foreground
            stopForeground(true);
            foregroundMode = false;
        }
    }

    public void resumeMusic() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(currentScrubPosition);
            mMediaPlayer.start();
            mState = State.Started;
            updateToUIFromMP(MSG_PLAY);

            // Update foregroundMode
            notificationParams = new String[] {topTrackList.get(curTrackPos).getAlbumThumbnailUrl(),NOTIF_ICON_PAUSE, NOTIF_UPDATE};
            startSendNotification = new StartSendNotification(getApplicationContext());
            startSendNotification.execute(notificationParams);
        }
    }

    public void nextMusic() {
        curTrackPos++;
        if (curTrackPos == topTrackList.size())
            curTrackPos = 0;
        startPlayingMusic();
        updateToUIFromMP(MSG_NEXT);
    }

    public void previousMusic() {
        curTrackPos--;
        if (curTrackPos < 0)
            curTrackPos = topTrackList.size() - 1;
        startPlayingMusic();
        updateToUIFromMP(MSG_PREVIOUS);
    }

    public void seekTo(int progress) {
        Log.v(TAG, "SEEKTO is called with progress: " + String.valueOf(progress) +
                "\nState is: " + mState);
        switch (mState) {
            case Started:
                mMediaPlayer.seekTo(progress);
                mMediaPlayer.start();
                updateToUIFromMP(MSG_PLAY);
                break;
            case Paused:
                currentScrubPosition= progress;
                updateToUIFromMP(MSG_PAUSE);

        }
    }

    public void stopMusic() {
        mMediaPlayer.stop();
        mState = State.Stopped;
        if (foregroundMode) {
            stopForeground(true);
            foregroundMode = false;
        }
    }

    // MediaController override methods:
    @Override
    public void start() {
        startPlayingMusic();
    }

    @Override
    public void pause() {

    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }
    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    // End MediaController override methods

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public MediaController getMediaController() {
        return mMediaController;
    }

    public State getMPState() {
        return mState;
    }

    public ArrayList<TrackParcel> getPlayingList() {
        return topTrackList;
    }

    public int getCurrentTrack() {
        return curTrackPos;
    }

    public boolean isForegroundMode() {
        return foregroundMode;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Play the next track on the playlist
        nextMusic();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer encounters error: " + "what: " + String.valueOf(what) +
                " extra: " + String.valueOf(extra));
        mp.reset();
        mState = State.Idle;
        Util.displayToast(getApplicationContext(), "MediaPlayer encounter errors!");
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.v(TAG, "onAUDIOFOCUSCHANGE !");
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.v(TAG, "audio focus gain !");
                // resume playback
                if (mState == State.Paused)
                    resumeMusic();
                else if (mState == State.Stopped)
                    prepareMP();
                mMediaPlayer.setVolume(50, 50);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                Log.v(TAG, "audio focus loss !");
                // Lost focus for an unbounded amount of time
                if (mState == State.Started || mMediaPlayer.isPlaying())
                    stopMusic();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.v(TAG, "audio focus loss transient !");
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mState == State.Started)
                    pauseMusic();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.v(TAG, "audio focus loss transient can duck !");
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mState == State.Started)
                    mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaController.setMediaPlayer(this);
        // Start playing the track
        mp.start();
        mState = State.Started;
        Log.v(TAG, "onPrepared: Music is started !");
        // Send message to Activity
        updateToUIFromMP(MSG_PLAY);
        // Update foregroundMode
        if (!foregroundMode) {
            /** Get notification from builder
            * Set State.Paused for Playing
            * Set State.Play for Pausing
            * Start the Foreground Notification **/
            Log.v(TAG, "STARTING FOREGROUND SERVICE NOTIFICATION !");
            notificationParams = new String[] {topTrackList.get(curTrackPos).getAlbumThumbnailUrl(),NOTIF_ICON_PAUSE, NOTIF_START};
            startSendNotification = new StartSendNotification(getApplicationContext());
            startSendNotification.execute(notificationParams);
            foregroundMode = true;
        } else {
            notificationParams = new String[] {topTrackList.get(curTrackPos).getAlbumThumbnailUrl(),NOTIF_ICON_PAUSE, NOTIF_UPDATE};
            startSendNotification = new StartSendNotification(getApplicationContext());
            startSendNotification.execute(notificationParams);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    private boolean isLargeWidth(){
        return getResources().getBoolean(R.bool.large_screen);
    }

    public void updateToUIFromMP(int action) {
        Log.v(TAG, "update UI through uiMessenger = " + uiMessenger.toString());
        Message msg = new Message();
        msg.what = action;
        try {
            uiMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger Error !");
            e.printStackTrace();
        }
        Log.v(TAG, "Message is sent !");
    }

    public void updateToASActivity (int action) {
        Message msg = new Message();
        msg.what = action;
        try {
            asaMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger Error !");
            e.printStackTrace();
        }
        Log.v(TAG, "Message is sent !");
    }

    class StartSendNotification extends AsyncTask<String, Void, Bitmap> {

        private final String LOG_TAG = StartSendNotification.class.getSimpleName();

        private String mpAction;
        private String notifAction;
        Context context;

        public StartSendNotification (Context context) {
            this.context = context;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Log.v(LOG_TAG, "DOING IN BACK GROUND FOR START/UPDATE NOTIFICATION !");
            try {
                notifAction = params[2];
                mpAction = params[1];
                return Picasso.with(context).load(params[0])
                        .resize(100, 100)
                        .error(R.drawable.spotify_streamer_orange).get();
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.v(LOG_TAG, "onPostExecution BUILD NOTIFICATION THEN START/UPDATE THE FOREGROUND NOTIFICATION !");

            final int REQUEST_CODE_NOTIF = 100;
            final int REQUEST_CODE_PREV = 101;
            final int REQUEST_CODE_NEXT = 102;
            final int REQUEST_CODE_PLAY_PAUSE = 103;

            String trackName;
            String artistName;
            String albumName;

            Bitmap notifBitmap;
            int playPauseIcon;
            int smallIcon;
            String playPauseTitle;
            boolean onGoing;

            Notification notifPlayMusic;
            NotificationManager mNotificationManager;


            // Notification Manager
            mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (bitmap != null) {
                //Log.v(TAG, "BITMAP LOAD SUCCEED !");
                notifBitmap = bitmap;
            } else {
                //Log.v(TAG, "BITMAP LOAD DOES NOT SUCCEED !");
                notifBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.spotify_streamer_orange);
            }
            // Build Notification
            trackName = topTrackList.get(curTrackPos).getName();
            artistName = topTrackList.get(curTrackPos).getArtistNames();
            albumName = topTrackList.get(curTrackPos).getAlbumName();

            Log.v(LOG_TAG, trackName +" ; "+ artistName +" ; " + albumName);

            Log.v(LOG_TAG, context.getPackageName());

            // Setup Intents
            Intent notifIntent;
            Log.v(TAG, "Build Notification Intent For REGULAR screen !");
            notifIntent = new Intent(context, MusicPlayerActivity.class);
            notifIntent.setAction(INTENT_ACTION_LAUNCH_UI);
            notifIntent.putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST, topTrackList)
                    .putExtra(KEY_TRACK_POSITION, curTrackPos);

            //Intent notifIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent pNotifIntent = PendingIntent.getActivity(context, REQUEST_CODE_NOTIF,
                    notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent prevIntent = new Intent(getApplicationContext(), PlayMusicService.class);
            prevIntent.setAction(ACTION_PREVIOUS_MP);
            PendingIntent pPrevIntent = PendingIntent.getService(context, REQUEST_CODE_PREV,
                    prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent nextIntent = new Intent(getApplicationContext(), PlayMusicService.class);
            nextIntent.setAction(ACTION_NEXT_MP);
            PendingIntent pNextIntent = PendingIntent.getService(context, REQUEST_CODE_NEXT,
                    nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent playPauseIntent = new Intent(getApplicationContext(), PlayMusicService.class);
            playPauseIntent.setAction(ACTION_PLAY_PAUSE_MP);
            PendingIntent pPlayPauseIntent = PendingIntent.getService(context, REQUEST_CODE_PLAY_PAUSE,
                    playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            // Check and set the Play/Pause Icon in Notification
            if (mpAction.equals(NOTIF_ICON_PAUSE)) {
                //Log.v(TAG, "onGoing is true !");
                playPauseIcon = R.drawable.ic_pause;
                playPauseTitle = NOTIF_ICON_PAUSE;
                onGoing = true;
                smallIcon = R.drawable.ic_play_circle_outline_white_48dp;
            } else {
                //Log.v(TAG, "onGoing is false !");
                playPauseIcon = R.drawable.ic_play;
                playPauseTitle = NOTIF_ICON_PLAY;
                onGoing = false;
                smallIcon = R.drawable.ic_pause_circle_outline_white_48dp;
            }

            // Setup Notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setContentTitle(trackName)
                    .setSmallIcon(smallIcon)
                    .setSubText(albumName)
                    .setContentText(artistName)
                    .setLargeIcon(Bitmap.createScaledBitmap(notifBitmap, 100, 100, false))
                    .setContentIntent(pNotifIntent)
                    .setOngoing(onGoing)
                    .addAction(R.drawable.ic_previous, NOTIF_ICON_PREVIOUS, pPrevIntent)
                    .addAction(playPauseIcon, playPauseTitle, pPlayPauseIntent)
                    .addAction(R.drawable.ic_next, NOTIF_ICON_NEXT, pNextIntent);
            notifPlayMusic = builder.build();

            // Start or Update Notification
            if (notifAction.equals(NOTIF_START)) {
                startForeground(NOTIF_ID_FOREGROUND, notifPlayMusic);
                Log.v(LOG_TAG, "FOREGROUND SERVICE IS STARTED !");
            } else {
                mNotificationManager.notify(NOTIF_ID_FOREGROUND, notifPlayMusic);
                Log.v(LOG_TAG, "FOREGROUND SERVICE IS UPDATED !");
            }
        }
    }

}


