package com.example.android.danga.spotifystreamer.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopTenFragment extends Fragment {

    public final String TAG = ArtistTopTenFragment.class.getSimpleName();

    private final String KEY_TOP_TEN_TRACKS_LIST = "list_tracks_top_ten";
    private final String KEY_TRACK_POSITION = "track_position";
    private final String KEY_ACTION = "action_argument";

    private final String ARGUMENT_ACTION_START_NEW_PLAYLIST = "new_playlist_start_action_argument";
    private final String INTENT_ACTION_START_NEW_PLAYLIST = "new_playlist_start_action_intent";

    private final String MUSIC_PLAYER_FRAGMENT_POPUP_TAG = "player_ui_popup";

    private ApplicationManager appManager;
    private Display display;

    private ArrayList<TrackParcel> topTenTracks;
    private String selectedArtistID;
    private int selectedTrackPos;

    private TrackArrayAdapter<TrackParcel> mDetailAdapter;

    private ViewStub noTrackStub,welcomeDetailStub;

    private Intent topTrackIntent;

    private ActivityCommunicator mActComm;

    private final int NULL_VALUE = 100;

    public ArtistTopTenFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Set activity position to check UI is shown before screen rotation.
        this.appManager = (ApplicationManager) getActivity().getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Instantiate local variables
        final String ARGUMENT_ACTION_CLEAR_TOP_TEN = "top_ten_clear_action_argument";
        final String ARGUMENT_ACTION_GET_TOP_TEN = "top_ten_get_action_argument";
        final String ARGUMENT_ACTION_WELCOME_TOP_TEN = "ton_ten_welcome_action_argument";

        final String INTENT_ACTION_GET_TOP_TEN = "top_ten_get_action_intent";

        final String KEY_ARTIST_NAME_ID = "id_name_artist";

        String[] artistNameId;
        selectedTrackPos = NULL_VALUE;

        // Inflate the rootView
        View rootView =  inflater.inflate(R.layout.fragment_artist_top_ten, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize ViewStub
        noTrackStub = (ViewStub) rootView.findViewById(R.id.notrack_viewstub);
        noTrackStub.inflate().setVisibility(View.GONE);

        // Set welcome screen in Large Screen Mode
        if (isLargeWidth()) {
            welcomeDetailStub = (ViewStub) rootView.findViewById(R.id.detail_welcome_viewstub);
            welcomeDetailStub.inflate().setVisibility(View.GONE);
        }

        // Setup the ListView
        List<TrackParcel> listTopTrack = new ArrayList<>();
        mDetailAdapter = new TrackArrayAdapter<>(getActivity(), listTopTrack);
        final ListView listTrackView = (ListView) rootView.findViewById(R.id.listview_top_track);
        listTrackView.setAdapter(mDetailAdapter);

        // Restoring with saved states from: either the screen is left (go to next activity)
        // or screen rotated
        if (savedInstanceState != null) {
            Log.v(TAG, "Restore from screen rotation for RegToReg or LarToLar !");
            topTenTracks = savedInstanceState.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST);
            if (topTenTracks != null){
                Log.v(TAG, "topTenTracks is NOT NULL !");
                if (topTenTracks.size() > 0) {
                    mDetailAdapter.clear();
                    mDetailAdapter.addAll(topTenTracks);
                } else
                    noTrackStub.setVisibility(View.VISIBLE);
            } else {
                welcomeDetailStub.setVisibility(View.VISIBLE);
            }
        }

        // Large Screen Mode
        if (isLargeWidth()) {
            Log.v(TAG, "Get Data from ARGUMENTS based on its action !");
            Bundle bundle = getArguments();
            if (bundle != null) {
                Log.v(TAG, "ARGUMENT FOUND FOR TABLET MODE!");
                String action = bundle.getString(KEY_ACTION);
                if (action != null) {
                    switch (action) {

                        case ARGUMENT_ACTION_WELCOME_TOP_TEN:
                            Log.v(TAG, "LARGE SCREEN ACTION WELCOME TOP TEN !");
                            welcomeDetailStub.setVisibility(View.VISIBLE);
                            break;

                        case ARGUMENT_ACTION_GET_TOP_TEN:
                            Log.v(TAG, "LARGE SCREEN ACTION GET TOP TEN !");
                            artistNameId = bundle.getStringArray(KEY_ARTIST_NAME_ID);
                            selectedArtistID = artistNameId[1];
                            break;

                        case ARGUMENT_ACTION_CLEAR_TOP_TEN:
                            Log.v(TAG, "LARGE SCREEN ACTION CLEAR TOP TEN !");
                            welcomeDetailStub.setVisibility(View.VISIBLE);
                            break;

                        default:
                            break;
                    }
                }
            } else {
                Log.v(TAG,"DUAL PANE - BUT NO ARGUMENT");
                welcomeDetailStub.setVisibility(View.VISIBLE);
            }
        }

        // REGULAR SCREEN MODE
        // Get data from Intent based on its action

        Intent intent = getActivity().getIntent();
        if (intent != null && savedInstanceState == null) {
            Log.v(TAG, "Get Data from ARGUMENTS based on its action !");
            if (INTENT_ACTION_GET_TOP_TEN.equals(intent.getAction())) {
                artistNameId = intent.getStringArrayExtra(KEY_ARTIST_NAME_ID);
                selectedArtistID = artistNameId[1];
            }
        }

        // Call the query in AsyncTask background when there is no saved from previous state.
        if (savedInstanceState == null ||
                savedInstanceState.getParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST) == null) {

            Log.v(TAG, "Execute the AsyncTask to get Top Ten Tracks using artist id !");
            if (selectedArtistID != null && !selectedArtistID.trim().isEmpty()) {
                ArtistTopTrackSearch artistTopTrack = new ArtistTopTrackSearch();
                artistTopTrack.execute(selectedArtistID);
            } else {
                if (isLargeWidth())
                    welcomeDetailStub.setVisibility(View.VISIBLE);
            }
        }

        // Set Track Click Listener !
        listTrackView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Display the Toast containing Track Information
                Context context = getActivity().getApplicationContext();
                TrackParcel selectedTrack = (TrackParcel) parent.getItemAtPosition(position);
                if (selectedTrack != null) {
                    // Check connection before launch the intent to Player
                    if (isNetworkAvailable()) {
                        if (isLargeWidth()) {
                            // Show DialogFragment for Player_UI
                            showDialog(position, ARGUMENT_ACTION_START_NEW_PLAYLIST);
                            /*// Launch The Player Activity for Selected Track
                            topTrackIntent = new Intent(context, MusicPlayerActivity.class)
                                    .setAction(INTENT_ACTION_START_NEW_PLAYLIST)
                                    .putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST, topTenTracks)
                                    .putExtra(KEY_TRACK_POSITION, position);
                            if (topTrackIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    startActivity(topTrackIntent,
                                            ActivityOptions.makeCustomAnimation(getActivity(),
                                                    R.animator.enter,
                                                    R.animator.exit).toBundle());
                                } else {
                                    startActivity(topTrackIntent);
                                }
                            }*/
                            selectedTrackPos = position;
                        }
                        else {
                            // Launch The Player Activity for Selected Track
                            topTrackIntent = new Intent(context, MusicPlayerActivity.class)
                                    .setAction(INTENT_ACTION_START_NEW_PLAYLIST)
                                    .putParcelableArrayListExtra(KEY_TOP_TEN_TRACKS_LIST, topTenTracks)
                                    .putExtra(KEY_TRACK_POSITION, position);
                            if (topTrackIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    startActivity(topTrackIntent,
                                            ActivityOptions.makeCustomAnimation(getActivity(),
                                                    R.animator.enter,
                                                    R.animator.exit).toBundle());
                                } else {
                                    startActivity(topTrackIntent);
                                }
                            }
                        }
                    } else { // LOST CONNECTION
                        Util.displayToast(context, "Connection Lost!");
                    }
                }
            }
        });
        return rootView;
    }

    public void showDialog(int selPos, String action) {
        FragmentManager mFragmentManager = getFragmentManager();
        DialogFragment playerUiFrag = (MusicPlayerFragment)
                mFragmentManager.findFragmentByTag(MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
        if (playerUiFrag == null) {
            playerUiFrag = MusicPlayerFragment.newInstance();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, topTenTracks);
            bundle.putString(KEY_ACTION, action);
            if (selPos != NULL_VALUE)
                bundle.putInt(KEY_TRACK_POSITION, selPos);
            playerUiFrag.setArguments(bundle);
            playerUiFrag.show(mFragmentManager, MUSIC_PLAYER_FRAGMENT_POPUP_TAG);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ActivityCommunicator)
            mActComm = (ActivityCommunicator) activity;
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

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "START onSaveInstanceState of ArtistTopTenFRAGMENT !");
        outState.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, topTenTracks);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mActComm= null;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    public String getArtistID () {
        return selectedArtistID;
    }

    private boolean isLargeWidth(){
        return getActivity().getResources().getBoolean(R.bool.large_screen);
    }

    // Background loading Artist's Top Tracks using AsyncTask
    class ArtistTopTrackSearch extends AsyncTask<String, Void, List<TrackParcel>> {

        public final String LOG_TAG = ArtistTopTrackSearch.class.getSimpleName();

        @Override
        protected List<TrackParcel> doInBackground(String... queries) {
            List<TrackParcel> listTopTrack;
            listTopTrack = searchArtistTopTrack(queries[0]);
            return listTopTrack;
        }

        @Override
        protected void onPostExecute(List<TrackParcel> tracks) {
            if (tracks != null) {
                if (tracks.size() > 0) {
                    topTenTracks = new ArrayList<>();
                    if (noTrackStub.getParent() == null)
                        noTrackStub.setVisibility(View.GONE);
                    if (isLargeWidth() && welcomeDetailStub.getParent() == null) {
                            welcomeDetailStub.setVisibility(View.GONE);
                    }
                    mDetailAdapter.clear();
                    mDetailAdapter.addAll(tracks);
                    topTenTracks.addAll(tracks);
                    // In case of Large Screen, update the topTenTracks in ArtistSearchActivity
                    // For restore on rotation purpose.
                    if (isLargeWidth()) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(KEY_TOP_TEN_TRACKS_LIST, topTenTracks);
                        if (mActComm != null)
                            mActComm.passToAcivity(bundle);
                        else
                            Log.v(LOG_TAG, "Something is WRONG !");
                    }
                } else {
                    mDetailAdapter.clear();
                    noTrackStub.setVisibility(View.VISIBLE);
                }
            }
        }

        private List<TrackParcel> searchArtistTopTrack(String artistId) {

            List<Track> tracks;
            List<TrackParcel> mTrackParcels;
            HashMap<String, Object> options = new HashMap<>();

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotifyService = api.getService();
            options.put(spotifyService.COUNTRY, Locale.getDefault().getCountry());
            try {
                Tracks results = spotifyService.getArtistTopTrack(artistId, options);
                if (results != null) {
                    tracks = results.tracks;
                    mTrackParcels = toTrackParcel(tracks);
                } else
                    mTrackParcels = null;
            } catch (RetrofitError e) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(e);
                Log.e(LOG_TAG, spotifyError.getMessage());
                spotifyError.printStackTrace();
                mTrackParcels = Collections.emptyList();
            }
            return mTrackParcels;
        }

        public List<TrackParcel> toTrackParcel(List<Track> tracks) {

            List<TrackParcel> mTrackParcels = new ArrayList<>();
            TrackParcel mTrackParcel;
            String artistNames;
            String albumThumbnailUrl;
            String extUrl;
            final String KEY_EXT_URL = "spotify";

            for (Track track : tracks) {
                StringBuilder artistNamesBuilder = new StringBuilder("");
                albumThumbnailUrl = (track.album.images.size() > 0) ?
                        track.album.images.get(0).url : "";
                for (ArtistSimple as : track.artists) {
                    artistNamesBuilder.append(as.name).append(", ");
                }
                try {
                    artistNames = artistNamesBuilder.substring(0, artistNamesBuilder.length() - 2);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "Artist Names is returned empty for a track from SPOTIFY !");
                    artistNames = "";
                }
                Map<String, String> trackExtUrl = track.external_urls;
                extUrl = trackExtUrl.get(KEY_EXT_URL);

                mTrackParcel = new TrackParcel(track.name, track.id, track.album.name,
                        albumThumbnailUrl, track.preview_url, artistNames, extUrl);
                mTrackParcels.add(mTrackParcel);
            }
            return mTrackParcels;
        }
    }
}
