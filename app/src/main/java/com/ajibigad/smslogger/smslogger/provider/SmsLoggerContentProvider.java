package com.ajibigad.smslogger.smslogger.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class SmsLoggerContentProvider extends ContentProvider {

    public static final String KEY_ID = "_id";
    public static final String KEY_BODY = "body";
    public static final String KEY_PHONE_NUMBER = "phoneNumber";
    public static final String KEY_SENDER = "sender";
    public static final String KEY_MESSAGE_TIMESTAMP = "timeSent"; // i should have named this message timestamp

    // This is a boolean that tells if the message has been sent or not. Sent messages should be deleted
    public static final String KEY_STATE = "state";

    public static final String CONTENT_AUTHORITY = "com.ajibigad.smsloggerprovider";

    public static final Uri CONTENT_URI =
            Uri.parse("content://" + CONTENT_AUTHORITY + "/messages");

    private SmsLoggerDBOpenHelper smsLoggerDBOpenHelper;

    private static final int ALLROWS = 1;
    private static final int SINGLE_ROW = 2;
    private static final UriMatcher uriMatcher;
    //Populate the UriMatcher object, where a URI ending in "todoitems" will
    //correspond to a request for all items, and "todoitems/[rowID]"
    //represents a single row.
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.ajibigad.smsloggerprovider", "messages", ALLROWS);
        uriMatcher.addURI("com.ajibigad.smsloggerprovider", "messages/#", SINGLE_ROW);
    }

    public SmsLoggerContentProvider() {
    }

    @Override
    public boolean onCreate() {
        smsLoggerDBOpenHelper = new SmsLoggerDBOpenHelper(getContext(),
                SmsLoggerDBOpenHelper.DATABASE_NAME, null,
                SmsLoggerDBOpenHelper.DATABASE_VERSION);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // Return a string that identifies the MIME type
        // for a Content Provider URI
        switch (uriMatcher.match(uri)) {
            case ALLROWS: return "vnd.android.cursor.dir/vnd.ajibigad.messages";
            case SINGLE_ROW: return "vnd.android.cursor.item/vnd.ajibigad.messages";
            default: throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Open a read / write database to support the transaction.
        SQLiteDatabase db = smsLoggerDBOpenHelper.getWritableDatabase();
        // If this is a row URI, limit the deletion to the specified row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW :
                String rowID = uri.getPathSegments().get(1);
                selection = KEY_ID + "=" + rowID
                    + (!TextUtils.isEmpty(selection) ?
                " AND (" + selection + ")" : "");
            default: break;
        }
        // To return the number of deleted items, you must specify a where
        // clause. To delete all rows and return a value, pass in "1".
        if (selection == null)
            selection = "1";

        // Execute the deletion.
        int deleteCount = db.delete(SmsLoggerDBOpenHelper.DATABASE_TABLE, selection,
                selectionArgs);
        // Notify any observers of the change in the data set.
        getContext().getContentResolver().notifyChange(uri, null);
        return deleteCount;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Open a read / write database to support the transaction.
        SQLiteDatabase db = smsLoggerDBOpenHelper.getWritableDatabase();
        // To add empty rows to your database by passing in an empty Content Values
        // object, you must use the null column hack parameter to specify the name of
        // the column that can be set to null.
        String nullColumnHack = null;
        // Insert the values into the table
        long id = db.insert(SmsLoggerDBOpenHelper.DATABASE_TABLE,
                nullColumnHack, values);
        if (id > -1) {
        // Construct and return the URI of the newly inserted row.
            Uri insertedId = ContentUris.withAppendedId(CONTENT_URI, id);
        // Notify any observers of the change in the data set.
            getContext().getContentResolver().notifyChange(insertedId, null);
            return insertedId;
        }
        else
            return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Open a read-only database.
        SQLiteDatabase db = smsLoggerDBOpenHelper.getWritableDatabase();
        // Replace these with valid SQL statements if necessary.
        String groupBy = null;
        String having = null;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(SmsLoggerDBOpenHelper.DATABASE_TABLE);
        // If this is a row query, limit the result set to the passed in row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW :
                String rowID = uri.getPathSegments().get(1);
                queryBuilder.appendWhere(KEY_ID + "=" + rowID);
            default: break;
        }
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, groupBy, having, sortOrder);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Open a read / write database to support the transaction.
        SQLiteDatabase db = smsLoggerDBOpenHelper.getWritableDatabase();
        // If this is a row URI, limit the deletion to the specified row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW :
                String rowID = uri.getPathSegments().get(1);
                selection = KEY_ID + "=" + rowID
                    + (!TextUtils.isEmpty(selection) ?
                " AND (" + selection + ")" : "");
            default: break;
        }
        // Perform the update.
        int updateCount = db.update(SmsLoggerDBOpenHelper.DATABASE_TABLE,
                values, selection, selectionArgs);
        // Notify any observers of the change in the data set.
        getContext().getContentResolver().notifyChange(uri, null);
        return updateCount;
    }

    private static class SmsLoggerDBOpenHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "smsLoggerDB.db";
        private static final String DATABASE_TABLE = "messages";
        private static final int DATABASE_VERSION = 4;

        // SQL Statement to create a new database.
        private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " (" +
                KEY_ID + " integer primary key autoincrement, " +
                KEY_BODY + " text not null, " +
                KEY_PHONE_NUMBER + " text not null, " +
                KEY_SENDER + " text not null, " +
                KEY_MESSAGE_TIMESTAMP + " integer not null, " +
                KEY_STATE + " integer not null);";

        public SmsLoggerDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            // Create a new one.
            onCreate(sqLiteDatabase);
        }
    }
}
