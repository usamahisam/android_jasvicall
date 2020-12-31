package com.diengcyber.jasvicall;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.diengcyber.jasvicall.helpers.DeviceInfo;
import com.diengcyber.jasvicall.helpers.SettingRecorder;
import com.diengcyber.jasvicall.services.BillingApi;
import com.hbisoft.hbrecorder.HBRecorderListener;


public class FloatingViewService extends Service implements HBRecorderListener {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private TextView tv_timer, tv_timer_1;
    private CountDownTimer timer;

    private DeviceInfo dev_info;
    private BillingApi billing_api;
    private SettingRecorder settRecorder;

    public FloatingViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Init HBRecorder
            settRecorder = new SettingRecorder(this);
        }

        dev_info = new DeviceInfo();
        billing_api = new BillingApi(this);

        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);
        tv_timer = (TextView) mFloatingView.findViewById(R.id.tv_timer);
        tv_timer_1 = (TextView) mFloatingView.findViewById(R.id.tv_timer_1);
        setupTimer();

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE | WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        //The root element of the collapsed view layout
        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
        //The root element of the expanded view layout
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);


        //Set the close button
        ImageView closeButton = (ImageView) mFloatingView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // collapsedView.setVisibility(View.VISIBLE);
                // expandedView.setVisibility(View.GONE);
                settRecorder.stopRec(new SettingRecorder.MyCallback() {
                    @Override
                    public void onStart() {
                    }
                    @Override
                    public void onStop() {
                        stopSelf();
                    }
                    @Override
                    public void onError() {
                    }
                });
            }
        });


        //Drag and move floating view using user's touch action.
        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Detect if the floating view is collapsed or expanded.
     *
     * @return true if the floating view is collapsed.
     */
    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }

    /**
     *  Setup Timer
     */
    private void setupTimer() {
        //  long totalSeconds = 1; // 1 detik
        long intervalSeconds = 1;
        timer = new CountDownTimer(Long.MAX_VALUE, intervalSeconds * 1000) {
            public void onTick(long millisUntilFinished) {
                long time = (Long.MAX_VALUE - millisUntilFinished) / 1000;
                String asText = String.format("%02d:%02d", (time / 60), (time % 60));
                tv_timer.setText(asText);
                tv_timer_1.setText(asText);
            }
            public void onFinish() {
            }
        };
        timer.start();
    }

    @Override
    public void HBRecorderOnStart() {
    }

    @Override
    public void HBRecorderOnComplete() {
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        if (errorCode == 38) {
        } else {
            Log.e("HBRecorderOnError", reason);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        stopSelf();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}