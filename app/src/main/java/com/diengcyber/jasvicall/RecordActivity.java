package com.diengcyber.jasvicall;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;


/**
 * Created by HBiSoft on 13 Aug 2019
 * Copyright (c) 2019 . All rights reserved.
 */

/*
* Implementation Steps
*
* 1. Implement HBRecorderListener by calling implements HBRecorderListener
*    After this you have to implement the methods by pressing (Alt + Enter)
*
* 2. Declare HBRecorder
*
* 3. Init implements HBRecorderListener by calling hbRecorder = new HBRecorder(this, this);
*
* 4. Set adjust provided settings
*
* 5. Start recording by first calling:
* MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
  Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
  startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);

* 6. Then in onActivityResult call hbRecorder.onActivityResult(resultCode, data, this);
*
* 7. Then you can start recording by calling hbRecorder.startScreenRecording(data);
*
* */

@SuppressWarnings({"deprecation", "SameParameterValue"})
public class RecordActivity extends AppCompatActivity implements HBRecorderListener {
    //Permissions
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private boolean hasPermissions = false;

    //Declare HBRecorder
    private HBRecorder hbRecorder;

    //Start/Stop Button
    private Button startbtn;

    //HD/SD quality
    private RadioGroup radioGroup;

    //Should record/show audio/notification
    private CheckBox recordAudioCheckBox;

    //Reference to checkboxes and radio buttons
    boolean wasHDSelected = true;
    boolean isAudioEnabled = true;

    //Should custom settings be used
    Switch custom_settings_switch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        initViews();
        setOnClickListeners();
        setRadioGroupCheckListener();
        setRecordAudioCheckBoxListener();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Init HBRecorder
            hbRecorder = new HBRecorder(this, this);

            //When the user returns to the application, some UI changes might be necessary,
            //check if recording is in progress and make changes accordingly
            if (hbRecorder.isBusyRecording()) {
                startbtn.setText(R.string.stop_recording);
            }
        }

        // Examples of how to use the HBRecorderCodecInfo class to get codec info
        HBRecorderCodecInfo hbRecorderCodecInfo = new HBRecorderCodecInfo();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int mWidth = hbRecorder.getDefaultWidth();
            int mHeight = hbRecorder.getDefaultHeight();
            String mMimeType = "video/avc";
            int mFPS = 30;
            if (hbRecorderCodecInfo.isMimeTypeSupported(mMimeType)) {
                String defaultVideoEncoder = hbRecorderCodecInfo.getDefaultVideoEncoderName(mMimeType);
                boolean isSizeAndFramerateSupported = hbRecorderCodecInfo.isSizeAndFramerateSupported(mWidth, mHeight, mFPS, mMimeType, ORIENTATION_PORTRAIT);
                Log.e("EXAMPLE", "THIS IS AN EXAMPLE OF HOW TO USE THE (HBRecorderCodecInfo) TO GET CODEC INFO:");
                Log.e("HBRecorderCodecInfo", "defaultVideoEncoder for (" + mMimeType + ") -> " + defaultVideoEncoder);
                Log.e("HBRecorderCodecInfo", "MaxSupportedFrameRate -> " + hbRecorderCodecInfo.getMaxSupportedFrameRate(mWidth, mHeight, mMimeType));
                Log.e("HBRecorderCodecInfo", "MaxSupportedBitrate -> " + hbRecorderCodecInfo.getMaxSupportedBitrate(mMimeType));
                Log.e("HBRecorderCodecInfo", "isSizeAndFramerateSupported @ Width = "+mWidth+" Height = "+mHeight+" FPS = "+mFPS+" -> " + isSizeAndFramerateSupported);
                Log.e("HBRecorderCodecInfo", "isSizeSupported @ Width = "+mWidth+" Height = "+mHeight+" -> " + hbRecorderCodecInfo.isSizeSupported(mWidth, mHeight, mMimeType));
                Log.e("HBRecorderCodecInfo", "Default Video Format = " + hbRecorderCodecInfo.getDefaultVideoFormat());

                HashMap<String, String> supportedVideoMimeTypes = hbRecorderCodecInfo.getSupportedVideoMimeTypes();
                for (Map.Entry<String, String> entry : supportedVideoMimeTypes.entrySet()) {
                    Log.e("HBRecorderCodecInfo", "Supported VIDEO encoders and mime types : " + entry.getKey() + " -> " + entry.getValue());
                }

                HashMap<String, String> supportedAudioMimeTypes = hbRecorderCodecInfo.getSupportedAudioMimeTypes();
                for (Map.Entry<String, String> entry : supportedAudioMimeTypes.entrySet()) {
                    Log.e("HBRecorderCodecInfo", "Supported AUDIO encoders and mime types : " + entry.getKey() + " -> " + entry.getValue());
                }

                ArrayList<String> supportedVideoFormats = hbRecorderCodecInfo.getSupportedVideoFormats();
                for (int j = 0; j < supportedVideoFormats.size(); j++) {
                    Log.e("HBRecorderCodecInfo", "Available Video Formats : " + supportedVideoFormats.get(j));
                }
            }else{
                Log.e("HBRecorderCodecInfo", "MimeType not supported");
            }

        }

    }

    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }

    //Init Views
    private void initViews() {
        startbtn = findViewById(R.id.button_start);
        radioGroup = findViewById(R.id.radio_group);
        recordAudioCheckBox = findViewById(R.id.audio_check_box);
        custom_settings_switch = findViewById(R.id.custom_settings_switch);
    }

    //Start Button OnClickListener
    private void setOnClickListeners() {
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //first check if permissions was granted
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
                        hasPermissions = true;
                    }
                    if (hasPermissions) {
                        //check if recording is in progress
                        //and stop it if it is
                        if (hbRecorder.isBusyRecording()) {
                            hbRecorder.stopScreenRecording();
                            startbtn.setText(R.string.start_recording);
                        }
                        //else start recording
                        else {
                            startRecordingScreen();
                        }
                    }
                } else {
                    showLongToast("This library requires API 21>");
                }
            }
        });
    }

    //Check if HD/SD Video should be recorded
    private void setRadioGroupCheckListener() {
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.hd_button:
                        //Ser HBRecorder to HD
                        wasHDSelected = true;

                        break;
                    case R.id.sd_button:
                        //Ser HBRecorder to SD
                        wasHDSelected = false;
                        break;
                }
            }
        });
    }

    //Check if audio should be recorded
    private void setRecordAudioCheckBoxListener() {
        recordAudioCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Enable/Disable audio
                isAudioEnabled = isChecked;
            }
        });
    }

    @Override
    public void HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called");
    }

    //Listener for when the recording is saved successfully
    //This will be called after the file was created
    @Override
    public void HBRecorderOnComplete() {
        startbtn.setText(R.string.start_recording);
        showLongToast("Saved Successfully");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Update gallery depending on SDK Level
            if (hbRecorder.wasUriSet()) {
                updateGalleryUri();
            }else{
                refreshGalleryFile();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    private void updateGalleryUri(){
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        // Error 38 happens when
        // - the selected video encoder is not supported
        // - the output format is not supported
        // - if another app is using the microphone

        //It is best to use device default

        if (errorCode == 38) {
            showLongToast("Some settings are not supported by your device");
        } else {
            showLongToast("HBRecorderOnError - See Log");
            Log.e("HBRecorderOnError", reason);
        }

        startbtn.setText(R.string.start_recording);

    }

    //Start recording screen
    //It is important to call it like this
    //hbRecorder.startScreenRecording(data); should only be called in onActivityResult
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRecordingScreen() {
        if (custom_settings_switch.isChecked()) {
            //WHEN SETTING CUSTOM SETTINGS YOU MUST SET THIS!!!
            hbRecorder.enableCustomSettings();
            customSettings();
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
            startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            startbtn.setText(R.string.stop_recording);
        } else {
            quickSettings();
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
            startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            startbtn.setText(R.string.stop_recording);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void customSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Is audio enabled
        boolean audio_enabled = prefs.getBoolean("key_record_audio", true);
        hbRecorder.isAudioEnabled(audio_enabled);

        //Audio Source
        String audio_source = prefs.getString("key_audio_source", null);
        if (audio_source != null) {
            switch (audio_source) {
                case "0":
                    hbRecorder.setAudioSource("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setAudioSource("CAMCODER");
                    break;
                case "2":
                    hbRecorder.setAudioSource("MIC");
                    break;
            }
        }

        //Video Encoder
        String video_encoder = prefs.getString("key_video_encoder", null);
        if (video_encoder != null) {
            switch (video_encoder) {
                case "0":
                    hbRecorder.setVideoEncoder("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setVideoEncoder("H264");
                    break;
                case "2":
                    hbRecorder.setVideoEncoder("H263");
                    break;
                case "3":
                    hbRecorder.setVideoEncoder("HEVC");
                    break;
                case "4":
                    hbRecorder.setVideoEncoder("MPEG_4_SP");
                    break;
                case "5":
                    hbRecorder.setVideoEncoder("VP8");
                    break;
            }
        }

        //NOTE - THIS MIGHT NOT BE SUPPORTED SIZES FOR YOUR DEVICE
        //Video Dimensions
        String video_resolution = prefs.getString("key_video_resolution", null);
        if (video_resolution != null) {
            switch (video_resolution) {
                case "0":
                    hbRecorder.setScreenDimensions(426, 240);
                    break;
                case "1":
                    hbRecorder.setScreenDimensions(640, 360);
                    break;
                case "2":
                    hbRecorder.setScreenDimensions(854, 480);
                    break;
                case "3":
                    hbRecorder.setScreenDimensions(1280, 720);
                    break;
                case "4":
                    hbRecorder.setScreenDimensions(1920, 1080);
                    break;
            }
        }

        //Video Frame Rate
        String video_frame_rate = prefs.getString("key_video_fps", null);
        if (video_frame_rate != null) {
            switch (video_frame_rate) {
                case "0":
                    hbRecorder.setVideoFrameRate(60);
                    break;
                case "1":
                    hbRecorder.setVideoFrameRate(50);
                    break;
                case "2":
                    hbRecorder.setVideoFrameRate(48);
                    break;
                case "3":
                    hbRecorder.setVideoFrameRate(30);
                    break;
                case "4":
                    hbRecorder.setVideoFrameRate(25);
                    break;
                case "5":
                    hbRecorder.setVideoFrameRate(24);
                    break;
            }
        }

        //Video Bitrate
        String video_bit_rate = prefs.getString("key_video_bitrate", null);
        if (video_bit_rate != null) {
            switch (video_bit_rate) {
                case "1":
                    hbRecorder.setVideoBitrate(12000000);
                    break;
                case "2":
                    hbRecorder.setVideoBitrate(8000000);
                    break;
                case "3":
                    hbRecorder.setVideoBitrate(7500000);
                    break;
                case "4":
                    hbRecorder.setVideoBitrate(5000000);
                    break;
                case "5":
                    hbRecorder.setVideoBitrate(4000000);
                    break;
                case "6":
                    hbRecorder.setVideoBitrate(2500000);
                    break;
                case "7":
                    hbRecorder.setVideoBitrate(1500000);
                    break;
                case "8":
                    hbRecorder.setVideoBitrate(1000000);
                    break;
            }
        }

        //Output Format
        String output_format = prefs.getString("key_output_format", null);
        if (output_format != null) {
            switch (output_format) {
                case "0":
                    hbRecorder.setOutputFormat("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setOutputFormat("MPEG_4");
                    break;
                case "2":
                    hbRecorder.setOutputFormat("THREE_GPP");
                    break;
                case "3":
                    hbRecorder.setOutputFormat("WEBM");
                    break;
            }
        }

    }

    //Get/Set the selected settings
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void quickSettings() {
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.recordHDVideo(wasHDSelected);
        hbRecorder.isAudioEnabled(isAudioEnabled);
        //Customise Notification
        hbRecorder.setNotificationSmallIcon(drawable2ByteArray(R.drawable.icon));
        hbRecorder.setNotificationTitle("Recording your screen");
        hbRecorder.setNotificationDescription("Drag down to stop the recording");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(RecordActivity.this, SettingsRecordActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Check if permissions was granted
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    //Handle permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                    //Permissions was provided
                    //Start screen recording
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startRecordingScreen();
                    }
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    //Set file path or Uri depending on SDK version
                    setOutputPath();
                    // Start screen recording
                    hbRecorder.startScreenRecording(data, resultCode, this);

                }
            }
        }
    }

    //For Android 10> we will pass a Uri to HBRecorder
    //This is not necessary - You can still use getExternalStoragePublicDirectory
    //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "HBRecorder");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        }else{
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +"/HBRecorder");
        }
    }

    //Generate a timestamp to be used as a file name
    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }

    //Show Toast
    private void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    //drawable to byte[]
    private byte[] drawable2ByteArray(@DrawableRes int drawableId) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), drawableId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
