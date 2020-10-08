package com.sevenmoor.penstriman;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class DiscoverActivity extends AppCompatActivity {

    ListView scrollView;
    BluetoothAdapter adapter;
    FloatingActionButton refresh;

    private ArrayList<String> bluetoothList;
    private ArrayAdapter<String> arrayAdapter;
    private HashMap<String,String> addressBook;


    BroadcastReceiver discoveryMonitor = new BroadcastReceiver() {
        String dStarted = BluetoothAdapter.ACTION_DISCOVERY_STARTED;
        String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (dStarted.equals(intent.getAction())) {
                Toast.makeText(getApplicationContext(), "Discovery Started. . .", Toast.LENGTH_SHORT).show();
                bluetoothList.clear();
                addressBook.clear();
                arrayAdapter.notifyDataSetChanged();
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
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            if(deviceName != null) {
                if(!bluetoothList.contains(deviceName)) {
                    bluetoothList.add(deviceName);
                    addressBook.put(deviceName,deviceAddress);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.discover);

        scrollView = findViewById(R.id.bluetooth);
        adapter = BluetoothAdapter.getDefaultAdapter();
        refresh = findViewById(R.id.refreshButton);

        bluetoothList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, bluetoothList);
        scrollView.setAdapter(arrayAdapter);
        addressBook = new HashMap<String,String>();

        String enableRequest = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        startActivityForResult(new Intent(enableRequest), 0);

        if (!adapter.isEnabled()) {
            String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
            String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
            registerReceiver(adapterState, new IntentFilter(actionStateChanged));
            if(checkSelfPermission(Manifest.permission.BLUETOOTH)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN},0);
            }
            startActivityForResult(new Intent(actionRequestEnable), 0);
        }

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(adapter.isDiscovering()) {
                    adapter.cancelDiscovery();
                }
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

        scrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                final String selected = addressBook.get(((TextView) view).getText().toString());
                Intent intent = new Intent(getApplicationContext(),StreamActivity.class);
                intent.putExtra("uuid",selected);
                startActivity(intent);
            }
        });

        if(adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        ActivityCompat.requestPermissions(DiscoverActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},2);
        if (adapter!=null){
            boolean result = adapter.startDiscovery();
            Log.i("Info", "adapter state int="+adapter.getState());
            Log.i("Info", "Discovering="+result);
        }
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(discoveryMonitor);
        unregisterReceiver(adapterState);
        unregisterReceiver(discoveryResult);
        super.onDestroy();
    }
}