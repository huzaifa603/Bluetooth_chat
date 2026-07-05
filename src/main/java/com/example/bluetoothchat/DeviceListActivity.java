package com.example.bluetoothchat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> availableDevicesAdapter;
    private ArrayList<String> pairedDevicesList = new ArrayList<>();
    private ArrayList<String> availableDevicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ListView lvPaired = findViewById(R.id.lv_paired);
        ListView lvAvailable = findViewById(R.id.lv_available);

        pairedDevicesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, pairedDevicesList);
        availableDevicesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, availableDevicesList);

        lvPaired.setAdapter(pairedDevicesAdapter);
        lvAvailable.setAdapter(availableDevicesAdapter);

        // Paired devices load karo
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if (name == null || name.isEmpty()) {
                    name = "Device-" + device.getAddress()
                            .substring(device.getAddress().length() - 5)
                            .replace(":", "");
                }
                pairedDevicesList.add(name + "\n" + device.getAddress());
            }
            pairedDevicesAdapter.notifyDataSetChanged();
        } else {
            pairedDevicesList.add("No paired devices");
            pairedDevicesAdapter.notifyDataSetChanged();
        }

        // Click listeners
        lvPaired.setOnItemClickListener((parent, view, position, id) -> {
            String info = ((TextView) view).getText().toString();
            if (!info.equals("No paired devices")) {
                String address = info.substring(info.length() - 17);
                bluetoothAdapter.cancelDiscovery();
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        lvAvailable.setOnItemClickListener((parent, view, position, id) -> {
            String info = ((TextView) view).getText().toString();
            if (!info.equals("No devices found")) {
                String address = info.substring(info.length() - 17);
                bluetoothAdapter.cancelDiscovery();
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        // Broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        // Scan shuru karo
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.startDiscovery();
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);

                if (device != null &&
                        device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    // Pehle EXTRA_NAME se naam lo
                    String deviceName = intent.getStringExtra(
                            BluetoothDevice.EXTRA_NAME);

                    // Agar null hai toh device.getName() try karo
                    if (deviceName == null || deviceName.isEmpty()) {
                        try {
                            deviceName = device.getName();
                        } catch (Exception e) {
                            deviceName = null;
                        }
                    }

                    // Agar phir bhi null hai toh address se naam banao
                    if (deviceName == null || deviceName.isEmpty()
                            || deviceName.equals("null")) {
                        String address = device.getAddress();
                        deviceName = "Device-" + address
                                .substring(address.length() - 5)
                                .replace(":", "");
                    }

                    String address = device.getAddress();
                    String entry = deviceName + "\n" + address;

                    // Duplicate check karo
                    if (!availableDevicesList.contains(entry)) {
                        availableDevicesList.add(entry);
                        availableDevicesAdapter.notifyDataSetChanged();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (availableDevicesList.isEmpty()) {
                    availableDevicesList.add("No devices found");
                    availableDevicesAdapter.notifyDataSetChanged();
                }
                Toast.makeText(context, "Scan complete!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(receiver);
    }
}