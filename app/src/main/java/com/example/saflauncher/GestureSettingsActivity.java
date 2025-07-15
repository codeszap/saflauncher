package com.example.saflauncher;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GestureSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);
        root.setBackgroundColor(Color.BLACK);

        // Title
        TextView title = new TextView(this);
        title.setText("🖐️ Gesture Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

        String[] gestures = {"Swipe Left", "Swipe Right", "Double Tap", "Swipe Down"};

        for (String g : gestures) {
            TextView tv = new TextView(this);
            tv.setText("👉 " + g + " (Tap to set)");
            tv.setTextColor(Color.LTGRAY);
            tv.setTextSize(16);
            tv.setPadding(0, 20, 0, 20);

            tv.setOnClickListener(v -> {
                if (g.equals("Double Tap")) {
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);

                    if (dpm.isAdminActive(admin)) {
                        dpm.lockNow(); // 🔒 Lock the screen
                    } else {
                        // Request admin permission
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Needed to lock screen on double tap");
                        startActivity(intent);
                    }
                }
                if (g.equals("Swipe Down")) {
                    // Ask user to enable accessibility service if not yet done
                    Intent accessibilityIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accessibilityIntent);

                    // Send broadcast to trigger swipe down
                    Intent intent = new Intent("com.example.OPEN_NOTIFICATION");
                    sendBroadcast(intent);
                }

            });

            root.addView(tv);
        }

        setContentView(root);
    }
}
