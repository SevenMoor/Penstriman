package com.sevenmoor.penstriman;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    String VIDEO_NAME_TAG = "VIDEO_NAME";
    private ListView listView;
    private ArrayList<String> videoList;
    private ArrayAdapter<String> adapter;
    private SaveRestore saveRestore;
    private BluetoothAdapter bluetoothAdapter;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);
        SearchView searchView = findViewById(R.id.searchView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),DiscoverActivity.class);
                startActivity(intent);
            }
        });

        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            startActivity(discoverableIntent);
        }

        listView = findViewById(R.id.listview);
        videoList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, videoList);
        listView.setAdapter(adapter);
        if(savedInstanceState!=null){
            videoList = savedInstanceState.getStringArrayList(VIDEO_NAME_TAG);
        }
        sharedPreferences = getSharedPreferences(VIDEO_NAME_TAG,MODE_PRIVATE);
        saveRestore = new SaveRestore(videoList, sharedPreferences);

        IntentFilter filter = new IntentFilter(
                "android.bluetooth.device.action.PAIRING_REQUEST");
        registerReceiver(mReceiver, filter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final String selected = ((TextView) view).getText().toString();
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Watch or stream the video ?");
                alertDialog.setMessage("Do you want to share or watch the video");
                alertDialog.setPositiveButton("Watch", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, PlayVideo.class);
                        intent.putExtra(VIDEO_NAME_TAG, selected);
                        startActivity(intent);
                    }
                });

                alertDialog.setNegativeButton("Stream", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 5000);
                            startActivity(discoverableIntent);
                        }
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.putExtra(VIDEO_NAME_TAG, selected);
                        startActivity(intent);
                    }
                });
                alertDialog.show();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    if(query.isEmpty()) Toast.makeText(MainActivity.this, "Please type an URL", Toast.LENGTH_LONG).show();
                    startDownloading(query);
                } catch (URISyntaxException e) {
                    Toast.makeText(MainActivity.this, "Non-valid URL, please check again", Toast.LENGTH_LONG).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private void startDownloading(String url) throws URISyntaxException {
        Uri uri = Uri.parse(url);
        String videoName = Paths.get(new URI(url).getPath()).getFileName().toString();
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setTitle("Download");
        request.setDescription("Video downloading");
        DownloadManager.Query query = null;
        Cursor cursor = null;
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS+"/", videoName);
        query = new DownloadManager.Query();
        query.setFilterByStatus(DownloadManager.STATUS_FAILED|DownloadManager.STATUS_PAUSED|DownloadManager.STATUS_SUCCESSFUL|
                DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_PENDING);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        cursor = manager.query(query);
        if(cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    break;
                case DownloadManager.STATUS_PENDING:
                    break;
                case DownloadManager.STATUS_RUNNING:
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    break;
                case DownloadManager.STATUS_FAILED:
                    break;
            }
        }
        Toast.makeText(this, statusMessage(cursor), Toast.LENGTH_LONG).show();
        manager.enqueue(request);
        videoList.add(videoName);
    }

    private void refreshList(){
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putStringArrayList(VIDEO_NAME_TAG, videoList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        videoList = savedState.getStringArrayList(VIDEO_NAME_TAG);
        refreshList();
    }

    public void onDestroy(){
        super.onDestroy();
        /*unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);*/

        saveRestore.save();
    }

    BroadcastReceiver onComplete=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshList();
        };
    };

    BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MainActivity.this, "Please wait until the end", Toast.LENGTH_LONG).show();
        };
    };

    private String statusMessage(Cursor c) {
        String msg="";
        switch(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg="Download failed!";
                break;
            case DownloadManager.STATUS_PAUSED:
                msg="Download paused!";
                break;
            case DownloadManager.STATUS_PENDING:
                msg="Download pending!";
                break;
            case DownloadManager.STATUS_RUNNING:
                msg="Download in progress!";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                msg="Download complete!";
                break;
            default:
                msg="Download is nowhere in sight";
                break;
        }
        return(msg);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
                    Log.d("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY",0));
                    Log.d("Bonded", device.getName());
                    Toast.makeText(MainActivity.this, "Bonded "+device.getName(), Toast.LENGTH_LONG).show();

                    byte[] pinBytes;
                    pinBytes = (""+pin).getBytes(StandardCharsets.UTF_8);
                    device.setPin(pinBytes);
                    device.setPairingConfirmation(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (adapter==null) {
            adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, videoList);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }
}