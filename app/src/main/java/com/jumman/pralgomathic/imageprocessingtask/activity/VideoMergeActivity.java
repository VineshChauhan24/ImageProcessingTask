package com.jumman.pralgomathic.imageprocessingtask.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.jumman.pralgomathic.imageprocessingtask.R;
import com.jumman.pralgomathic.imageprocessingtask.Utilities.Constants;
import com.jumman.pralgomathic.imageprocessingtask.Utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;

public class VideoMergeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "VideoMergeActivity";
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1020;
    private static final int SELECT_VIDEO_ONE = 100;
    private static final int SELECT_VIDEO_TWO = 200;
    private Button selectVideoOneButton, selectVideoTwoButton, mergeVideoButton;
    private VideoView mergeVideoView;
    private ProgressDialog progressDialog;
    private String selectedVideoOnePath = "";
    private String selectedVideoTwoPath = "";
    private String videoOutputDestination = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_merge);
        initUI();
    }

    private void initUI() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Merge Video");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        selectVideoOneButton = (Button) findViewById(R.id.selectVideoOneButton);
        selectVideoOneButton.setOnClickListener(this);
        selectVideoTwoButton = (Button) findViewById(R.id.selectVideoTwoButton);
        selectVideoTwoButton.setOnClickListener(this);
        mergeVideoButton = (Button) findViewById(R.id.mergeVideoButton);
        mergeVideoButton.setOnClickListener(this);
        mergeVideoView = (VideoView) findViewById(R.id.mergeVideoView);
        mergeVideoView.setVisibility(View.INVISIBLE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
    }

    private void selectVideoOne() {
        if (Utils.checkAndRequestPermissions(getApplicationContext(), this)) {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, SELECT_VIDEO_ONE);
        } else {
            Utils.checkAndRequestPermissions(getApplicationContext(), this);
        }
    }

    private void selectVideoTwo() {
        if (Utils.checkAndRequestPermissions(getApplicationContext(), this)) {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, SELECT_VIDEO_TWO);
        } else {
            Utils.checkAndRequestPermissions(getApplicationContext(), this);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.selectVideoOneButton:
                selectVideoOne();
                break;
            case R.id.selectVideoTwoButton:
                selectVideoTwo();
                break;
            case R.id.mergeVideoButton:
                mergeVideo();
                break;
        }
    }

    private void mergeVideo() {
        if (!selectedVideoOnePath.isEmpty() && !selectedVideoTwoPath.isEmpty()) {
            concatVideoCommand();
        } else {
            Toast.makeText(this, "Please select two videos...", Toast.LENGTH_LONG).show();
        }
    }

    private void concatVideoCommand() {
        ArrayList<String> fileList = new ArrayList<String>();
        fileList.add(selectedVideoOnePath);
        fileList.add(selectedVideoTwoPath);

        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder filterComplex = new StringBuilder();
        stringBuilder.append("-y" + ",");
        filterComplex.append("-filter_complex,");
        for (int i = 0; i < fileList.size(); i++) {
            stringBuilder.append("-i" + "," + fileList.get(i) + ",");
            filterComplex.append("[").append(i).append(":v").append(i).append("] [").append(i).append(":a").append(i).append("] ");

        }
        filterComplex.append("concat=n=").append(fileList.size()).append(":v=1:a=1 [v] [a]");
        String[] inputCommand = stringBuilder.toString().split(",");
        String[] filterCommand = filterComplex.toString().split(",");

        videoOutputDestination = Utils.getExternalStoragePath() + "/" + Utils.getOutputFileName("merge_video", Utils.getFileExtension(selectedVideoOnePath));
        String[] destinationCommand = {"-map", "[v]", "-map", "[a]", videoOutputDestination};
        runFFmpegCommand(combine(inputCommand, filterCommand, destinationCommand));
    }

    private void runFFmpegCommand(String[] command) {
        FFmpeg.getInstance(this).execute(command, new ExecuteBinaryResponseHandler() {

            @Override
            public void onStart() {
                progressDialog.setMessage("Processing...");
                progressDialog.show();
            }

            @Override
            public void onSuccess(String message) {
                progressDialog.setMessage("Processing\n" + message);
                Constants.debugLog(TAG,message);
            }

            @Override
            public void onProgress(String message) {
                progressDialog.setMessage("Processing..Please Wait..");
                Constants.debugLog(TAG,message);
            }

            @Override
            public void onFinish() {
                progressDialog.dismiss();
                if (!videoOutputDestination.isEmpty()) {
                    mergeVideoView.setVisibility(View.VISIBLE);
                    playVideo(videoOutputDestination);
                }
            }
        });

    }

    public static String[] combine(String[] arg1, String[] arg2, String[] arg3) {
        String[] result = new String[arg1.length + arg2.length + arg3.length];
        System.arraycopy(arg1, 0, result, 0, arg1.length);
        System.arraycopy(arg2, 0, result, arg1.length, arg2.length);
        System.arraycopy(arg3, 0, result, arg1.length + arg2.length, arg3.length);
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_VIDEO_ONE) {
                selectedVideoOnePath = getVideoPath(data.getData());
                if (selectedVideoOnePath == null) {
                    Log.d(TAG, "selected video path = null!");

                } else {
                    Toast.makeText(this, selectedVideoOnePath, Toast.LENGTH_LONG).show();
                    Constants.debugLog(TAG, selectedVideoOnePath);

                }
            } else if (requestCode == SELECT_VIDEO_TWO) {
                selectedVideoTwoPath = getVideoPath(data.getData());
                if (selectedVideoTwoPath == null) {
                    Constants.errorLog(TAG, "selected video path = null!");

                } else {
                    Toast.makeText(this, selectedVideoTwoPath, Toast.LENGTH_LONG).show();
                    Constants.debugLog(TAG, selectedVideoTwoPath);

                }
            }
        }

    }

    private String getVideoPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else return null;
    }

    public void playVideo(String videoPath) {
        MediaController mediaController = new MediaController(this);
        mergeVideoView.setMediaController(mediaController);
        mergeVideoView.setVideoPath(videoPath);
        mergeVideoView.start();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
       Constants.debugLog(TAG, "Permission callback called-------");
        switch (requestCode) {
            case Constants.REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            ) {
                        Constants.debugLog(TAG, "read & write permission granted");
                        Toast.makeText(this, "read & write permission granted", Toast.LENGTH_LONG).show();

                    } else {
                        Constants.errorLog(TAG, "Some permissions are not granted ask again ");
                    }
                }
            }
        }

    }

}
