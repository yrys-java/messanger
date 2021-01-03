package com.example.messengerapp.models;

import android.text.format.Time;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Message {

    private String text;
    private String sender;
    private String recipient;


    public Message() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setSender(String sender) {
        this.sender = sender;

    }
}
