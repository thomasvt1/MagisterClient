package me.thomasvt.magisterclient.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SchoolDatabase extends SQLiteOpenHelper {
    public SchoolDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    protected static final String DATABASE_NAME = "schools.db";
    protected static final int DATABASE_VERSION = 1;
    public static final String TABLE_SCHOOLS = "schools";

    public static class Fields {
        public static final String CID = "id";
        public static final String CNAME = "name";
        public static final String CHOST = "host";
        public static final String CFAVOURITE = "favourite";
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s BOOLEAN)", TABLE_SCHOOLS, Fields.CID, Fields.CNAME, Fields.CHOST, Fields.CFAVOURITE));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }
}
