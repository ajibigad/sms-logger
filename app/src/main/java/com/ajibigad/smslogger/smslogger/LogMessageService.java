package com.ajibigad.smslogger.smslogger;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.ajibigad.smslogger.smslogger.provider.SmsLoggerContentProvider;
import com.facebook.stetho.common.StringUtil;

public class LogMessageService extends IntentService {

    private final String TAG = LogMessageService.class.getSimpleName();

    // the service should be able to re log the messages in case of any restarts by the system
    // the service would check this START_FLAG_REDELIVERY and then use the original intent sent to it
    //then later use a syncAdapter or some form of scheduled service to update the message on the webservice

    //pass the pdus object array as an extra to the service intent or get the sms messages and add as extra
    public LogMessageService() {
        super(LogMessageService.class.getSimpleName());
    }

    public LogMessageService(String name){
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create account, if needed
        SyncUtils.CreateSyncAccount(getApplicationContext());
        Log.i(TAG, "Default Account created");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // get the extras and insert messages in the database
        // all this would run in a background thread and intents would be queued as they come in.
        // it would then terminate itself onces it has completed all the intents sent
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Bundle pduBundle = (Bundle)bundle.get("pdus");
            Object[] pdus = (Object[]) pduBundle.get("pdus");
            final SmsMessage[] messages = new SmsMessage[pdus.length];
            for (int i = 0; i < pdus.length; i++)
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            insertMessages(messages);
        }
    }

    private void insertMessages(final SmsMessage [] messages){
        // Create a new row of values to insert.
        ContentValues [] newMessages = new ContentValues[messages.length];
        TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tMgr.getLine1Number();
        phoneNumber = phoneNumber == null || TextUtils.isEmpty(phoneNumber) ? "No Number" : phoneNumber; // there should be a more sensible
        for(int i = 0; i< newMessages.length; i++){
            ContentValues newMessage = new ContentValues();
            newMessage.put(SmsLoggerContentProvider.KEY_PHONE_NUMBER, phoneNumber);
            newMessage.put(SmsLoggerContentProvider.KEY_MESSAGE_TIMESTAMP, messages[i].getTimestampMillis());
            newMessage.put(SmsLoggerContentProvider.KEY_SENDER, messages[i].getOriginatingAddress());
            newMessage.put(SmsLoggerContentProvider.KEY_BODY, messages[i].getMessageBody());
            newMessage.put(SmsLoggerContentProvider.KEY_STATE, 1);
            newMessages[i] = newMessage;
        }

        ContentResolver cr = getContentResolver();
        // Insert the row into your table
        int successfulInsertCount = cr.bulkInsert(SmsLoggerContentProvider.CONTENT_URI, newMessages);
    }
}
