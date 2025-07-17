package com.example.saflauncher;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.graphics.PixelFormat;

import androidx.annotation.Nullable;

public class EdgeGestureService extends Service {

    private WindowManager windowManager;
    private View edgeView;
    private View arrowView;

    private float startX, startY, endX, endY;
    private long startTime;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        addArrowView();
        addEdgeSwipeView();
    }

    private void addArrowView() {
        arrowView = new View(this);
        arrowView.setBackgroundResource(R.drawable.ic_swipe_arrow); // 👉 your custom arrow drawable
        arrowView.setVisibility(View.GONE); // initially hidden

        WindowManager.LayoutParams arrowParams = new WindowManager.LayoutParams(
                100, 100,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        arrowParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        arrowParams.x = 40;

        windowManager.addView(arrowView, arrowParams);
    }

    private void addEdgeSwipeView() {
        edgeView = new View(this);
        edgeView.setBackgroundColor(0x00000000); // fully transparent

        int edgeWidth = 30;
        int swipeHeight = 1000;

        WindowManager.LayoutParams edgeParams = new WindowManager.LayoutParams(
                edgeWidth,
                swipeHeight,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        edgeParams.gravity = Gravity.END | Gravity.TOP;

        edgeView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    startTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_UP:
                    endX = event.getX();
                    endY = event.getY();
                    long holdDuration = System.currentTimeMillis() - startTime;

                    if (startX < 100 && holdDuration > 200 && Math.abs(endX - startX) > 100) {
                        vibrate();
                        showSystemVolumePanel();
                        showArrowBriefly();
                    }
                    return true;
            }
            return false;
        });

        windowManager.addView(edgeView, edgeParams);
    }

    private void showSystemVolumePanel() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
            );
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(40);
            }
        }
    }

    private void showArrowBriefly() {
        if (arrowView != null) {
            arrowView.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> arrowView.setVisibility(View.GONE), 800);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (edgeView != null) {
            windowManager.removeView(edgeView);
        }
        if (arrowView != null) {
            windowManager.removeView(arrowView);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
