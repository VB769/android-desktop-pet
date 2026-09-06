package com.vb769.desktoppet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        page.setPadding(pad, pad, pad, pad);
        page.setBackgroundColor(Color.rgb(247, 244, 255));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("口袋小猫");
        title.setTextSize(32);
        title.setTextColor(Color.rgb(79, 59, 135));
        title.setGravity(Gravity.CENTER);
        page.addView(title);
        TextView intro = new TextView(this);
        intro.setText("把一只小猫带到手机桌面\n拖动移动 · 点击打招呼\n长按小猫可打开控制页面");
        intro.setTextSize(17);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, pad, 0, pad);
        page.addView(intro);
        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        page.addView(status);
        button(page, "① 允许悬浮显示", () -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
            catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "请在系统设置中搜索悬浮窗，允许口袋小猫", Toast.LENGTH_LONG).show();
            }
        });
        button(page, "② 召唤小猫", () -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先允许悬浮显示，再回来召唤小猫", Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else summon();
        });
        button(page, "让小猫休息（关闭）", () -> {
            stopService(new Intent(this, PetService.class));
            Toast.makeText(this, "小猫休息啦", Toast.LENGTH_SHORT).show();
        });
        TextView note = new TextView(this);
        note.setText("启动后按手机主页键，就能看到小猫。\n不需要登录；本应用不申请联网权限。\n若小猫被系统关闭，可重新打开本应用召唤。");
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, pad, 0, 0);
        page.addView(note);
    }
    private void button(LinearLayout page, String label, Runnable action) {
        Button b = new Button(this); b.setText(label);
        page.addView(b, new LinearLayout.LayoutParams(-1, -2));
        b.setOnClickListener(v -> action.run());
    }
    private void summon() {
        if (!Settings.canDrawOverlays(this)) return;
        startForegroundService(new Intent(this, PetService.class));
        Toast.makeText(this, "已召唤，可以返回桌面啦", Toast.LENGTH_SHORT).show();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1) summon();
    }
    @Override public void onResume() {
        super.onResume();
        status.setText(Settings.canDrawOverlays(this) ? "悬浮显示：已允许 ✓" : "悬浮显示：尚未允许");
    }
}
