package com.sevenmoor.penstriman;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class DiscoverActivity extends AppCompatActivity {

    private ListView listView;
    private BluetoothAdapter adapter;
    private FloatingActionButton refresh;
    private ArrayList<String> bluetoothList;
    private ArrayAdapter<String> arrayAdapter;

    BroadcastReceiver discoveryMonitor = new BroadcastReceiver() {
        String dStarted = BluetoothAdapter.ACTION_DISCOVERY_STARTED;
        String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (dStarted.equals(intent.getAction())) {
                Toast.makeText(getApplicationContext(), "Discovery Started. . .", Toast.LENGTH_SHORT).show();
            } else if (dFinished.equals(intent.getAction())) {
                Toast.makeText(getApplicationContext(), "Discovery Completed. . .", Toast.LENGTH_SHORT).show();
            }
        }
    };

    BroadcastReceiver adapterState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stateExtra = BluetoothAdapter.EXTRA_STATE;
            int state = intent.getIntExtra(stateExtra, -1);
            String tt = "";
            switch (state) {
                case (BluetoothAdapter.STATE_TURNING_ON): {
                    tt = "Bluetooth turning on";
                    break;
                }
                case (BluetoothAdapter.STATE_ON): {
                    tt = "Bluetooth on";
                    unregisterReceiver(this);
                    break;
                }
                case (BluetoothAdapter.STATE_TURNING_OFF): {
                    tt = "Bluetooth turning off";
                    break;
                }
                case (BluetoothAdapter.STATE_OFF): {
                    tt = "Bluetooth off";
                    break;
                }
                default:
                    break;
            }
            Toast.makeText(getApplicationContext(), tt, Toast.LENGTH_SHORT).show();

        }
    };

    BroadcastReceiver discoveryResult = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceAddr = device.getAddress();
            if(deviceName != null) {
                bluetoothList.add("Device : " + deviceName+" MAC add : "+deviceAddr);
                Toast.makeText(DiscoverActivity.this, deviceName, Toast.LENGTH_SHORT).show();
                arrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.discover);


        adapter = BluetoothAdapter.getDefaultAdapter();
        refresh = findViewById(R.id.refreshButton);
        listView = findViewById(R.id.bluetooth);
        bluetoothList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, bluetoothList);
        listView.setAdapter(arrayAdapter);

        String enableRequest = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        startActivityForResult(new Intent(enableRequest), 0);



        if (!adapter.isEnabled()) {
            String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
            String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
            registerReceiver(adapterState, new IntentFilter(actionStateChanged));
            startActivityForResult(new Intent(actionRequestEnable), 0);
        }

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(DiscoverActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},2);
                if (adapter!=null){
                    boolean result = adapter.startDiscovery();
                    Log.i("Info", "adapter state int="+adapter.getState());
                    Log.i("Info", "Discovering="+result);
                }


            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryMonitor,filter);
        registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }
}