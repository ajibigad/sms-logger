package com.ajibigad.smslogger.smslogger;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class IncomingSmsReceiver extends BroadcastReceiver {

    public IncomingSmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Intent logMessageService = new Intent(context, LogMessageService.class);
        logMessageService.putExtra("pdus", intent.getExtras());
        context.startService(logMessageService);
    }

}
