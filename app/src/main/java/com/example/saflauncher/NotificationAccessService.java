package com.example.saflauncher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class NotificationAccessService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    public void swipeDownNotification() {
        Path path = new Path();
        path.moveTo(540, 0);  // start at top center (depends on resolution)
        path.lineTo(540, 1000);  // swipe down vertically

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 300);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        dispatchGesture(builder.build(), null, null);
    }
}
