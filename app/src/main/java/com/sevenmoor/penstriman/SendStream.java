package com.sevenmoor.penstriman;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;

public class SendStream extends AppCompatActivity {

    ProgressBar progressBar;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static String CLASSNAME = "BOOOM";
    private UUID MY_UUID = UUID.fromString("542a3b22-d3f9-476c-ad5b-49bc19f24b1a");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sendstream);
        progressBar = findViewById(R.id.progressBar);

        IntentFilter receiverIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, receiverIntent);
        IntentFilter filterDiscover = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        filterDiscover.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filterDiscover.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filterDiscover.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(blutoothDiscovery, filterDiscover);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.i(CLASSNAME,"Bluetooth enabled");
        } else if (!bluetoothAdapter.isEnabled()) {

            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Log.i(CLASSNAME,"Bluetooth disabled");

        }
        discovery();

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(CLASSNAME, "State - Off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(CLASSNAME, "Turning Off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        discovery();
                        Log.i(CLASSNAME, "On");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i(CLASSNAME, "Turning On");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver blutoothDiscovery = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch(mode){
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.i(CLASSNAME, "SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                        AcceptThread thread = new AcceptThread();
                        thread.start();
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.i(CLASSNAME, "SCAN_MODE_CONNECTABLE");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.i(CLASSNAME, "SCAN_MODE_NONE");
                        break;
                }
            }
        }
    };

    private void discovery(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket temporary = null;
            try {
                String APP_NAME = "PENSTRIMAN";
                temporary = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(CLASSNAME, "Socket's listen() failed", e);
            }
            bluetoothServerSocket = temporary;
        }

        public void run() {
            BluetoothSocket bluetoothSocket;
            while (true) {
                Log.i(CLASSNAME, "Thread running");
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                    Log.i(CLASSNAME, "Accept Connection");
                    //Log.i(CLASSNAME, "ThreadAccept run() - accept");
                } catch (IOException e) {
                    Log.e(CLASSNAME, "Socket's accept() failed", e);
                    break;
                }

                if (bluetoothSocket != null) {
                    Log.i(CLASSNAME, "Connection accepted (Run())");
                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.e(CLASSNAME, "Cannot close socket", e);
            }
        }
    }

}
