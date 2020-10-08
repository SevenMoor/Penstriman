package com.sevenmoor.penstriman;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class PlayVideo extends AppCompatActivity {

    String VIDEO_NAME_TAG = "VIDEO_NAME";
    private VideoView vidView;
    private ProgressBar bar;
    private int seekTime = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayer);
        Intent intent = getIntent();
        String videoName = intent.getStringExtra(VIDEO_NAME_TAG);
        String[] requiredPermissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, requiredPermissions, 0);

        // Get a reference to the VideoView instance as follows, using the id we set in the XML layout.
        vidView = (VideoView)findViewById(R.id.videoView);
        bar = findViewById(R.id.progressStream);

        // Add playback controls.
        MediaController vidControl = new MediaController(this);
        // Set it to use the VideoView instance as its anchor.
        vidControl.setAnchorView(vidView);
        // Set it as the media controller for the VideoView object.
        vidView.setMediaController(vidControl);

        // Prepare the URI for the endpoint.
        Uri vidUri = Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) + "/"+ videoName));
        // Parse the address string as a URI so that we can pass it to the VideoView object.
        vidView.setVideoURI(vidUri);

        bar.setVisibility(View.INVISIBLE);
        // Start playback.
        vidView.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        vidView.start();
        vidView.seekTo(seekTime);
    }

    @Override
    public void onPause() {
        super.onPause();
        seekTime = vidView.getCurrentPosition(); //stopPosition is an int
        vidView.pause();
    }

}
