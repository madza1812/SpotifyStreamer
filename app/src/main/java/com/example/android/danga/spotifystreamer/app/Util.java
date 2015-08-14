package com.example.android.danga.spotifystreamer.app;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by An on 7/14/2015.
 */
public class Util {
    private static Util util;
        private static final String LOG_TAG = "UTIL CLASS";
    public static synchronized Util instatnce() {
        if (util == null)
            util = new Util();
        return util;
    }

    public static void displayToast (Context context, CharSequence msg) {
        Toast noConToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        noConToast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
        noConToast.show();
    }

    public static String msToFormatTime(long ms) { // Format {H*:MM:SS}
        StringBuilder ftBuilder = new StringBuilder("");
        int hours = (int) (ms / (1000*60*60));
        int minutes = (int) ((ms % (1000*3600)) /(1000*60));
        int seconds = (int) ((ms % (1000*3600) % (1000*60)) / 1000);
        if (hours > 0)
            ftBuilder.append(hours).append(":");
        if (minutes < 10)
            ftBuilder.append("0").append(minutes).append(":");
        else
            ftBuilder.append(minutes);
        if (seconds < 10)
            ftBuilder.append("0").append(seconds);
        else
            ftBuilder.append(seconds);

        return ftBuilder.toString();
    }
}
