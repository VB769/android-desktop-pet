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
    private SharedPreferences prefs;
    private boolean folded, rightSide;
    private int size;
    private AlertDialog menu;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsListener = (p, key) -> {
        if (cat != null && ("size".equals(key) || "folded".equals(key))) applySettings();
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!Settings.canDrawOverlays(PetService.this)) { stopSelf(); return; }
            boolean active = !folded && getSystemService(PowerManager.class).isInteractive();
            if (cat != null && active) cat.invalidate();
            handler.postDelayed(this, active ? 100 : 1000);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("pet", 0);
        prefs.registerOnSharedPreferenceChangeListener(settingsListener);
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("pet", "桌面宠物", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, PetService.class).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, "pet")
            .setSmallIcon(R.drawable.ic_cat).setContentTitle("口袋小猫正在陪你")
            .setContentText("点击打开大小与显示设置").setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "关闭小猫", stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, notification);
        if (cat == null) {
            manager = getSystemService(WindowManager.class);
            rightSide = prefs.getBoolean("rightSide", true);
            params = new WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.y = prefs.getInt("y", dp(180));
            readSettings();
            placeAtEdge();
            cat = new CatView();
            updateDescription();
            try { manager.addView(cat, params); handler.post(tick); }
            catch (RuntimeException e) { cat = null; failOverlay(); }
        }
        return START_NOT_STICKY;
    }
    private void failOverlay() {
        Toast.makeText(this, "悬浮窗不可用，请在设置中检查权限后重新召唤", Toast.LENGTH_LONG).show();
        stopSelf();
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private Rect usableScreen() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics metrics = manager.getCurrentWindowMetrics();
            Rect r = new Rect(metrics.getBounds());
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            r.left += insets.left; r.top += insets.top;
            r.right -= insets.right; r.bottom -= insets.bottom;
            return r;
        }
        android.util.DisplayMetrics m = new android.util.DisplayMetrics();
        manager.getDefaultDisplay().getRealMetrics(m);
        return new Rect(0, dp(28), m.widthPixels, Math.max(dp(28), m.heightPixels - dp(48)));
    }
    private void readSettings() {
        size = PetGeometry.clamp(prefs.getInt("size", 80), 56, 120);
        folded = prefs.getBoolean("folded", false);
        params.width = dp(folded ? 40 : size);
        params.height = dp(folded ? 48 : Math.round(size * 112f / 120f));
    }
    private void placeAtEdge() {
        Rect r = usableScreen();
        params.x = PetGeometry.edgeX(rightSide, r.left, r.right, params.width);
        params.y = PetGeometry.clamp(params.y, r.top, r.bottom - params.height);
    }
    private void savePosition() {
        prefs.edit().putBoolean("rightSide", rightSide).putInt("y", params.y).apply();
    }
    private void updateDescription() {
        cat.setContentDescription(folded ? "展开口袋小猫；可拖动，长按打开菜单" :
            "口袋小猫，点击打开互动、大小、收起和关闭菜单；拖动松手贴边");
    }
    private void updateWindow() {
        if (cat == null || !cat.isAttachedToWindow()) return;
        try { manager.updateViewLayout(cat, params); cat.invalidate(); }
        catch (RuntimeException e) { failOverlay(); }
    }
    private void applySettings() {
        cat.cancelTouch();
        readSettings(); placeAtEdge(); updateDescription(); updateWindow(); savePosition();
    }
    private void dismissMenu() {
        if (menu != null) { menu.dismiss(); menu = null; }
    }
    private void showMenu() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        dismissMenu();
        String[] items = { "摸摸小猫", "更小一点", "更大一点", folded ? "展开小猫" : "收起到边缘",
            "打开完整设置", "关闭小猫" };
        menu = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_Material_Light_Dialog_Alert))
            .setTitle("口袋小猫 · " + size)
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    prefs.edit().putBoolean("folded", false).apply();
                    if (cat != null) { cat.happyUntil = SystemClock.uptimeMillis() + 1800; cat.invalidate(); }
                } else if (which == 1 || which == 2) {
                    int next = PetGeometry.clamp(size + (which == 1 ? -8 : 8), 56, 120);
                    boolean atLimit = next == size;
                    prefs.edit().putInt("size", next).apply();
                    Toast.makeText(this, atLimit ? "已到大小范围边界" : "大小已调整", Toast.LENGTH_SHORT).show();
                } else if (which == 3) {
                    prefs.edit().putBoolean("folded", !folded).apply();
                } else if (which == 4) {
                    startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else stopSelf();
            }).setNegativeButton("返回", null).create();
        menu.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        try { menu.show(); }
        catch (RuntimeException e) { dismissMenu(); failOverlay(); }
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        dismissMenu();
        if (cat != null) { cat.cancelTouch(); placeAtEdge(); updateWindow(); savePosition(); }
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        prefs.unregisterOnSharedPreferenceChangeListener(settingsListener);
        dismissMenu();
        if (cat != null) {
            cat.cancelTouch(); savePosition();
            if (cat.isAttachedToWindow()) manager.removeView(cat);
            cat = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    private class CatView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path shape = new Path();
        float downX, downY;
        int startX, startY;
        long happyUntil;
        boolean dragging, holding, touching;
        final Runnable longPress = () -> {
            if (touching && !dragging) { holding = true; showMenu(); }
        };
        CatView() {
            super(PetService.this);
            setClickable(true);
        }
        void cancelTouch() {
            handler.removeCallbacks(longPress);
            touching = false;
        }
        @Override public boolean performClick() {
            super.performClick();
            if (folded) prefs.edit().putBoolean("folded", false).apply();
            else showMenu();
            return true;
        }
        void color(int c) { p.setColor(c); p.setStyle(Paint.Style.FILL); }
        void stroke(int c, float width) { p.setColor(c); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width); p.setStrokeCap(Paint.Cap.ROUND); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.save();
            if (folded) {
                c.scale(getWidth()/40f, getHeight()/48f);
                color(0xEE7762B2); c.drawRoundRect(0, 4, 40, 44, 19, 19, p);
                color(Color.WHITE);
                c.drawOval(14, 24, 28, 35, p);
                c.drawCircle(11, 21, 3, p); c.drawCircle(18, 17, 3, p); c.drawCircle(26, 18, 3, p); c.drawCircle(31, 23, 3, p);
                c.restore(); return;
            }
            c.scale(getWidth()/120f, getHeight()/112f);
            c.translate(0, -30);
            long now = SystemClock.uptimeMillis();
            boolean happy = now < happyUntil;
            float bob = (float)Math.sin(now/450.0)*1.2f;
            c.translate(0, bob);
            color(0x226B5984); c.drawOval(24, 129, 100, 138, p);
            stroke(0xFFB4A0E5, 13);
            shape.reset(); shape.moveTo(86, 117);
            shape.cubicTo(115, 119, 117, 95 + (float)Math.sin(now/650.0)*3, 103, 92);
            c.drawPath(shape, p);
            color(0xFFE5D9FF); c.drawOval(30, 80, 94, 132, p);
            color(0xFFF8F2FF); c.drawOval(43, 101, 80, 131, p);
            color(0xFFE5D9FF);
            shape.reset();
            shape.moveTo(22, 66); shape.lineTo(22, 34); shape.lineTo(47, 53); shape.close();
            shape.moveTo(73, 53); shape.lineTo(98, 34); shape.lineTo(99, 68); shape.close();
            c.drawPath(shape, p);
            c.drawOval(16, 48, 104, 108, p);
            color(0xFFFFB7CD);
            shape.reset();
            shape.moveTo(27, 51); shape.lineTo(27, 41); shape.lineTo(38, 52); shape.close();
            shape.moveTo(82, 52); shape.lineTo(93, 41); shape.lineTo(94, 54); shape.close();
            c.drawPath(shape, p);
            c.drawOval(26, 82, 42, 90, p); c.drawOval(78, 82, 94, 90, p);
            color(0xFF483856);
            if (happy || now % 4300 < 180) {
                stroke(0xFF483856, 3); c.drawArc(36, 71, 47, 81, 200, 140, false, p); c.drawArc(73, 71, 84, 81, 200, 140, false, p);
            } else {
                c.drawOval(39, 69, 46, 80, p); c.drawOval(74, 69, 81, 80, p);
                color(Color.WHITE); c.drawCircle(42, 71, 1.5f, p); c.drawCircle(77, 71, 1.5f, p);
            }
            color(0xFF795B86); c.drawOval(57, 82, 63, 86, p);
            stroke(0xFF795B86, 2);
            c.drawArc(50, 84, 60, 94, 0, 160, false, p); c.drawArc(60, 84, 70, 94, 20, 160, false, p);
            stroke(0xFFB4A0E5, 1.5f);
            c.drawLine(22, 82, 32, 84, p); c.drawLine(23, 90, 32, 89, p);
            c.drawLine(88, 84, 98, 82, p); c.drawLine(88, 89, 97, 90, p);
            color(0xFFCCB9EE); c.drawOval(34, 105, 48, 128, p); c.drawOval(76, 105, 90, 128, p);
            color(0xFFB4A0E5); c.drawOval(32, 124, 53, 135, p); c.drawOval(71, 124, 92, 135, p);
            stroke(0xFF9178BF, 1.2f); c.drawLine(40, 130, 40, 134, p); c.drawLine(79, 130, 79, 134, p);
            if (happy) {
                color(0xFFEA719B);
                shape.reset(); shape.moveTo(61, 118); shape.cubicTo(44, 109, 54, 101, 61, 107);
                shape.cubicTo(68, 101, 78, 109, 61, 118); c.drawPath(shape, p);
            }
            c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX=e.getRawX(); downY=e.getRawY(); startX=params.x; startY=params.y;
                    dragging=false; holding=false; touching=true;
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!touching || holding) return true;
                    float dx=e.getRawX()-downX, dy=e.getRawY()-downY;
                    if (Math.hypot(dx,dy) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                        dragging=true; handler.removeCallbacks(longPress);
                    }
                    if (dragging) {
                        Rect r=usableScreen();
                        params.x=PetGeometry.clamp(startX+(int)dx, r.left, r.right-params.width);
                        params.y=PetGeometry.clamp(startY+(int)dy, r.top, r.bottom-params.height);
                        updateWindow();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!touching) return true;
                    cancelTouch();
                    if (dragging) {
                        Rect r=usableScreen();
                        rightSide=PetGeometry.nearestRight(params.x, params.width, r.left, r.right);
                        placeAtEdge(); updateWindow(); savePosition();
                    } else if (!holding && e.getActionMasked()==MotionEvent.ACTION_UP) performClick();
                    return true;
                default: return true;
            }
        }
    }
}
