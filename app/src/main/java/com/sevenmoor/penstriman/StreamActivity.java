package com.sevenmoor.penstriman;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class StreamActivity extends AppCompatActivity {

    private FileOutputStream fileOutputStream;
    private VideoView video;
    //private OutputStreamWriter writer;
    private File file;
    private static final String TAG = "PES";
    private Handler handler;
    private interface MessageConstants{
        public static final int MESSAGE_READ = 0;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayer);
        video = findViewById(R.id.videoView);

        ActivityCompat.requestPermissions(StreamActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
        File outputDir = StreamActivity.this.getCacheDir();
        file = null;
        try {
            file = File.createTempFile("penstriman_", ".mp4", outputDir);
            if (!file.exists()) {
                boolean res = file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file,true);
            //writer = new OutputStreamWriter(fileOutputStream);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        handler = new Handler(){
          @Override
          public void handleMessage(android.os.Message msg) {
              super.handleMessage(msg);
              if (msg.what == StreamActivity.MessageConstants.MESSAGE_READ){
                  byte[] text = (byte[]) msg.obj;
                  String str = new String(text);
                  if(!str.equals("EOF")){
                      try {
                          Log.i(TAG, "bytes: "+Arrays.toString(text));
                          fileOutputStream.write(text);
                      }
                      catch (IOException e){
                          e.printStackTrace();
                      }
                  }
                  else{
                      try {
                          Uri vidUri = Uri.fromFile(new File(file.getPath()));
                          video.setVideoURI(vidUri);
                          //writer.close();
                          fileOutputStream.close();
                          video.start();
                      }
                      catch (IOException e){
                          e.printStackTrace();
                      }
                  }
              }
          }
        };

        video.setKeepScreenOn(true);

        //Media Controller preparation
        MediaController vidControl = new MediaController(this);
        vidControl.setAnchorView(video);
        video.setMediaController(vidControl);

        Intent intent = getIntent();
        String address = intent.getStringExtra("uuid");

        ConnectThread co = new ConnectThread(address);
        co.start();
    }



    //CONNECTED THREAD DEFINITION
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream input;

        public ConnectedThread(BluetoothSocket socket){
            this.socket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            }
            catch (IOException e){
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            input = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;
            int size = 0;

            while (true){
                try{
                    numBytes = input.read(buffer);
                    if(numBytes>0){
                        Log.i(TAG, "Caught bytes");
                        Message readMsg = handler.obtainMessage(StreamActivity.MessageConstants.MESSAGE_READ, numBytes, -1, Arrays.copyOf(buffer,numBytes));
                        readMsg.sendToTarget();
                        size += numBytes;
                    }
                    else sleep(50);
                }
                catch (IOException | InterruptedException e){
                    Log.i(TAG, "FORCED EOF");
                    Message readMsg = handler.obtainMessage(StreamActivity.MessageConstants.MESSAGE_READ, 3, -1, "EOF".getBytes());
                    readMsg.sendToTarget();
                    Log.i(TAG, "Size="+size);
                    return;
                }
            }
        }

        public void cancel(){
            try {
                socket.close();
            }
            catch (IOException e){
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }



    //CONNECT THREAD DEFINITION
    public class ConnectThread extends Thread {

        BluetoothSocket sock = null;
        BluetoothDevice device;
        BluetoothAdapter adapter;

        public ConnectThread(String address){
            BluetoothSocket tmp = null;
            adapter = BluetoothAdapter.getDefaultAdapter();
            device = adapter.getRemoteDevice(address);
            try{
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("542a3b22-d3f9-476c-ad5b-49bc19f24b1a"));
            }
            catch(IOException e){
                Log.d(TAG, "Could not create connection", e);
            }
            sock = tmp;
        }

        public void run(){
            adapter.cancelDiscovery();

            try{
                sock.connect();
                Log.i(TAG, "Established connection");
            }
            catch (IOException connect){
                try{
                    sock.close();
                }
                catch(IOException close){
                    Log.d(TAG, "Could not close connection", close);
                }
                return;
            }
            ConnectedThread connection = new ConnectedThread(sock);
            connection.start();
        }

        public void cancel(){
            try{
                sock.close();
            }
            catch(IOException close){
                Log.d(TAG, "Could not close connection", close);
            }
        }

        public BluetoothSocket getSock(){
            return sock;
        }
    }
}
