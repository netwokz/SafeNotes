package com.netwokz.safenotes;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

public class SQLiteBase extends SQLiteOpenHelper {
    private static SQLiteBase instance;

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "notes.db";

    private static final String TEXT_TYPE = " TEXT";
    public static final String CREATE_BDD =
            "CREATE TABLE " +
                    Notes.NotesEntry.TABLE_NAME + " (" +
                    Notes.NotesEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TEXT_TYPE + "," +
                    Notes.NotesEntry.COL_NOTE + TEXT_TYPE + "," +
                    Notes.NotesEntry.COL_TITLE + TEXT_TYPE + "," +
                    Notes.NotesEntry.COL_DATE_CREATION + TEXT_TYPE + "," +
                    Notes.NotesEntry.COL_DATE_MODIFICATION + TEXT_TYPE + "," +
                    Notes.NotesEntry.COL_PASSWORD + " VARCHAR(41) );";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Notes.NotesEntry.TABLE_NAME;

    public SQLiteBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static public synchronized SQLiteBase getInstance(Context context) {
        if (instance == null) {
            instance = new SQLiteBase(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BDD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Create password column when upgrading old version ( < 1.1 )
        if (oldVersion < 2) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }
}
