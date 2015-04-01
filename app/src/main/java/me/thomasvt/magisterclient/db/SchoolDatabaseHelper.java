package me.thomasvt.magisterclient.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by me on 4/1/2015.
 */
public class SchoolDatabaseHelper {
    private SchoolDatabase mSchoolDatabase;
    private SQLiteDatabase mSqlite;
    public SchoolDatabaseHelper(Context context) {
        mSchoolDatabase = new SchoolDatabase(context);
        mSqlite = mSchoolDatabase.getWritableDatabase();
    }

    public void close() {
        mSqlite.close();
    }

    private List<School> _getSchools(boolean favourites) {
        Cursor cursor;
        if(favourites)
            cursor = mSqlite.query(SchoolDatabase.TABLE_SCHOOLS, null, SchoolDatabase.Fields.CFAVOURITE + " > 0", null, null, null, SchoolDatabase.Fields.CNAME);
        else
            cursor = mSqlite.query(SchoolDatabase.TABLE_SCHOOLS, null, null, null, null, null, SchoolDatabase.Fields.CNAME);

        List<School> schoolList = new ArrayList<>();
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            schoolList.add(getSchoolFromCursor(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return schoolList;
    }

    public List<School> getSchools() {
        return _getSchools(false);
    }

    public List<School> getFavourites() {
        return _getSchools(true);
    }

    public void setFavourite(int id, boolean favourite) {
        ContentValues values = new ContentValues();
        values.put(SchoolDatabase.Fields.CFAVOURITE, favourite);
        mSqlite.update(SchoolDatabase.TABLE_SCHOOLS, values, "id = " + id, null);
    }

    private static School getSchoolFromCursor(Cursor cursor) {
        School school = new School();
        school.id = cursor.getInt(0);
        school.name = cursor.getString(1);
        school.host = cursor.getString(2);
        school.favourite = cursor.getInt(3) > 0;
        return school;
    }

    public boolean hasFavourites() {
        Cursor cursor = mSqlite.query(SchoolDatabase.TABLE_SCHOOLS, null, SchoolDatabase.Fields.CFAVOURITE + " > 0", null, null, null, null, "1");
        cursor.moveToFirst();
        boolean hasNoFavourites = cursor.isAfterLast();
        cursor.close();
        return !hasNoFavourites;
    }

    public void setSchools(List<School> schools) {
        mSqlite.delete(SchoolDatabase.TABLE_SCHOOLS, null, null);
        mSqlite.beginTransaction();
        ContentValues values = new ContentValues();
        for(School school : schools) {
            values.put(SchoolDatabase.Fields.CNAME, school.name);
            values.put(SchoolDatabase.Fields.CHOST, school.host);
            values.put(SchoolDatabase.Fields.CFAVOURITE, school.favourite);
            mSqlite.insert(SchoolDatabase.TABLE_SCHOOLS, null, values);
        }
        mSqlite.setTransactionSuccessful();
        mSqlite.endTransaction();
    }
}
