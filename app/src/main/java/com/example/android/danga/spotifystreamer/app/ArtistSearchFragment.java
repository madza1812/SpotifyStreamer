package com.example.android.danga.spotifystreamer.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchFragment extends Fragment {

    public final String TAG = ArtistSearchFragment.class.getSimpleName();

    private final String KEY_ARTISTS_LIST = "list_artists";
    private final String KEY_QUERY = "query";
    private final String KEY_ARTIST_NAME_ID = "id_name_artist";
    private final String KEY_ACTION = "action_argument";

    private final String INTENT_ACTION_GET_TOP_TEN = "top_ten_get_action_intent";
    private final String ARGUMENT_ACTION_CLEAR_TOP_TEN = "top_ten_clear_action_argument";
    private final String ARGUMENT_ACTION_GET_TOP_TEN = "top_ten_get_action_argument";
    private final String ARGUMENT_ACTION_WELCOME_TOP_TEN = "ton_ten_welcome_action_argument";

    private final String ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG = "artist_top_ten_large_screen";

    private final int MAX_SEARCH_LENGTH = 50;
    private final long DELAY = 200; // in ms

    private ArtistSearch search;
    private Handler handler;
    private Runnable runnableSearch;

    private ArrayList<ArtistParcel> listArtists;
    private String savedQuery;

    // Dual-Pane: keep track of current selected artist
    private static ArtistParcel currentArtist;

    private ArtistArrayAdapter<ArtistParcel> mArtistAdapter;
    private Intent detailIntent;

    // Butter Knife Binding Views
    @Bind(R.id.search_artist) SearchView searchView;
    @Bind(R.id.welcome_viewstub) ViewStub welcomeStub;
    @Bind(R.id.noresult_viewstub) ViewStub noresultStub;
    @Bind(R.id.listview_artist) ListView artistListView;

    // End Butter Knife Binding Views

    public ArtistSearchFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.v(TAG, "Starting the FRAGMENT ARTIST SEARCH !");

        final Context context = getActivity();

        // Inflate Fragment Layout
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize ListView
        List<ArtistParcel> listCustomArtists = new ArrayList<>();
        mArtistAdapter = new ArtistArrayAdapter<>(getActivity(), listCustomArtists);
        artistListView.setAdapter(mArtistAdapter);

        // Inflate ViewStubs and set GONE from start
        welcomeStub.inflate().setVisibility(View.GONE);
        noresultStub.inflate().setVisibility(View.GONE);

        // Restoring with saved states: either left the screen (go to next activity)
        // or screen rotated
        if (savedInstanceState != null) {
            Log.v(TAG, "Restore RegToReg !");
            restoreFromScreenRotation(savedInstanceState);
        } else {
            // Initial, the Welcome ViewStub is VISIBLE when starting new activity
            welcomeStub.setVisibility(View.VISIBLE);
            // Invoke ArtistTopTenFragment for Tablet Screen
            if (isLargeWidth())
                invokeTopTenFragmentLargeScreen(null, ARGUMENT_ACTION_WELCOME_TOP_TEN);

        }

        // Set Up the remaining View for the ArtistSearchFragment
        // Initialize the InputMethodManager
        final InputMethodManager inputManager = (InputMethodManager) getActivity().getApplicationContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        // Disable the Mag Icon in SearchView
        int searchMagId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchMagImage = (ImageView) searchView.findViewById(searchMagId);
        if (searchMagImage != null) {
            searchMagImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        }

        Log.v(TAG, "SET UP THE SEARCH VIEW !");
        // Return search artist when there at least 2 characters typed and not more than 50 characters
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Just close the Soft Keyboard
                try {
                    inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus()
                            .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                } catch (NullPointerException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    return true;
                }
                return true;
                }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Perform a live search when user typing
                final String query = newText;
                // Cancel the scheduled tasks in Handler
                if (handler != null) {
                    handler.removeCallbacks(runnableSearch);
                }
                // Cancel the AsyncTasks that are not started yet.
                if (search != null && search.getStatus().equals(AsyncTask.Status.PENDING))
                    search.cancel(true);
                // Check if the device connected
                if (isNetworkAvailable()) {
                    if (query.trim().length() > MAX_SEARCH_LENGTH) {
                        welcomeStub.setVisibility(View.GONE);
                        Util.displayToast(context, "Cannot search more than 50 characters!");
                        searchView.setQuery(query.substring(0, MAX_SEARCH_LENGTH), true);
                    } else if (query.trim().length() > 1 && query.trim().length() <= MAX_SEARCH_LENGTH) {
                        mArtistAdapter.clear();
                        // Delay the execution in 200 ms, which is average duration of Spotify request/response.
                        // Using the Handler instead of Timer
                        handler = new Handler();
                        runnableSearch = new Runnable() {
                            @Override
                            public void run() {
                                Log.v("TESTING HANDLER", "TASK is RUN for QUERY: " + query.trim());
                                search = new ArtistSearch();
                                search.execute(query.trim());
                            }
                        };
                        Log.v("TESTING HANDLER", "TASK is SCHEDULED! for QUERY: " + query.trim());
                        handler.postDelayed(runnableSearch, DELAY);
                        welcomeStub.setVisibility(View.GONE);
                    } else {
                        // Show the Welcome Stub if there is less than 2 characters.
                        mArtistAdapter.clear();
                        enableWelcomeStub();
                    }
                } else { // NO CONNECTION
                    mArtistAdapter.clear();
                    enableWelcomeStub();
                    Util.displayToast(context, "Connection Lost!");
                }
                return true;
            }
        });

        // When Clear Button is clicked, show the Welcome View rather than the Empty View.
        Log.v(TAG, "SET UP CLEAR BUTTON !");
        int searchClearId = getResources().getIdentifier("android:id/search_close_btn", null, null);
        ImageView clearButton = (ImageView) searchView.findViewById(searchClearId);
        if (clearButton != null) {
            clearButton.setImageResource(R.drawable.ic_clear_black_36dp);
            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchView.setQuery("", false);
                    mArtistAdapter.clear();
                    enableWelcomeStub();
                    if (isLargeWidth()) {
                        invokeTopTenFragmentLargeScreen(null, ARGUMENT_ACTION_CLEAR_TOP_TEN);
                    }
                }
            });
        }

        // When clicking an artist in the list
        Log.v(TAG, "SET UP onItemClickListener !");
        artistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Display the Toast containing Artist Information
                Context context = getActivity().getApplicationContext();
                ArtistParcel selArtist = (ArtistParcel) parent.getItemAtPosition(position);
                if (selArtist != null) {
                    String[] nameAndId = new String[]{selArtist.getName(), selArtist.getId()};
                    // Check connection before launch the intent to TopTenActivity
                    if (isNetworkAvailable()) {
                        if (isLargeWidth()) {
                            // Load the ArtistTopTenFragment
                            invokeTopTenFragmentLargeScreen(selArtist, ARGUMENT_ACTION_GET_TOP_TEN);
                        } else {
                            // Launch the ArtistTopTenActivity
                            detailIntent = new Intent(context, ArtistTopTenActivity.class);
                            detailIntent.setAction(INTENT_ACTION_GET_TOP_TEN)
                                    .putExtra(KEY_ARTIST_NAME_ID, nameAndId);
                            launchDetailActivity(detailIntent);
                        }
                    } else { // NO CONNECTION
                        Util.displayToast(context, "Connection Lost!");
                    }
                }
            }
        });
        return rootView;
    }

    public void enableWelcomeStub() {
        if (noresultStub.getParent() == null)
            noresultStub.setVisibility(View.GONE);
        welcomeStub.setVisibility(View.VISIBLE);
    }

    public void invokeTopTenFragmentLargeScreen(ArtistParcel selArtist, String action) {
        ArtistTopTenFragment mFragment =
                (ArtistTopTenFragment) getFragmentManager().findFragmentById(R.id.artist_detail_fragment);
        Bundle bundle = new Bundle();
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        switch (action) {

            case ARGUMENT_ACTION_WELCOME_TOP_TEN:
                Log.v(TAG, "Welcome Top Ten Track in Large Screen !");
                mFragment = new ArtistTopTenFragment();
                bundle.putString(KEY_ACTION, ARGUMENT_ACTION_WELCOME_TOP_TEN);
                mFragment.setArguments(bundle);
                ft.replace(R.id.detail_topten_container, mFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .commit();
                break;

            case ARGUMENT_ACTION_GET_TOP_TEN:
                if (selArtist != null) {
                    Log.v(TAG, "Get Top Ten Tracks in Large Screen !");
                    if (mFragment == null ||
                            currentArtist == null ||
                            !mFragment.getArtistID().equals(currentArtist.getId())) {

                        mFragment = new ArtistTopTenFragment();
                        bundle.putString(KEY_ACTION, ARGUMENT_ACTION_GET_TOP_TEN);
                        bundle.putStringArray(KEY_ARTIST_NAME_ID, new String[]{selArtist.getName(), selArtist.getId()});
                        mFragment.setArguments(bundle);
                        ft.replace(R.id.detail_topten_container, mFragment,ARTIST_TOP_TEN_FRAGMENT_LARGE_SCREEN_TAG)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                                .commit();
                        currentArtist = selArtist;
                    }
                }
                break;

            case ARGUMENT_ACTION_CLEAR_TOP_TEN:
                Log.v(TAG, "Clear Top Ten Track due to Clear Button in Large Screen !");
                mFragment = new ArtistTopTenFragment();
                bundle.putString(KEY_ACTION, ARGUMENT_ACTION_CLEAR_TOP_TEN);
                mFragment.setArguments(bundle);
                ft.replace(R.id.detail_topten_container, mFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .commit();
                break;

            default:
                break;
        }
    }

    public void restoreFromScreenRotation(Bundle savedInstanceState) {
        listArtists = savedInstanceState.getParcelableArrayList(KEY_ARTISTS_LIST);
        savedQuery = savedInstanceState.getString(KEY_QUERY);
        recoverSearchArtist(listArtists, savedQuery);
    }

    public void recoverSearchArtist(ArrayList<ArtistParcel> artistParcels, String query) {
        if (artistParcels != null) {
            searchView.setQuery(query, false);
            boolean noResultFlag = (query.trim().length() > 1 && artistParcels.size() == 0);
            if (noResultFlag) {
                noresultStub.setVisibility(View.VISIBLE);
            } else if (query.trim().length() > 1 && artistParcels.size() > 0) {
                mArtistAdapter.clear();
                mArtistAdapter.addAll(artistParcels);
            } else {
                welcomeStub.setVisibility(View.VISIBLE);
            }
        }
    }

    public void launchDetailActivity (Intent intent) {
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            if (Build.VERSION.SDK_INT >=16) {
                startActivity(intent,
                        ActivityOptions.makeCustomAnimation(getActivity(),
                                R.animator.enter,
                                R.animator.exit).toBundle());
            } else {
                startActivity(intent);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "START onSaveInstanceState of ArtistSearchFRAGMENT !");
        outState.putParcelableArrayList(KEY_ARTISTS_LIST, listArtists);
        outState.putString(KEY_QUERY, savedQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
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

    // Background loading Artits using AsyncTask
    class ArtistSearch extends AsyncTask<String, Void, List<ArtistParcel>> {

        public final String LOG_TAG = ArtistSearch.class.getSimpleName();

        @Override
        protected List<ArtistParcel> doInBackground(String... queries) {
            Log.v(LOG_TAG, "Start doInBackgroud");
            List<ArtistParcel> listArtists;
            listArtists = doSearch(queries[0]);
            return listArtists;
        }

        @Override
        protected void onPostExecute(List<ArtistParcel> artists) {
            if (artists != null){
                if (artists.size() > 1) {
                    if (noresultStub.getParent() == null)
                        noresultStub.setVisibility(View.GONE);
                    CharSequence currentQuery  = searchView.getQuery();
                    /*Synchronizing the live search
                    by check if the currentQuery equal to the requested query*/
                    if (artists.get(artists.size()-1).getName().equalsIgnoreCase(currentQuery.toString().trim())) {
                        listArtists = new ArrayList<>();
                        for (int i = 0; i<artists.size()-1; i++){
                            mArtistAdapter.add(artists.get(i));
                            listArtists.add(artists.get(i));
                        }
                        savedQuery = currentQuery.toString();
                        Log.v(LOG_TAG, "SAVEDQUERY = "+savedQuery);
                    }
                } else {
                    // Empty Array list to ArtistParcel Array.
                    listArtists = new ArrayList<>();
                    noresultStub.setVisibility(View.VISIBLE);
                }
            }
        }

        private List<ArtistParcel> doSearch (String query) {
            Log.v(LOG_TAG, "Started SearchArtist !");

            List<Artist> artists;
            List<ArtistParcel> mArtistParcels;
            ArtistsPager results;
            HashMap<String, Object> options = new HashMap<>();
            // Build a fake Artist containing the query in Artist Name;
            ArtistParcel fakeArtist = new ArtistParcel();
            fakeArtist.setName(query);

            StringBuilder queryBuilder = new StringBuilder(query);
            queryBuilder.append("*");
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotifyService = api.getService();

            // Get the country market from sharepreference
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String countryCode = prefs.getString(
                    getString(R.string.pref_country_key),
                    getString(R.string.pref_country_default)
            );

            final String CURRENT_LOCATION = "LO";
            Log.v(TAG, "Country " + countryCode + " is chosen by user!");
            if (CURRENT_LOCATION.equals(countryCode)) {
                options.put(spotifyService.COUNTRY, Locale.getDefault().getCountry());
            } else {
                options.put(spotifyService.COUNTRY, countryCode);
            }



            try {
                results = spotifyService.searchArtists(queryBuilder.toString().toUpperCase(), options);
                artists = results.artists.items;
                // Sort with priority to Image, then Followers then Popularity.
                Collections.sort(artists, new ArtistComparable());
                mArtistParcels = toArtistParcels(artists);
            } catch (RetrofitError e){
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(e);
                Log.e(LOG_TAG, spotifyError.getMessage());
                spotifyError.printStackTrace();
                mArtistParcels = new ArrayList<>();
                mArtistParcels.add(fakeArtist);
                return mArtistParcels;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
                mArtistParcels = new ArrayList<>();
                return mArtistParcels;
            }
            mArtistParcels.add(fakeArtist);
            return mArtistParcels;
        }
        private List<ArtistParcel> toArtistParcels(List<Artist> artists) {
            List<ArtistParcel> mArtistParcels = new ArrayList<>();
            ArtistParcel mArtistParcel;
            for(Artist a : artists) {
                if (a.images.size() > 0)
                    mArtistParcel = new ArtistParcel(a.name, a.id, a.images.get(0).url);
                else
                    mArtistParcel = new ArtistParcel(a.name, a.id, "");
                mArtistParcels.add(mArtistParcel);
            }
            return mArtistParcels;
        }
    }
}
