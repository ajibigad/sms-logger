package com.ajibigad.smslogger.smslogger;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.ajibigad.smslogger.smslogger.provider.SmsLoggerContentProvider;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{


    // STEP 1: Write a broadcast receiver to receive all text messages DONE!
    // STEP 2: Assign the task of saving the messages to the database to a service
    // STEP 3: Write a sync adapter that would move the messages from database to the server
    // STEP 4: Figure out a way to know messages that have been sent to the server and delete them from the database

    private MessageCursorAdapter cursorAdapter;
    private static final String PREF_PHONE_NUMBER = "phoneNumber";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final boolean phoneNumberSetup = PreferenceManager
                .getDefaultSharedPreferences(this).contains(PREF_PHONE_NUMBER);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if(phoneNumberSetup){
            fab.setVisibility(View.GONE);
        }
        else{
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Activity thisActivity = MainActivity.this;
                    AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                    // Get the layout inflater
                    LayoutInflater inflater = thisActivity.getLayoutInflater();

                    // Inflate and set the layout for the dialog
                    // Pass null as the parent view because its going in the dialog layout
                    builder.setView(inflater.inflate(R.layout.dialog_phone_number, null))
                            // Add action buttons
                            .setPositiveButton(R.string.setPhoneNumber, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // add the Number or name
                                    TextView phoneNumberText = (TextView) findViewById(R.id.phoneNumber);
                                    if(!TextUtils.isEmpty(phoneNumberText.getText())){
                                        String phoneNumber = (String) phoneNumberText.getText();
                                        PreferenceManager.getDefaultSharedPreferences(thisActivity).edit()
                                                .putString(PREF_PHONE_NUMBER, phoneNumber).commit();
                                        Snackbar.make(view, "Name or Number set", Snackbar.LENGTH_LONG)
                                                .setAction("Action", null).show();
                                    }
                                    else{
                                        Snackbar.make(view, "Empty Name or Number not allowed. Click the Fab to reset", Snackbar.LENGTH_LONG)
                                                .setAction("Action", null).show();
                                    }
                                }
                            }).setCancelable(false);
//                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                LoginDialogFragment.this.getDialog().cancel();
//                            }
//                        });
                }
            });
        }

        cursorAdapter = new MessageCursorAdapter(this, null, 0);
        getSupportLoaderManager().initLoader(0, null, this);
        ListView messageList = (ListView) findViewById(R.id.message_list);
        messageList.setAdapter(cursorAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        //Starts a new or restarts an existing Loader in this manager
//        getSupportLoaderManager().restartLoader(0, null, this);
//    }

    private void disableIncomingSmsReceiver(boolean disable){
        ComponentName incomingSmsReceiver = new ComponentName(this, IncomingSmsReceiver.class);
        PackageManager pm = getPackageManager();

        // Disable a manifest receiver
        pm.setComponentEnabledSetting(incomingSmsReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void enableIncomingSmsReceiver(){
        ComponentName incomingSmsReceiver = new ComponentName(this, IncomingSmsReceiver.class);
        PackageManager pm = getPackageManager();

        // Enable a manifest receiver
        pm.setComponentEnabledSetting(incomingSmsReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define the columns to retrieve
        String[] projectionFields = new String[] {
                SmsLoggerContentProvider.KEY_ID,
                SmsLoggerContentProvider.KEY_SENDER+"chc",
                SmsLoggerContentProvider.KEY_BODY,
                SmsLoggerContentProvider.KEY_MESSAGE_TIMESTAMP};
        // Construct the loader
        CursorLoader cursorLoader = new CursorLoader(MainActivity.this,
                SmsLoggerContentProvider.CONTENT_URI, // URI
                null, // projection fields
                null, // the selection criteria
                null, // the selection args
                SmsLoggerContentProvider.KEY_MESSAGE_TIMESTAMP + " desc" // the sort order
        );
        // Return the loader for use
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        for(String name : data.getColumnNames()){
            Log.i("DATABASE", name);
        }
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }
}
