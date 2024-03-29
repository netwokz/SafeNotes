package com.netwokz.safenotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public final class NotesBDD {

    private static final int VERSION_BDD = 2;
    private static final String NOM_BDD = "notes.db";

    private static final String TABLE_NOTES = "table_notes";
    private static final String COL_ID = "ID";
    private static final int NUM_COL_ID = 0;
    private static final String COL_NOTE = "Note";
    private static final int NUM_COL_ISBN = 1;
    private static final String COL_TITRE = "Titre";
    private static final int NUM_COL_TITRE = 2;
    private static final String COL_DATECREATION = "Date_creation";
    private static final int NUM_COL_DATECREATION = 3;
    private static final String COL_DATEMODIFICATION = "Date_modification";
    private static final int NUM_COL_DATEMODIFICATION = 4;
    private static final String COL_PASSWORD = "password";
    private static final int NUM_COL_PASSWORD = 5;
    private static final String SQL_ORDER = " ORDER BY ";
    private static final String DATA_PATH = "/data/";
    private static final String DATABASE_FOLDER = "/databases/";

    private SQLiteDatabase bdd;

    private SQLiteBase maBaseSQLite;

    public NotesBDD(Context context) {
        //On créer la BDD et sa table
        maBaseSQLite = SQLiteBase.getInstance(context);
    }

    public void open() {
        bdd = maBaseSQLite.getWritableDatabase("somePass");
    }

    public void close() {
        //on ferme l'accès à la BDD
        if (bdd != null) {
            bdd.close();
        }
    }

    public SQLiteDatabase getBDD() {
        return (bdd);
    }

    public long insertNote(Note note) {
        //Création d'un ContentValues (fonctionne comme une HashMap)
        ContentValues values = new ContentValues();

        //on lui ajoute une valeur associé à une clé (qui est le nom de la colonne dans laquelle on veut mettre la valeur)
        values.put(COL_NOTE, note.getNote());
        values.put(COL_TITRE, note.getTitre());
        values.put(COL_DATECREATION, note.getDateCreation());
        values.put(COL_DATEMODIFICATION, note.getDateModification());
        //on insère l'objet dans la BDD via le ContentValues
        return (bdd.insert(TABLE_NOTES, null, values));
    }

    public int updateNote(int id, Note note) {
        //La mise à jour d'une note dans la BDD fonctionne plus ou moins comme une insertion
        //il faut simple préciser quelle note on doit mettre à jour grâce à l'ID
        ContentValues values = new ContentValues();

        values.put(COL_NOTE, note.getNote());
        values.put(COL_TITRE, note.getTitre());
        values.put(COL_DATEMODIFICATION, note.getDateModification());
        return (bdd.update(TABLE_NOTES, values, COL_ID + " = " + id, null));
    }

    public int updatePassword(int id, String pw) {
        ContentValues values = new ContentValues();

        values.put(COL_PASSWORD, pw);
        return (bdd.update(TABLE_NOTES, values, COL_ID + " = " + id, null));
    }

    public int removeNoteWithID(int id) {
        //Suppression d'un livre de la BDD grâce à l'ID
        return (bdd.delete(TABLE_NOTES, COL_ID + " = " + id, null));
    }

    public Note getNoteWithTitre(String titre) {
        //Récupère dans un Cursor les valeur correspondant à une note contenue dans la BDD (ici on sélectionne la note grâce à son titre)
        Cursor c = bdd.query(TABLE_NOTES, new String[]{COL_ID, COL_NOTE, COL_TITRE, COL_DATECREATION, COL_DATEMODIFICATION}, COL_TITRE + " LIKE ? ", new String[]{titre}, null, null, null);

        return (cursorToNote(c));
    }

    public Note getNoteWithId(int id) {
        //Récupère dans un Cursor les valeur correspondant à un livre contenu dans la BDD (ici on sélectionne le livre grâce à son titre)
        Cursor c = bdd.query(TABLE_NOTES, new String[]{COL_ID, COL_NOTE, COL_TITRE, COL_DATECREATION, COL_DATEMODIFICATION}, COL_ID + " LIKE " + id + "", null, null, null, null);

        return (cursorToNote(c));
    }

    //Cette méthode permet de convertir un cursor en une note
    private Note cursorToNote(Cursor c) {
        //si aucun élément n'a été retourné dans la requête, on renvoie null
        if (c.getCount() == 0) {
            return (null);
        }

        //Sinon on se place sur le premier élément
        c.moveToFirst();
        //On créé un livre
        Note note = new Note();
        //on lui affecte toutes les infos grâce aux infos contenues dans le Cursor
        note.setId(c.getInt(NUM_COL_ID));
        note.setNote(c.getString(NUM_COL_ISBN));
        note.setTitre(c.getString(NUM_COL_TITRE));
        note.setDateCreation(c.getString(NUM_COL_DATECREATION));
        note.setDateModification(c.getString(NUM_COL_DATEMODIFICATION));
        //On ferme le cursor
        c.close();

        //On retourne la note
        return (note);
    }

    public List<Note> getAllNotes(int tri, boolean ordre) {
        List<Note> noteList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES + SQL_ORDER;
        // Select All Query
        if (tri == 1) {
            selectQuery += COL_DATECREATION + " ";
        } else if (tri == 2) {
            selectQuery += COL_DATEMODIFICATION + " ";
        } else if (tri == 3) {
            selectQuery += COL_TITRE + " ";
        } else {
            selectQuery += COL_TITRE + " COLLATE NOCASE ";
        }

        if (!ordre) {
            selectQuery = selectQuery + " DESC";
        }

        SQLiteDatabase db = this.maBaseSQLite.getWritableDatabase("somePass");
        return (fillListNote(db, selectQuery, noteList, null, false));
    }

    public List<Note> getSearchedNotes(String str, Boolean contentSearch, Boolean sensitiveSearch, int tri, boolean ordre) {
        List<Note> noteList = new ArrayList<>();
        // Select All Query
        SQLiteDatabase db = this.maBaseSQLite.getWritableDatabase("somePass");
        String selectQuery = null;
        if (sensitiveSearch) {
            db.rawQuery("PRAGMA case_sensitive_like=ON;", null);
        } else {
            db.rawQuery("PRAGMA case_sensitive_like=OFF;", null);
        }

        selectQuery = "SELECT  * FROM " + TABLE_NOTES + " WHERE ";
        if (!contentSearch) {
            selectQuery += COL_TITRE + " LIKE ? ";
        } else {
            selectQuery += COL_TITRE + " LIKE ?" + " OR ( " + COL_NOTE + " LIKE ? AND " + COL_PASSWORD + " IS NULL) ";
        }
        selectQuery += SQL_ORDER;

        if (tri == 1) {
            selectQuery += COL_DATECREATION + " ";
        } else if (tri == 2) {
            selectQuery += COL_DATEMODIFICATION + " ";
        } else if (tri == 3) {
            selectQuery += COL_TITRE + " ";
        } else {
            selectQuery += COL_TITRE + " COLLATE NOCASE ";
        }

        if (!ordre) {
            selectQuery += " DESC";
        }

        return (fillListNote(db, selectQuery, noteList, str, contentSearch));
    }

    public List<Note> fillListNote(SQLiteDatabase db, String selectQuery, List<Note> noteList, String s, boolean contentSearch) {
        Cursor c;

        if (s != null && contentSearch) {
            c = db.rawQuery(selectQuery, new String[]{"%" + s + "%", "%" + s + "%"});
        } else if (s != null) {
            c = db.rawQuery(selectQuery, new String[]{"%" + s + "%"});
        } else {
            c = db.rawQuery(selectQuery, null);
        }

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(c.getInt(NUM_COL_ID));
                note.setNote(c.getString(NUM_COL_ISBN));
                note.setTitre(c.getString(NUM_COL_TITRE));
                note.setDateCreation(c.getString(NUM_COL_DATECREATION));
                note.setDateModification(c.getString(NUM_COL_DATEMODIFICATION));
                if (c.getString(NUM_COL_PASSWORD) != null) {
                    note.setPassword(c.getString(NUM_COL_PASSWORD));
                }
                // Adding contact to list
                noteList.add(note);
            } while (c.moveToNext());
        }

        // return contact list
        c.close();
        return (noteList);
    }

    // Getting notes Count

    /*public int getNotesCount()
     * {
     *  String countQuery = "SELECT  * FROM " + TABLE_NOTES;
     *  SQLiteDatabase db = this.maBaseSQLite.getReadableDatabase();
     *  Cursor cursor = db.rawQuery(countQuery, null);
     *  cursor.close();
     *
     *  // return count
     *  return cursor.getCount();
     * }*/

    public String exportDB() {
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source = null;
        FileChannel destination = null;
        String currentDBPath = DATA_PATH + BuildConfig.APPLICATION_ID + DATABASE_FOLDER + NOM_BDD;
        String backupDBPath = BuildConfig.APPLICATION_ID + "/unote_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime()) + ".db";
        File currentDB = new File(data, currentDBPath);
        File backupDirDB = new File(sd, BuildConfig.APPLICATION_ID);

        backupDirDB.mkdirs();
        File backupDB = new File(sd, backupDBPath);
        try {
            backupDB.createNewFile();
            // returns true, if the named file doesn't exist and was successfully created
            // returns false if the file exists => overwritten, manual action db cannot changed
        } catch (IOException e) {
            Log.e(BuildConfig.APPLICATION_ID, "IOException exportDB", e);
            return (null);
        }

        try (
                FileInputStream s = new FileInputStream(currentDB);
                FileOutputStream d = new FileOutputStream(backupDB);
        ) {
            source = s.getChannel();
            destination = d.getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        } catch (IOException e) {
            Log.e(BuildConfig.APPLICATION_ID, "IOException exportDB", e);
            return (null);
        }
        return (backupDB.toString());
    }

    public String importDB(File dbToImport) {
        File data = Environment.getDataDirectory();
        FileChannel source = null;
        FileChannel destination = null;
        String currentDBPath = DATA_PATH + BuildConfig.APPLICATION_ID + DATABASE_FOLDER + NOM_BDD;
        File currentDB = new File(data, currentDBPath);

        if (dbToImport.exists()) {
            try (
                    FileInputStream s = new FileInputStream(dbToImport);
                    FileOutputStream d = new FileOutputStream(currentDB);
            ) {
                source = s.getChannel();
                destination = d.getChannel();
                destination.transferFrom(source, 0, source.size());
                source.close();
                destination.close();
            } catch (IOException e) {
                Log.e(BuildConfig.APPLICATION_ID, "IOException importDB", e);
            }
            try {
                bdd.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_PASSWORD + " VARCHAR(41);");
            } catch (Exception e) {
                Log.e(BuildConfig.APPLICATION_ID, "Exception importDB", e);
            }
            return (dbToImport.toString());
        } else {
            return (null);
        }
    }

    public String exportCSV() {
        File sd = Environment.getExternalStorageDirectory();
        String exportCSVFile = BuildConfig.APPLICATION_ID + "/unote_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime()) + ".csv";
        File exportDir = new File(sd, "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file = new File(exportDir, exportCSVFile);
        try {
            FileWriter csvWrite = new FileWriter(file, true);
            String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " WHERE " + COL_PASSWORD + " IS NULL ";
            if (this.maBaseSQLite == null)
                return "null";

            if (this.bdd == null)
                return "null2";
            //SQLiteDatabase db =
            //this.open();
            //db.close();
            Cursor c = this.bdd.rawQuery(selectQuery, null);

            // looping through all rows and adding to list
            if (c.moveToFirst()) {
                CSVUtils.writeLine(csvWrite, Arrays.asList("TITLE",
                        "DATE_CREATION",
                        "DATE_MODIFICATION",
                        "NOTE"),
                        ',',
                        '"');
                do {
                    CSVUtils.writeLine(csvWrite, Arrays.asList(c.getString(NUM_COL_TITRE),
                            c.getString(NUM_COL_DATECREATION),
                            c.getString(NUM_COL_DATEMODIFICATION),
                            c.getString(NUM_COL_ISBN)),
                            ',',
                            '"');

                } while (c.moveToNext());
            }

            // return contact list
            csvWrite.close();
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (file.toString());
    }
}
