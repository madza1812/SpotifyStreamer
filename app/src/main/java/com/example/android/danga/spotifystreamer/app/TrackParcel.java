package com.example.android.danga.spotifystreamer.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by An on 6/26/2015.
 */
public class TrackParcel implements Parcelable {

    private static final String KEY_NAME = "track_name";
    private static final String KEY_ID = "track_id";
    private static final String KEY_ALBUM_NAME = "album_name";
    private static final String KEY_ALBUM_THUMBNAIL_URL = "album_thumbnail_url";
    private static final String KEY_PREVIEW_URL = "preview_url";
    private static final String KEY_ARTIST_NAMES = "artist_names";
    private static final String KEY_EXT_URL = "track_ext_url";

    private String name;
    private String id;
    private String albumName;
    private String albumThumbnailUrl;
    private String previewUrl;
    private String artistNames;
    private String extUrl;


    public TrackParcel () {}

    public TrackParcel(String name,
                       String id,
                       String albumName,
                       String albumThumbnailUrl,
                       String previewUrl,
                       String artistNames,
                       String extUrl) {
        this.name = name;
        this.id = id;
        this.albumName = albumName;
        this.albumThumbnailUrl = albumThumbnailUrl;
        this.previewUrl = previewUrl;
        this.artistNames = artistNames;
        this.extUrl = extUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getAlbumThumbnailUrl() {
        return albumThumbnailUrl;
    }

    public void setAlbumThumbnailUrl(String albumThumbnailUrl) {
        this.albumThumbnailUrl = albumThumbnailUrl;
    }

    public String getArtistNames() {
        return artistNames;
    }

    public void setArtistNames(String artistNames) {
        this.artistNames = artistNames;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getExtUrl() {
        return extUrl;
    }

    public void setExtUrl(String extUrl) {
        this.extUrl = extUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_NAME, name);
        bundle.putString(KEY_ID, id);
        bundle.putString(KEY_ALBUM_NAME, albumName);
        bundle.putString(KEY_ALBUM_THUMBNAIL_URL, albumThumbnailUrl);
        bundle.putString(KEY_PREVIEW_URL, previewUrl);
        bundle.putString(KEY_ARTIST_NAMES, artistNames);
        bundle.putString(KEY_EXT_URL, extUrl);

        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<TrackParcel> CREATOR = new Creator<TrackParcel>() {
        @Override
        public TrackParcel createFromParcel(Parcel source) {
            Bundle bundle = source.readBundle();
            return new TrackParcel(bundle.getString(KEY_NAME),
                    bundle.getString(KEY_ID),
                    bundle.getString(KEY_ALBUM_NAME),
                    bundle.getString(KEY_ALBUM_THUMBNAIL_URL),
                    bundle.getString(KEY_PREVIEW_URL),
                    bundle.getString(KEY_ARTIST_NAMES),
                    bundle.getString(KEY_EXT_URL));
        }

        @Override
        public TrackParcel[] newArray(int size) {
            return new TrackParcel[size];
        }
    };

}
