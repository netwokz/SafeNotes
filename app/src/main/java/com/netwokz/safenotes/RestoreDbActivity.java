package com.netwokz.safenotes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;

public class RestoreDbActivity extends ListActivity {

    private static final int DIALOG_RESTORE_WARN = 300;
    private static final String EXTENSION = ".db";
    private static final String NOM_BDD = "notes.db";

    private ArrayAdapter<NameOnlyFile> adapter;
    private AlertDialog.Builder restoreWarningBuilder;
    private NameOnlyFile restoreFile;
    private FilenameFilter restoreFilenameFilter;
    private Comparator<NameOnlyFile> comparator;
    private SharedPreferences pref;
    int textSize;
    boolean toast_enabled;

    public RestoreDbActivity() {
        restoreFilenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return (filename.endsWith(EXTENSION));
            }
        };

        comparator = new Comparator<NameOnlyFile>() {
            @Override
            public int compare(NameOnlyFile object1, NameOnlyFile object2) {
                return (object2.toString().compareTo(object1.toString()));
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        textSize = Integer.parseInt(pref.getString("pref_sizeNote", "18"));
        if (textSize == -1) {
            textSize = Integer.parseInt(pref.getString("pref_sizeNote_custom", "18"));
        }
        toast_enabled = pref.getBoolean("pref_notifications", true);
        adapter = new ArrayAdapter<NameOnlyFile>(this, R.layout.restore_entry, R.id.RestoreName) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Cast the list view each item as text view
                TextView item = (TextView) super.getView(position, convertView, parent);
                // Change the item text size
                item.setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
                // return the view
                return item;
            }
        };

        refresh();

        TextView tv = new TextView(this);
        tv.setText(new File(
                Environment.getExternalStorageDirectory(),
                BuildConfig.APPLICATION_ID).toString() + "/*" + EXTENSION);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextSize((int) (0.8 * textSize));
        setListAdapter(adapter);
        registerForContextMenu(getListView());
        getListView().addHeaderView(tv, null, false);

        restoreWarningBuilder = new AlertDialog.Builder(this);
        restoreWarningBuilder.setMessage(R.string.restoreWarnMessage)
                .setCancelable(true)
                .setTitle(R.string.restoreWarnTitle)
                .setPositiveButton(R.string.restoreWarnPositive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        doRestore();
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.restoreWarnNegative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_WARN) {
            AlertDialog adRestoreWarningBuilder = restoreWarningBuilder.create();
            return (adRestoreWarningBuilder);
        }

        return (super.onCreateDialog(id));
    }

    @Override
    public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle((this.getString(R.string.dialog_backup_menu)));
        menu.add(0, v.getId(), 0, (this.getString(R.string.dialog_backup_menu_deletion)));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo aInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        boolean res = false;
        if (item.getTitle().equals((this.getString(R.string.dialog_backup_menu_deletion)))) {
            restoreFile = adapter.getItem(aInfo.position - 1);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle(this.getString(R.string.dialog_delete_backup) + " " + adapter.getItem(aInfo.position - 1).toString())
                    .setMessage(this.getString(R.string.dialog_delete_msg))
                    .setPositiveButton(this.getString(R.string.dialog_delete_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            if (!restoreFile.getFile().delete()) {
                                Toast toast = Toast.makeText(
                                        RestoreDbActivity.this,
                                        RestoreDbActivity.this.getString(R.string.toast_backup_deleted_error),
                                        Toast.LENGTH_LONG);
                                ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
                                if (toast_enabled)
                                    toast.show();
                            } else {
                                refresh();
                                Toast toast = Toast.makeText(
                                        RestoreDbActivity.this,
                                        RestoreDbActivity.this.getString(R.string.toast_backup_deleted),
                                        Toast.LENGTH_LONG);
                                ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
                                if (toast_enabled)
                                    toast.show();
                            }
                        }
                    })
                    .setNegativeButton(this.getString(R.string.dialog_delete_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
        return (res);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        restoreFile = adapter.getItem(position - 1);

        try {
            // test restore file
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    restoreFile.getFile().getPath(), null, SQLiteDatabase.OPEN_READONLY);
            db.close();

            // Dialog: This will overwrite the existing items, continue?
            showDialog(DIALOG_RESTORE_WARN);
        } catch (Exception e) {
            Toast toast = Toast.makeText(
                    RestoreDbActivity.this,
                    RestoreDbActivity.this.getString(R.string.restoreToastInvalidDB),
                    Toast.LENGTH_LONG);
            ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
            if (toast_enabled)
                toast.show();
        }
    }

    private void refresh() {
        File backupDirectory = new File(
                Environment.getExternalStorageDirectory(),
                BuildConfig.APPLICATION_ID);

        adapter.clear();
        if (backupDirectory.isDirectory()) {
            File[] files = backupDirectory.listFiles(restoreFilenameFilter);
            for (File f : files) {
                adapter.add(new NameOnlyFile(f));
            }
            adapter.sort(comparator);
        }
        adapter.notifyDataSetChanged();
    }

    private void doRestore() {
        String state = Environment.getExternalStorageState();

        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Toast toast = Toast.makeText(
                    RestoreDbActivity.this,
                    RestoreDbActivity.this.getString(R.string.restoreToastMountProblem),
                    Toast.LENGTH_LONG);
            ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
            if (toast_enabled)
                toast.show();
            return;
        }

        // backup old file
        File dbFile = getDatabasePath(NOM_BDD);
        File dbBakFile = getDatabasePath(NOM_BDD + ".bak");
        boolean moved = dbFile.renameTo(dbBakFile);
        if (!moved) {
            Toast toast = Toast.makeText(
                    RestoreDbActivity.this,
                    R.string.restoreToastUnableToMove,
                    Toast.LENGTH_LONG);
            ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
            if (toast_enabled)
                toast.show();
            return;
        }

        try {
            // copy new file into place
            NotesBDD noteBdd = new NotesBDD(null);
            noteBdd.importDB(restoreFile.getFile());

            // delete backup
            dbBakFile.delete();
        } catch (Exception e) {
            // put the old database back
            dbFile.delete();
            dbBakFile.renameTo(dbFile);

            Toast toast = Toast.makeText(
                    RestoreDbActivity.this,
                    R.string.restoreToastCopyFailed,
                    Toast.LENGTH_LONG);
            ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
            if (toast_enabled)
                toast.show();
            return;
        }

        Toast toast = Toast.makeText(
                RestoreDbActivity.this,
                (new NameOnlyFile(restoreFile.getFile())).toString() + " " +
                        getResources().getString(R.string.restoreToastRestoreFinished),
                Toast.LENGTH_LONG);
        ((TextView) ((LinearLayout) toast.getView()).getChildAt(0)).setTextSize((int) (NoteMain.TOAST_TEXTSIZE_FACTOR * textSize));
        if (toast_enabled)
            toast.show();

        finish();
    }

    private static class NameOnlyFile {
        private File file;

        public NameOnlyFile(File f) {
            file = f;
        }

        public File getFile() {
            return (file);
        }

        @Override
        public String toString() {
            String name = file.getName();

            return (name.substring(0, name.length() - EXTENSION.length()));
        }
    }
}
