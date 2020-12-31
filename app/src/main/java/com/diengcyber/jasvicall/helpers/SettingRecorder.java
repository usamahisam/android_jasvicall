package com.diengcyber.jasvicall.helpers;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.diengcyber.jasvicall.FloatingViewService;
import com.diengcyber.jasvicall.MainActivity;
import com.diengcyber.jasvicall.R;
import com.diengcyber.jasvicall.retro.UuidModel;
import com.diengcyber.jasvicall.services.BillingApi;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingRecorder extends ContextWrapper implements HBRecorderListener {

    Context context;
    private HBRecorder hbRecorder;
    private BillingApi billing_api;
    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;

    public SettingRecorder(Context base) {
        super(base);
        this.context  = base;
        hbRecorder = new HBRecorder(this, this);
        hbRecorder.enableCustomSettings();
        billing_api = new BillingApi(base);
    }

    public interface MyCallback {
        void onStart();
        void onStop();
        void onError();
    }

    public void startRec(Intent data, int resultCode, MyCallback callback) {
        billing_api.startBilling(new Callback<UuidModel>() {
            @Override
            public void onResponse(Call<UuidModel> call, Response<UuidModel> response) {
                if (response.isSuccessful()) {
                    customSettings();
                    // Set file path or Uri depending on SDK version
                    setOutputPath();
                    // Start screen recording
                    hbRecorder.startScreenRecording(data, resultCode, (Activity) context);

                    callback.onStart();
                    Log.i("A", "post submitted to API." + response);
                }
            }
            @Override
            public void onFailure(Call<UuidModel> call, Throwable t) {
                callback.onError();
                Log.e("Err", String.valueOf(t));
                Log.e("A", "Unable to submit post to API.");
            }
        });
    }

    public void stopRec(MyCallback callback) {
        if (hbRecorder.isBusyRecording()) {
            billing_api.closeBilling(new Callback<UuidModel>() {
                @Override
                public void onResponse(Call<UuidModel> call, Response<UuidModel> response) {
                    if (response.isSuccessful()) {
                        hbRecorder.stopScreenRecording();
                        callback.onStop();
                        Log.i("A", "post submitted to API." + response);
                    }
                }
                @Override
                public void onFailure(Call<UuidModel> call, Throwable t) {
                    callback.onError();
                    Log.e("Err", String.valueOf(t));
                    Log.e("A", "Unable to submit post to API.");
                }
            });
        }
    }

    public boolean isBusy() {
        return hbRecorder.isBusyRecording();
    }

    //Generate a timestamp to be used as a file name
    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }

    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ScreenRecorder");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "ScreenRecorder");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        }else{
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +"/ScreenRecorder");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void customSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Is audio enabled
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    public void updateGalleryUri(){
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }

    @Override
    public void HBRecorderOnStart() {
        startService(new Intent(this, FloatingViewService.class));
        Log.e("ScreenRecorder", "ScreenRecorderOnStart called");
    }

    @Override
    public void HBRecorderOnComplete() {
        stopService(new Intent(this, FloatingViewService.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Update gallery depending on SDK Level
            if (hbRecorder.wasUriSet()) {
                updateGalleryUri();
            }else{
                refreshGalleryFile();
            }
        }
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        // Error 38 happens when
        // - the selected video encoder is not supported
        // - the output format is not supported
        // - if another app is using the microphone
        // It is best to use device default
        if (errorCode == 38) {
        } else {
            Log.e("HBRecorderOnError", reason);
        }
    }

}
