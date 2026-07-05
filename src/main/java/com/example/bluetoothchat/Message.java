package com.example.bluetoothchat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {

    public static final int TYPE_SENT = 1;
    public static final int TYPE_RECEIVED = 2;

    private String text;
    private int type;
    private String time;

    public Message(String text, int type) {
        this.text = text;
        this.type = type;
        this.time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date());
    }

    public String getText() { return text; }
    public int getType() { return type; }
    public String getTime() { return time; }
}