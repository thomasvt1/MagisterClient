package me.thomasvt.magisterclient.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SchoolDatabase extends SQLiteOpenHelper {
    public SchoolDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    protected static final String DATABASE_NAME = "schools.db";
    protected static final int DATABASE_VERSION = 3;
    public static final String TABLE_SCHOOLS = "schools";

    public static class Fields {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String HOST = "host";
        public static final String FAVOURITE = "favourite";
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s BOOLEAN, %s BOOLEAN)", TABLE_SCHOOLS, Fields.ID, Fields.NAME, Fields.HOST, Fields.FAVOURITE));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int before, int after) {
        // Tobias: door ondoordachte veranderingen aan de database is het nu nodig
        // de database te legen en opnieuw te vullen.
        if(after == 3) {
            sqLiteDatabase.delete(TABLE_SCHOOLS, null, null);
        }
    }
}
