package me.thomasvt.magisterclient;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Created by me on 3/28/2015.
 */
public final class Utils {
    public static String convertStream(InputStream is) {
        Scanner s = new Scanner(is, "UTF-8");
        s.useDelimiter("\\A");
        String a = s.hasNext() ? s.next() : "";
        s.close();
        return a;
    }
}
