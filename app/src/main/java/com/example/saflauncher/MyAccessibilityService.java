package com.example.saflauncher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class MyAccessibilityService extends AccessibilityService {

    public static MyAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Toast.makeText(this, "✅ Accessibility Service Connected", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    public void triggerSwipeDown() {
        Path path = new Path();
        path.moveTo(540, 0);          // Swipe from top-center
        path.lineTo(540, 1200);       // Swipe to bottom

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));

        dispatchGesture(builder.build(), null, null);
    }
}
