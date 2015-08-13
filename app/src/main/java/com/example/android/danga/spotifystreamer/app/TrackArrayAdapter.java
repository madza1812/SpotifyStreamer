package com.example.android.danga.spotifystreamer.app;

import android.app.Activity;
import android.util.DisplayMetrics;
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

/**
 * Created by An on 6/21/2015.
 */
public class TrackArrayAdapter<T extends TrackParcel>  extends ArrayAdapter<T>{
    public final String LOG_TAG = TrackArrayAdapter.class.getSimpleName();
    Activity context;
    List<T> data;

    public TrackArrayAdapter(Activity context, List<T> data) {
        super(context, R.layout.list_top_track, data);
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ArtistDetailViewHolder holder;

        int imWidthInDp= 0;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int imWidthInPx = 0;
        if (isLargeWidth()) {
            imWidthInDp = (int )(getContext().getResources().getConfiguration().screenWidthDp*0.6f*0.3f);
            imWidthInPx = (int) (imWidthInDp * displayMetrics.densityDpi / 160f);
        }
        else {
            imWidthInDp = (int )(getContext().getResources().getConfiguration().screenWidthDp*0.3f);
            imWidthInPx = (int) (imWidthInDp * displayMetrics.densityDpi / 160f);;
        }
        LayoutInflater inflater = context.getLayoutInflater();
        if (view != null) {
            holder = (ArtistDetailViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.list_top_track, parent, false);
            holder = new ArtistDetailViewHolder(view);
            view.setTag(holder);
        }

        T track = data.get(position);
        holder.trackName.setText(track.getName());
        holder.albumTitle.setText(track.getAlbumName());
        if (!track.getAlbumThumbnailUrl().trim().isEmpty()) {
            Picasso.with(context).load(track.getAlbumThumbnailUrl())
                    .resize(imWidthInPx, imWidthInPx)
                    .centerInside()
                    .placeholder(R.drawable.loading_icon)
                    .error(R.drawable.ic_photo_black_48dp)
                    .into(holder.albumThumbnail);
        } else{
            Picasso.with(context).load(R.drawable.ic_photo_black_48dp)
                    .resize(imWidthInPx, imWidthInPx)
                    .centerInside()
                    .into(holder.albumThumbnail);
        }
        return view;
    }
    public boolean isLargeWidth() {
        return getContext().getResources().getBoolean(R.bool.large_screen);
    }

    static class ArtistDetailViewHolder {
        @Bind(R.id.track_title_textview) TextView trackName;
        @Bind(R.id.album_title_textview) TextView albumTitle;
        @Bind(R.id.album_thumbnail) ImageView albumThumbnail;

        public ArtistDetailViewHolder (View view) {
            ButterKnife.bind(this, view);
        }
    }
}
