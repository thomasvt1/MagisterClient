package me.thomasvt.magisterclient;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import me.thomasvt.magisterclient.db.School;

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

    public static void sortSchoolList(List<School> schoolList) {
        Collections.sort(schoolList, new Comparator<School>() {
            @Override
            public int compare(School school, School t1) {
                return school.name.compareToIgnoreCase(t1.name);
            }
        });
    }
}
