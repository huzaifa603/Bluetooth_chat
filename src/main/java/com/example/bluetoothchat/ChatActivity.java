package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private BluetoothService bluetoothService;
    private ChatAdapter chatAdapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText etMessage;
    private TextView tvConnectionStatus;
    private String deviceAddress;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case BluetoothService.MSG_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            tvConnectionStatus.setText("Connected");
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            tvConnectionStatus.setText("Connecting...");
                            break;
                        case BluetoothService.STATE_NONE:
                            tvConnectionStatus.setText("Disconnected");
                            break;
                    }
                    break;
                case BluetoothService.MSG_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    messageList.add(new Message(readMessage, Message.TYPE_RECEIVED));
                    chatAdapter.notifyDataSetChanged();
                    break;
                case BluetoothService.MSG_DEVICE_NAME:
                    String deviceName = msg.getData().getString("device_name");
                    Toast.makeText(ChatActivity.this,
                            "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MSG_TOAST:
                    String toast = msg.getData().getString("toast");
                    Toast.makeText(ChatActivity.this, toast, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        deviceAddress = getIntent().getStringExtra("device_address");
        String deviceName = getIntent().getStringExtra("device_name");

        TextView tvDeviceName = findViewById(R.id.tv_device_name);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        etMessage = findViewById(R.id.et_message);
        Button btnSend = findViewById(R.id.btn_send);
        ListView lvMessages = findViewById(R.id.lv_messages);

        tvDeviceName.setText(deviceName != null ? deviceName : "Unknown Device");

        chatAdapter = new ChatAdapter(this, messageList);
        lvMessages.setAdapter(chatAdapter);

        bluetoothService = new BluetoothService(handler);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        bluetoothService.connect(device);

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                byte[] send = message.getBytes();
                bluetoothService.write(send);
                messageList.add(new Message(message, Message.TYPE_SENT));
                chatAdapter.notifyDataSetChanged();
                etMessage.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) bluetoothService.stop();
    }
}