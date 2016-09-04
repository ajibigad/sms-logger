package com.ajibigad.smslogger.smslogger;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.ajibigad.smslogger.smslogger.models.Message;
import com.ajibigad.smslogger.smslogger.network.MessageApiEndpointInterface;
import com.ajibigad.smslogger.smslogger.provider.SmsLoggerContentProvider;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Julius on 06/08/2016.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final ContentResolver mContentResolver;

    private final String TAG = SyncAdapter.class.getSimpleName();

    private static final String MESSAGES_URL = "https://weelocate.herokuapp.com/weelocate/api/message/";

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 30000;  // 30 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 60000;  // 60 seconds to allow enough time for the server to wake up

    /**
     * Network write timeout, in milliseconds.
     */
    private static final int NET_WRITE_TIMEOUT_MILLIS = 60000;  // 60 seconds to allow enough time for the server to wake up

    private final String [] PROJECTION = {
            SmsLoggerContentProvider.KEY_ID,
            SmsLoggerContentProvider.KEY_PHONE_NUMBER,
            SmsLoggerContentProvider.KEY_SENDER,
            SmsLoggerContentProvider.KEY_BODY,
            SmsLoggerContentProvider.KEY_STATE,
            SmsLoggerContentProvider.KEY_MESSAGE_TIMESTAMP
    };

    private static final int COLUMN_ID = 0;
    private static final int COLUMN_PHONE_NUMBER = 1;
    private static final int COLUMN_SENDER = 2;
    private static final int COLUMN_BODY = 3;
    private static final int COLUMN_STATE = 4;
    private static final int COLUMN_MESSAGE_TIMESTAMP = 5;

    // these are the states for messages that have not been updated or not
    private final int UPLOADED = 0;
    private final int NOT_UPLOADED = 1;

    private Retrofit retrofit;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
        setUpRetrofit();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
        setUpRetrofit();
    }

    private void setUpRetrofit(){
        // Define the interceptor, add authentication token headers
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request newRequest = chain.request().newBuilder().
                        addHeader("X-Auth-Token", "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhamliaWdhZCIsImF1ZGllbmNlIjoid2ViIiwiY3JlYXRlZCI6MTQ3MDYyNjE4OTY0MCwiZXhwIjoxNDcxMjMwOTg5fQ.j0zjOWPe2pFy_QgBUfLtJr_RZ-S2FP555zrJGBbYYO6IOnKz5ZB8_sC5Dnb-BusJz2lLTWLjfn5kLtiQlqMQ8w").build();
                return chain.proceed(newRequest);
            }
        };

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(NET_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .writeTimeout(NET_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .connectTimeout(NET_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .addInterceptor(authInterceptor)
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        retrofit = new Retrofit.Builder()
                .baseUrl(MESSAGES_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // check the database for none sent messages
        // if there are, then send
        // else forget
        Uri uri = SmsLoggerContentProvider.CONTENT_URI;
        String selectionCriteria = SmsLoggerContentProvider.KEY_STATE + " = " + NOT_UPLOADED;
        Cursor cursor = mContentResolver.query(uri, PROJECTION, selectionCriteria, null, null);
        List<Message> messages = new ArrayList<>();
        List<Long> messageIds = new ArrayList<>(); // stores a list of the ids of message that would be deleted from the db once the data transfer was successfull
        boolean transferSuccess = false;
        while(cursor != null && cursor.moveToNext()){
           // combine them into a list of messages and then use retrofit to send the data to the server
            // on successful transmission we would then delete all the messages that were sent in the
            long id = cursor.getLong(COLUMN_ID);
            String phoneNumber = cursor.getString(COLUMN_PHONE_NUMBER);
            String sender = cursor.getString(COLUMN_SENDER);
            String body = cursor.getString(COLUMN_BODY);
            long messageTimestamp = cursor.getLong(COLUMN_MESSAGE_TIMESTAMP);
            Message newMessge = new Message(phoneNumber, body, sender, messageTimestamp);
            messages.add(newMessge);
            messageIds.add(id);
        }
        cursor.close();
        if(!messages.isEmpty()){
            MessageApiEndpointInterface apiService = retrofit.create(MessageApiEndpointInterface.class);
            Call<ResponseBody> call =  apiService.sendMessages(messages);
            try {
                Response<ResponseBody> response = call.execute();
                if(response.isSuccessful()){
                    transferSuccess = true;
                    Log.i(TAG, "Message transfer successful");
                }
                else{
                    Log.w(TAG, "Message upload failed");
                    Log.e(TAG, response.message());
                }
            } catch (IOException e) {
                Log.e(TAG, "Message Transfer Failed due to network issues");
                Log.e(TAG, e.getLocalizedMessage());
            }
            if(transferSuccess){
                // delete all the data that were sent
                StringBuilder builder = new StringBuilder();
                builder.append("(");
                for(long id : messageIds){
                    builder.append(id).append(",");
                }
                builder.trimToSize();
                builder.deleteCharAt(builder.length() - 1 ); //to delete the last ,
                builder.append(")");
                String inClause = builder.toString();
                Log.i(TAG, inClause);
                String whereClause = SmsLoggerContentProvider.KEY_ID + " IN " + inClause;
                //TODO make the content provider for messages support transactions to make this operation atomic
                int noDeleted = mContentResolver.delete(SmsLoggerContentProvider.CONTENT_URI, whereClause, null);
                Log.i(TAG, noDeleted + " messages deleted");
            }
        }

    }
}
