package com.example.ibnsina;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "offline_sync.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "pending_updates";
    
    public static final String COL_CODE = "code";
    public static final String COL_SHORT = "shortQty";
    public static final String COL_EXCESS = "excessQty";
    public static final String COL_REMARK = "remark";
    public static final String COL_STATUS = "status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_CODE + " TEXT PRIMARY KEY, " +
                COL_SHORT + " TEXT, " +
                COL_EXCESS + " TEXT, " +
                COL_REMARK + " TEXT, " +
                COL_STATUS + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addUpdate(String code, String s, String e, String r, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CODE, code);
        values.put(COL_SHORT, s);
        values.put(COL_EXCESS, e);
        values.put(COL_REMARK, r);
        values.put(COL_STATUS, status);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public Cursor getAllPending() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public void deleteUpdate(String code) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_CODE + "=?", new String[]{code});
        db.close();
    }
}