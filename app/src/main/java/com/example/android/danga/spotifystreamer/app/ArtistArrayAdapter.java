package com.example.android.danga.spotifystreamer.app;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by An on 6/18/2015.
 */
public class ArtistArrayAdapter<T extends ArtistParcel> extends ArrayAdapter<T> {

    public final String LOG_TAG = ArtistArrayAdapter.class.getSimpleName();
    Activity context;
    List<T> data;
    int imageSize = 0;

    public ArtistArrayAdapter(Activity context, List<T> data) {
        super(context, R.layout.list_artist, data);
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ArtistViewHolder holder;

        int imWidthInDp= 0;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int imWidthInPx = 0;

        if (isLargeWidth()) {
            imWidthInDp = (int )(getContext().getResources().getConfiguration().screenWidthDp*0.4f*0.3f);
            imWidthInPx = (int) (imWidthInDp * displayMetrics.densityDpi / 160f);
        }
        else {
            imWidthInDp = (int )(getContext().getResources().getConfiguration().screenWidthDp*0.3f);
            imWidthInPx = (int) (imWidthInDp * displayMetrics.densityDpi / 160f);;
        }

        LayoutInflater inflater = context.getLayoutInflater();
        if (view != null) {
            holder = (ArtistViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.list_artist, parent, false);
            holder = new ArtistViewHolder(view);
            view.setTag(holder);
        }

        try {
            T artist = data.get(position);
            holder.artistName.setText(artist.getName());
            if (!artist.getImageUrl().trim().isEmpty()) {
                Picasso.with(context).load(artist.getImageUrl())
                        .resize(imWidthInPx,imWidthInPx)
                        .centerInside()
                        .error(R.drawable.ic_photo_black_48dp)
                        .into(holder.artistThumbnail);
            } else {
                Picasso.with(context).load(R.drawable.ic_photo_black_48dp)
                        .resize(imWidthInPx, imWidthInPx)
                        .centerInside()
                        .error(R.drawable.ic_photo_black_48dp)
                        .into(holder.artistThumbnail);
            }

        }catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }

        return view;
    }
    public boolean isLargeWidth() {
        return getContext().getResources().getBoolean(R.bool.large_screen);
    }

    static class ArtistViewHolder {
        @Bind(R.id.artist_name) TextView artistName;
        @Bind(R.id.artist_thumbnail) ImageView artistThumbnail;

        public ArtistViewHolder (View view) {
            ButterKnife.bind(this, view);
        }
    }

}
