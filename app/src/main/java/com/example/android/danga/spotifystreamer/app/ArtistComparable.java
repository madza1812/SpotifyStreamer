package com.example.android.danga.spotifystreamer.app;

import java.util.Comparator;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by An on 6/20/2015.
 */
public class ArtistComparable implements Comparator<Artist> {

    @Override
    public int compare(Artist artist1, Artist artist2) {
        int imSize1, imSize2, folNum1, folNum2,pop1, pop2;
        imSize1 = artist1.images.size();
        imSize2 = artist2.images.size();
        folNum1 = artist1.followers.total;
        folNum2 = artist2.followers.total;
        pop1 = artist1.popularity;
        pop2 = artist2.popularity;

        if ((imSize1 > 0 && imSize2 > 0)|| (imSize1 == 0 && imSize2 == 0)) {
            if (folNum1 == folNum2 ) {
                return (pop1 > pop2 ? -1 : (pop1 == pop2 ? 0 : 1));
            } else if (folNum1 > folNum2)
                return -1;
            else return 1;
        } else if (imSize1 == 0 && imSize2 > 0)
            return 1;
        else // imSize1 > 0 && imSize2 == 0
            return -1;
    }
}
