package com.sevenmoor.penstriman;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class StreamActivity extends AppCompatActivity {

    VideoView video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayer);
        video = findViewById(R.id.videoView);


        Intent intent = getIntent();
        String address = intent.getStringExtra("uuid");

        //TODO
        Toast.makeText(this, address, Toast.LENGTH_LONG).show();
    }

}
