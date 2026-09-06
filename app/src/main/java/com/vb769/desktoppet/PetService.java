package com.vb769.desktoppet;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.Toast;

public class PetService extends Service {
    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private CatView cat;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!Settings.canDrawOverlays(PetService.this)) { stopSelf(); return; }
            if (cat != null) cat.invalidate();
            handler.postDelayed(this, 100);
        }
    };
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("pet", "桌面宠物", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, PetService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, "pet")
            .setSmallIcon(com.vb769.desktoppet.R.drawable.ic_cat).setContentTitle("口袋小猫正在陪你")
            .setContentText("点击打开控制页面，或点关闭让小猫休息")
            .setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "关闭小猫", stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, notification);
        if (cat == null) {
            manager = getSystemService(WindowManager.class);
            params = new WindowManager.LayoutParams(dp(120), dp(144), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = getSharedPreferences("pet", 0).getInt("x", 24);
            params.y = getSharedPreferences("pet", 0).getInt("y", 250);
            clamp();
            cat = new CatView();
            try { manager.addView(cat, params); handler.post(tick); }
            catch (RuntimeException e) { cat = null; Toast.makeText(this, "悬浮窗启动失败，请检查系统权限", Toast.LENGTH_LONG).show(); stopSelf(); }
        }
        return START_NOT_STICKY;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private void clamp() {
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        params.x = Math.max(0, Math.min(params.x, m.widthPixels - params.width));
        params.y = Math.max(0, Math.min(params.y, m.heightPixels - params.height - dp(32)));
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (cat != null) { clamp(); manager.updateViewLayout(cat, params); }
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (cat != null) {
            getSharedPreferences("pet", 0).edit().putInt("x", params.x).putInt("y", params.y).apply();
            if (cat.isAttachedToWindow()) manager.removeView(cat);
            cat = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    private class CatView extends View {
        final Paint p = new Paint(3);
        final GestureDetector detector;
        float downX, downY;
        int startX, startY;
        long happyUntil;
        boolean dragged;
        CatView() {
            super(PetService.this);
            setContentDescription("口袋小猫，点击打招呼，拖动移动，长按打开控制");
            detector = new GestureDetector(PetService.this, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDown(MotionEvent e) { return true; }
                @Override public boolean onSingleTapUp(MotionEvent e) { performClick(); return true; }
                @Override public void onLongPress(MotionEvent e) {
                    if (!dragged) startActivity(new Intent(PetService.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            });
        }
        @Override public boolean performClick() {
            super.performClick(); happyUntil = SystemClock.uptimeMillis() + 2200; invalidate(); return true;
        }
        void color(int c) { p.setColor(c); p.setStyle(Paint.Style.FILL); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.save(); c.scale(getWidth()/120f, getHeight()/144f);
            long now = SystemClock.uptimeMillis();
            boolean happy = now < happyUntil;
            float bob = (float)Math.sin(now/450.0)*2;
            if (happy) {
                color(Color.WHITE); c.drawRoundRect(8, 2, 112, 29, 12, 12, p);
                color(Color.rgb(91, 67, 133)); p.setTextSize(14); p.setTextAlign(Paint.Align.CENTER);
                c.drawText("喵～ 陪着你！", 60, 21, p);
            }
            c.translate(0, bob);
            color(0x226B5984); c.drawOval(24, 129, 100, 138, p);
            color(0xFFB4A0E5); c.drawOval(79, 89, 113, 119, p);
            color(0xFFE5D9FF); c.drawOval(30, 80, 94, 132, p);
            Path ears = new Path();
            ears.moveTo(22, 66); ears.lineTo(22, 34); ears.lineTo(47, 53);
            ears.moveTo(73, 53); ears.lineTo(98, 34); ears.lineTo(99, 68);
            c.drawPath(ears, p);
            c.drawOval(16, 48, 104, 108, p);
            color(0xFFFFB7CD);
            Path inner = new Path();
            inner.moveTo(27, 51); inner.lineTo(27, 41); inner.lineTo(38, 52);
            inner.moveTo(82, 52); inner.lineTo(93, 41); inner.lineTo(94, 54);
            c.drawPath(inner, p);
            c.drawOval(26, 82, 42, 90, p); c.drawOval(78, 82, 94, 90, p);
            color(0xFF483856);
            if (happy || now % 4300 < 180) {
                p.setStrokeWidth(3); c.drawLine(37, 75, 46, 75, p); c.drawLine(74, 75, 83, 75, p);
            } else { c.drawOval(39, 69, 45, 80, p); c.drawOval(75, 69, 81, 80, p); }
            c.drawOval(57, 82, 63, 86, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawArc(50, 84, 60, 94, 0, 160, false, p);
            c.drawArc(60, 84, 70, 94, 20, 160, false, p);
            color(0xFFB4A0E5); c.drawOval(32, 121, 53, 134, p); c.drawOval(71, 121, 92, 134, p);
            c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downX=e.getRawX(); downY=e.getRawY(); startX=params.x; startY=params.y; dragged=false;
            } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx=e.getRawX()-downX, dy=e.getRawY()-downY;
                if (Math.hypot(dx,dy) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) dragged=true;
                if (dragged) {
                    params.x=startX+(int)dx; params.y=startY+(int)dy; clamp();
                    manager.updateViewLayout(this,params);
                }
            }
            detector.onTouchEvent(e);
            return true;
        }
    }
}
