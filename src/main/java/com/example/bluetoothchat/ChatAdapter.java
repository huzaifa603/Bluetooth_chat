package com.example.bluetoothchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class ChatAdapter extends ArrayAdapter<Message> {

    private Context context;
    private List<Message> messages;

    public ChatAdapter(Context context, List<Message> messages) {
        super(context, 0, messages);
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType() - 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = messages.get(position);
        LayoutInflater inflater = LayoutInflater.from(context);

        if (message.getType() == Message.TYPE_SENT) {
            convertView = inflater.inflate(R.layout.item_message_sent, parent, false);
        } else {
            convertView = inflater.inflate(R.layout.item_message_received, parent, false);
        }

        TextView tvMessage = convertView.findViewById(R.id.tv_message);
        TextView tvTime = convertView.findViewById(R.id.tv_time);

        tvMessage.setText(message.getText());
        tvTime.setText(message.getTime());

        return convertView;
    }
}