package com.ajibigad.smslogger.smslogger.models;

/**
 * Created by Julius on 07/08/2016.
 */
public class Message {

    String phoneNumber;
    String body;
    String sender;
    long messageTimestamp;

    public Message(String phoneNumber, String body, String sender, long messageTimestamp){
        this.phoneNumber = phoneNumber;
        this.body = body;
        this.sender = sender;
        this.messageTimestamp = messageTimestamp;
    }

}
