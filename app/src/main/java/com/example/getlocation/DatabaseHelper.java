package com.example.getlocation;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "userstore.db";
    private static final int SCHEMA = 1;
    static final String TABLE = "nodes";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE + "(" + COLUMN_ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_NAME
                + " TEXT, " + COLUMN_LATITUDE + " TEXT, " + COLUMN_LONGITUDE + " TEXT);");

    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,  int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE);
        onCreate(db);
    }

    public void addNode(SQLiteDatabase db, String name, String latitude1, String longitude1){
        db.execSQL("INSERT INTO "+ TABLE +" (" + COLUMN_NAME
                + ", " + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + ")" +
                " VALUES ('" + name + "', '" + latitude1 + "', '" + longitude1 + "');");
    }

    public void cleanTable(SQLiteDatabase db){
        db.execSQL("DELETE FROM " + TABLE);
    }
}
