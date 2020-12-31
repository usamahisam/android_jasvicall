package com.diengcyber.jasvicall;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.diengcyber.jasvicall.helpers.SettingRecorder;
import com.diengcyber.jasvicall.services.BillingApi;
import com.hbisoft.hbrecorder.HBRecorderListener;

import com.diengcyber.jasvicall.helpers.DeviceInfo;


@SuppressWarnings({"deprecation", "SameParameterValue"})
public class MainActivity extends AppCompatActivity implements HBRecorderListener {

    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private static final int PERMISSION_REQ_ID_PHONE_STATE = 97;

    private boolean hasPermissions = false;
    public static final int RESULT_ENABLE = 11;
    private ComponentName compName;

    private Button startbtn;

    private DeviceInfo dev_info;
    private BillingApi billing_api;
    private SettingRecorder settRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dev_info = new DeviceInfo();
        billing_api = new BillingApi(this);
        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // permission record audio
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO);
            }

            // permission write external storage
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
            }

            // permission read phone state
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE, PERMISSION_REQ_ID_PHONE_STATE);
            }

            if (!Settings.canDrawOverlays(this)) {
                Intent intent2 = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent2, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
            }

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            compName = new ComponentName(this, MyAdmin.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why we need this permission");
            startActivityForResult(intent, RESULT_ENABLE);
        } else {
        }
        initializeView();

    }

    /**
     * Set and initialize the view elements.
     */
    private void initializeView() {
        startbtn = findViewById(R.id.button_start);
        settRecorder = new SettingRecorder(this);
        if (settRecorder.isBusy()) {
            startbtn.setText(R.string.stop_recording);
        }

        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //first check if permissions was granted
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
                        hasPermissions = true;
                    }
                    if (hasPermissions) {
                        //check if recording is in progress
                        //and stop it if it is
                        if (settRecorder.isBusy()) {
                            settRecorder.stopRec(new SettingRecorder.MyCallback() {
                                @Override
                                public void onStart() {
                                }
                                @Override
                                public void onStop() {
                                    startbtn.setText(R.string.start_recording);
                                }
                                @Override
                                public void onError() {
                                }
                            });
                        } else {
                            startbtn.setEnabled(false);
                            startRecordingScreen();
                        }
                    }
                } else {
                    showLongToast("This library requires API 21>");
                }
            }
        });
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
            startActivity(new Intent(MainActivity.this, SettingsRecordActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //Handle permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                break;
            case PERMISSION_REQ_ID_PHONE_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                } else {
                    hasPermissions = false;
                }
                break;
            default:
                hasPermissions = false;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            // Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
            } else {
                // Permission is not available
            }
        } else if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                settRecorder.startRec(data, resultCode, new SettingRecorder.MyCallback() {
                    @Override
                    public void onStart() {
                        startbtn.setEnabled(true);
                    }
                    @Override
                    public void onStop() {
                        startbtn.setEnabled(true);
                    }
                    @Override
                    public void onError() {
                        startbtn.setEnabled(true);
                    }
                });
            } else {
                startbtn.setEnabled(true);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Check if permissions was granted
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void HBRecorderOnStart() {
        startbtn.setText(R.string.stop_recording);
    }

    @Override
    public void HBRecorderOnComplete() {
        showLongToast("Saved Successfully");
        startbtn.setText(R.string.start_recording);
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
    }

    // Start recording screen
    // It is important to call it like this
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRecordingScreen() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }

    // Show Toast
    private void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

}