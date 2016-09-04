package com.ajibigad.smslogger.smslogger;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by Julius on 07/08/2016.
 */
public class SmsLoggerApplication extends Application {

    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}
