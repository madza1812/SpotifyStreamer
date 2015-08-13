package com.example.android.danga.spotifystreamer.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by An on 6/25/2015.
 */
// Class to save the data when restarting the activity due to configuration changed.
// Such as screen rotation.
public class ArtistParcel implements Parcelable {

    // Parcel keys
    private static final String KEY_NAME = "name";
    private static final String KEY_ID = "id";
    private static final String KEY_IMAGEURL = "imageUrl";

    private String name;
    private String id;
    private String imageUrl;

    public ArtistParcel () {}

    public ArtistParcel (String name, String id, String imageUrl) {
        this.name = name;
        this.id = id;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        // Insert the key-value pari into the bundle
        bundle.putString(KEY_NAME, name);
        bundle.putString(KEY_ID, id);
        bundle.putString(KEY_IMAGEURL, imageUrl);

        // Write the key-valu pairs to the parcel
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<ArtistParcel> CREATOR = new Creator<ArtistParcel>() {
        @Override
        public ArtistParcel createFromParcel(Parcel source) {
            // Read the bundle containing key-value pairs from the parcel
            Bundle bundle= source.readBundle();
            return new ArtistParcel(bundle.getString(KEY_NAME),
                    bundle.getString(KEY_ID),
                    bundle.getString(KEY_IMAGEURL));
        }

        @Override
        public ArtistParcel[] newArray(int size) {
            return new ArtistParcel[size];
        }
    };
}