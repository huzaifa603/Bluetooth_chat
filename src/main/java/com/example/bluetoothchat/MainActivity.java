package com.example.bluetoothchat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DEVICE = 2;
    private static final int REQUEST_PERMISSIONS = 3;

    private BluetoothAdapter bluetoothAdapter;
    private TextView tvStatus;
    private Switch switchBluetooth;
    private ArrayAdapter<String> pairedAdapter;
    private ArrayList<String> pairedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        tvStatus = findViewById(R.id.tv_status);
        switchBluetooth = findViewById(R.id.switch_bluetooth);
        Button btnScan = findViewById(R.id.btn_scan);
        ListView lvPairedDevices = findViewById(R.id.lv_paired_devices);

        pairedAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, pairedList);
        lvPairedDevices.setAdapter(pairedAdapter);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Permissions check karo
        requestPermissions();

        // Bluetooth status
        if (bluetoothAdapter.isEnabled()) {
            switchBluetooth.setChecked(true);
            tvStatus.setText("Bluetooth ON");
            loadPairedDevices();
        } else {
            switchBluetooth.setChecked(false);
            tvStatus.setText("Bluetooth OFF");
        }

        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                tvStatus.setText("Bluetooth ON");
                loadPairedDevices();
            } else {
                bluetoothAdapter.disable();
                tvStatus.setText("Bluetooth OFF");
                pairedList.clear();
                pairedAdapter.notifyDataSetChanged();
            }
        });

        btnScan.setOnClickListener(v -> {
            if (bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_DEVICE);
            } else {
                Toast.makeText(this, "Please turn on Bluetooth first!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        lvPairedDevices.setOnItemClickListener((parent, view, position, id) -> {
            String info = pairedList.get(position);
            String address = info.substring(info.length() - 17);
            String name = info.substring(0, info.indexOf("\n"));
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("device_address", address);
            intent.putExtra("device_name", name);
            startActivity(intent);
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_ADVERTISE,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, REQUEST_PERMISSIONS);
            }
        } else {
            // Android 11 aur pehle
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }, REQUEST_PERMISSIONS);
            }
        }
    }

    private void loadPairedDevices() {
        pairedList.clear();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedList.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedList.add("No paired devices found");
        }
        pairedAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!",
                        Toast.LENGTH_SHORT).show();
                loadPairedDevices();
            } else {
                Toast.makeText(this, "Permissions denied! App may not work.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                tvStatus.setText("Bluetooth ON");
                loadPairedDevices();
            } else {
                switchBluetooth.setChecked(false);
                tvStatus.setText("Bluetooth OFF");
            }
        } else if (requestCode == REQUEST_DEVICE) {
            if (resultCode == RESULT_OK) {
                String address = data.getStringExtra(
                        DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("device_address", address);
                intent.putExtra("device_name", device.getName());
                startActivity(intent);
            }
        }
    }
}