package com.netwokz.safenotes;

import android.provider.BaseColumns;

public final class Notes {
    public Notes() {
    }

    public static abstract class NotesEntry implements BaseColumns {
        public static final String TABLE_NAME = "table_notes";
        public static final String COL_ID = "ID";
        public static final String COL_NOTE = "Note";
        public static final String COL_TITLE = "Title";
        public static final String COL_DATE_CREATION = "Date_creation";
        public static final String COL_DATE_MODIFICATION = "Date_modification";
        public static final String COL_PASSWORD = "password";
    }
}
