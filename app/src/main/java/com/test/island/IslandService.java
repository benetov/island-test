package com.test.island;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IslandService extends Service {

    private WindowManager windowManager;
    private View islandView;
    private ImageView iconView;
    private TextView labelView;
    private Handler handler = new Handler();
    private String lastPackage = "";

    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            updateApp();
            handler.postDelayed(this, 800);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "island_ch";
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(channelId, "Island", NotificationManager.IMPORTANCE_LOW));
            Notification n = new Notification.Builder(this, channelId)
                    .setContentTitle("Остров работает")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .build();
            startForeground(1, n);
        } else {
            startForeground(1, new Notification());
        }

        createView();
        handler.post(pollRunnable);
    }

    private void createView() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(30, 15, 30, 15);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#111111"));
        bg.setCornerRadius(60f);
        bg.setStroke(3, Color.parseColor("#444444"));
        container.setBackground(bg);

        iconView = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(60, 60);
        iconLp.setMargins(0, 0, 20, 0);
        iconView.setLayoutParams(iconLp);

        labelView = new TextView(this);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(13);
        labelView.setText("...");

        TextView closeBtn = new TextView(this);
        closeBtn.setText(" X ");
        closeBtn.setTextColor(Color.GRAY);
        closeBtn.setTextSize(16);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });

        container.addView(iconView);
        container.addView(labelView);
        container.addView(closeBtn);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 20;

        islandView = container;
        try {
            windowManager.addView(islandView, params);
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void updateApp() {
        String pkg = getForegroundPackage();
        if (pkg == null || pkg.equals(lastPackage) || pkg.equals(getPackageName())) return;
        lastPackage = pkg;

        try {
            PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            String name = pm.getApplicationLabel(info).toString();
            iconView.setImageDrawable(pm.getApplicationIcon(info));
            labelView.setText(name.length() > 15 ? name.substring(0, 15) : name);
        } catch (Exception e) {
        }
    }

    private String getForegroundPackage() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(now - 10000, now);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPkg = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPkg = event.getPackageName();
            }
        }
        return lastPkg;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        if (islandView != null && windowManager != null) {
            try { windowManager.removeView(islandView); } catch (Exception e) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
                              }
