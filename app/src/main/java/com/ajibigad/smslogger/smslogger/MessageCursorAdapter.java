package com.ajibigad.smslogger.smslogger;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ajibigad.smslogger.smslogger.provider.SmsLoggerContentProvider;

/**
 * Created by Julius on 06/08/2016.
 */
public class MessageCursorAdapter extends CursorAdapter {

    private final int MAX_SUMMARY =50;

    public MessageCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView messageSender = (TextView) view.findViewById(R.id.message_sender);
        TextView messageSummary = (TextView) view.findViewById(R.id.message_summary);
        TextView messageTimeReceived = (TextView) view.findViewById(R.id.timeReceived);

        // Extract properties from cursor
        String sender = cursor.getString(cursor.getColumnIndexOrThrow(SmsLoggerContentProvider.KEY_SENDER));
        String body = cursor.getString(cursor.getColumnIndexOrThrow(SmsLoggerContentProvider.KEY_BODY));
        //multiply by 1000 to convert from seconds to millseconds
        long timeReceived = cursor.getInt(cursor.getColumnIndexOrThrow(SmsLoggerContentProvider.KEY_MESSAGE_TIMESTAMP)) * 1000;

        // Populate fields with extracted properties
        messageSender.setText(sender);
        messageSummary.setText(generateMessageSummary(body));
        messageTimeReceived.setText(DateUtils.formatDateTime(context, timeReceived, DateUtils.FORMAT_NUMERIC_DATE));
    }

    private String generateMessageSummary(String message){
        if(message.length() < MAX_SUMMARY)
            return message;
        else
            return message.substring(0, MAX_SUMMARY).concat("....");
    }
}
