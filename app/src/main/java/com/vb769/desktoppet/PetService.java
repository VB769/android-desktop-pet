package com.vb769.desktoppet;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

public class PetService extends Service {
    private WindowManager manager;
    private WindowManager.LayoutParams params, bubbleParams;
    private CatView cat;
    private TextView bubble;
    private SharedPreferences prefs;
    private PowerManager power;
    private boolean folded, snap, autoActions, rightSide;
    private int size, petX, petY, foldedY;
    private AlertDialog menu;
    private long bubbleUntil, nextAuto, nextPermissionCheck;
    private final PetMotion motion=new PetMotion();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)-> {
        if(cat!=null && ("size".equals(key)||"folded".equals(key)||"snap".equals(key)||"autoActions".equals(key))) applySettings();
    };
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            long now=SystemClock.uptimeMillis();
            if(now>=nextPermissionCheck) {
                nextPermissionCheck=now+1000;
                if(!Settings.canDrawOverlays(PetService.this)){stopSelf();return;}
            }
            boolean visible=!folded && power.isInteractive();
            if(!visible) {
                removeBubble(); motion.reset(); nextAuto=now+25000;
            } else {
                if(bubble!=null && now>=bubbleUntil)removeBubble();
                if(autoActions && now>=nextAuto && motion.current(now)==PetMotion.Action.IDLE
                    && menu==null && cat!=null && !cat.touching) playNext();
                if(cat!=null)cat.invalidate();
            }
            handler.postDelayed(this,visible?(motion.current(now)==PetMotion.Action.IDLE?100:33):1000);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        prefs=getSharedPreferences("pet",0); power=getSystemService(PowerManager.class);
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && "STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("pet","桌面宠物",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PetService.class).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"pet").setSmallIcon(R.drawable.ic_companion)
            .setContentTitle("口袋小凪正在陪你").setContentText("短按互动 · 长按菜单 · 自由拖动")
            .setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"关闭小凪",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1,n);
        if(cat==null) {
            manager=getSystemService(WindowManager.class);
            params=new WindowManager.LayoutParams(1,1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.LEFT;
            readSettings();
            Rect r=usableScreen();
            petX=prefs.getInt("freeX",r.centerX()-dp(size)/2);
            petY=prefs.getInt("freeY",prefs.getInt("y",dp(180)));
            foldedY=prefs.getInt("foldedY",petY); rightSide=prefs.getBoolean("rightSide",true);
            place();
            cat=new CatView(); updateDescription();
            try {
                manager.addView(cat,params);
                nextAuto=SystemClock.uptimeMillis()+25000;
                handler.post(tick);
                if(!folded)play(PetMotion.Action.WAVE);
            }catch(RuntimeException e){cat=null;failOverlay();}
        }
        return START_NOT_STICKY;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private Rect usableScreen() {
        if(Build.VERSION.SDK_INT>=30) {
            WindowMetrics metrics=manager.getCurrentWindowMetrics();
            Rect r=new Rect(metrics.getBounds());
            Insets i=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            r.left+=i.left;r.top+=i.top;r.right-=i.right;r.bottom-=i.bottom;return r;
        }
        android.util.DisplayMetrics m=new android.util.DisplayMetrics();
        manager.getDefaultDisplay().getRealMetrics(m);
        return new Rect(0,dp(28),m.widthPixels,Math.max(dp(28),m.heightPixels-dp(48)));
    }
    private void readSettings() {
        size=PetGeometry.clamp(prefs.getInt("size",80),56,120);
        folded=prefs.getBoolean("folded",false);
        snap=prefs.getBoolean("snap",false);
        autoActions=prefs.getBoolean("autoActions",true);
        params.width=dp(folded?40:size);params.height=dp(folded?48:size);
    }
    private void place() {
        Rect r=usableScreen();
        // Expanded coordinates survive folding, rotating, resuming and resizing.
        petX=PetGeometry.restingX(petX,dp(size),r.left,r.right,snap);
        petY=PetGeometry.clamp(petY,r.top,r.bottom-dp(size));
        if(folded) {
            params.x=PetGeometry.edgeX(rightSide,r.left,r.right,params.width);
            foldedY=PetGeometry.clamp(foldedY,r.top,r.bottom-params.height);
            params.y=foldedY;
        } else {params.x=petX;params.y=petY;}
    }
    private void savePosition() {
        prefs.edit().putInt("freeX",petX).putInt("freeY",petY)
            .putInt("foldedY",foldedY).putBoolean("rightSide",rightSide).apply();
    }
    private void applySettings() {
        cat.cancelTouch(); removeBubble(); motion.reset();
        boolean wasFolded=folded;int oldSize=size;
        if(!wasFolded) {
            petX=params.x;petY=params.y;
            Rect r=usableScreen();rightSide=PetGeometry.nearestRight(petX,dp(size),r.left,r.right);
        }
        readSettings();
        petX=PetGeometry.resizedOrigin(petX,dp(oldSize),dp(size));
        petY=PetGeometry.resizedOrigin(petY,dp(oldSize),dp(size));
        if(folded&&!wasFolded)foldedY=petY;
        place();updateDescription();updateWindow();savePosition();
        nextAuto=SystemClock.uptimeMillis()+25000;
    }
    private void updateDescription() {
        cat.setContentDescription(folded?"展开口袋小凪，长按菜单":
            "口袋小凪，短按冒字并做动作，长按菜单，可自由拖动");
    }
    private void updateWindow() {
        if(cat==null||!cat.isAttachedToWindow())return;
        try {manager.updateViewLayout(cat,params);cat.invalidate();}
        catch(RuntimeException e){failOverlay();}
    }
    private void failOverlay() {
        Toast.makeText(this,"悬浮窗不可用，请检查权限后重新召唤",Toast.LENGTH_LONG).show();stopSelf();
    }
    private void playNext() {
        PetMotion.Action action=motion.nextAction(SystemClock.uptimeMillis());play(action);
    }
    private void play(PetMotion.Action action) {
        if(cat==null||folded)return;
        long now=SystemClock.uptimeMillis();motion.start(action,now);
        nextAuto=now+action.duration+25000;
        speak(action.words,Math.min(action.duration,4500));
        cat.invalidate();
    }
    private void speak(String words,long duration) {
        removeBubble();
        if(!Settings.canDrawOverlays(this))return;
        Rect r=usableScreen();
        TextView text=new TextView(this);
        text.setText(words);text.setTextSize(14);text.setTextColor(0xFF51425F);
        text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        text.setGravity(Gravity.CENTER);text.setMaxLines(2);text.setPadding(dp(10),dp(7),dp(10),dp(7));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xFFFFFCF8);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),0xFFE1D4EC);text.setBackground(bg);
        int maxWidth=Math.max(1,Math.min(dp(180),r.width()));
        text.measure(View.MeasureSpec.makeMeasureSpec(maxWidth,View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        bubbleParams=new WindowManager.LayoutParams(text.getMeasuredWidth(),text.getMeasuredHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        bubbleParams.gravity=Gravity.TOP|Gravity.LEFT;
        // Non-interactive speech must not block underlying apps' touches on Android 12+.
        bubbleParams.alpha=.75f;
        bubbleParams.x=PetGeometry.clamp(params.x+params.width/2-bubbleParams.width/2,r.left,r.right-bubbleParams.width);
        bubbleParams.y=PetGeometry.bubbleY(params.y,params.height,bubbleParams.height,r.top,r.bottom,dp(3));
        try {manager.addView(text,bubbleParams);bubble=text; bubbleUntil=SystemClock.uptimeMillis()+duration;}
        catch(RuntimeException e){bubble=null;failOverlay();}
    }
    private void removeBubble() {
        if(bubble!=null){if(bubble.isAttachedToWindow())manager.removeView(bubble);bubble=null;}
    }
    private void dismissMenu() {
        if(menu!=null){AlertDialog old=menu;menu=null;old.dismiss();}
    }
    private void showMenu() {
        if(!Settings.canDrawOverlays(this)){stopSelf();return;}
        dismissMenu();removeBubble();
        String[] choices={"挥手打招呼","跳一跳","伸懒腰","打个盹","开心欢呼",
            "更小一点","更大一点",folded?"展开小凪":"收起小凪",
            snap?"关闭自动贴边（自由放置）":"开启自动贴边","完整设置","关闭小凪"};
        AlertDialog dialog=new AlertDialog.Builder(new ContextThemeWrapper(this,android.R.style.Theme_Material_Light_Dialog_Alert))
            .setTitle("口袋小凪 · 动作与设置")
            .setItems(choices,(d,which)->{
                if(which<5) {
                    if(folded)prefs.edit().putBoolean("folded",false).apply();
                    play(PetMotion.Action.values()[which+1]);
                }else if(which==5||which==6){
                    prefs.edit().putInt("size",PetGeometry.clamp(size+(which==5?-8:8),56,120)).apply();
                }else if(which==7)prefs.edit().putBoolean("folded",!folded).apply();
                else if(which==8)prefs.edit().putBoolean("snap",!snap).apply();
                else if(which==9)startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                else stopSelf();
            }).setNegativeButton("返回",null).create();
        menu=dialog;
        dialog.setOnDismissListener(d->{if(menu==dialog)menu=null;});
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        try{dialog.show();}catch(RuntimeException e){dismissMenu();failOverlay();}
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);dismissMenu();removeBubble();
        if(cat!=null){cat.cancelTouch();place();updateWindow();savePosition();}
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);prefs.unregisterOnSharedPreferenceChangeListener(listener);
        dismissMenu();removeBubble();
        if(cat!=null){cat.cancelTouch();savePosition();if(cat.isAttachedToWindow())manager.removeView(cat);cat=null;}
        stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
    private class CatView extends View {
        private final CatRenderer renderer=new CatRenderer(getResources());
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.drawable.Drawable ribbon=getDrawable(R.drawable.ic_companion).mutate();
        float downX,downY;int startX,startY;boolean dragging,holding,touching;
        final Runnable longPress=()->{if(touching&&!dragging){holding=true;showMenu();}};
        CatView(){super(PetService.this);setClickable(true);ribbon.setTint(Color.WHITE);ribbon.setBounds(7,11,33,37);}
        void cancelTouch(){handler.removeCallbacks(longPress);touching=false;}
        @Override public boolean performClick() {
            super.performClick();
            if(folded){prefs.edit().putBoolean("folded",false).apply();play(PetMotion.Action.WAVE);}
            else playNext();
            return true;
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if(folded) {
                c.save();c.scale(getWidth()/40f,getHeight()/48f);
                paint.setColor(0xEE8875B9);c.drawRoundRect(0,4,40,44,19,19,paint);
                // The companion's ribbon replaces the old paw symbol.
                ribbon.draw(c);
                c.restore();
            }else{
                long now=SystemClock.uptimeMillis();
                renderer.draw(c,getWidth(),getHeight(),motion.current(now),motion.progress(now),now,touching&&dragging);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    downX=e.getRawX();downY=e.getRawY();startX=params.x;startY=params.y;
                    dragging=false;holding=false;touching=true;removeBubble();
                    handler.postDelayed(longPress,ViewConfiguration.getLongPressTimeout());return true;
                case MotionEvent.ACTION_MOVE:
                    if(!touching||holding)return true;
                    float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
                    if(Math.hypot(dx,dy)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){
                        dragging=true;handler.removeCallbacks(longPress);motion.reset();
                    }
                    if(dragging){
                        Rect r=usableScreen();
                        params.x=PetGeometry.clamp(startX+(int)dx,r.left,r.right-params.width);
                        params.y=PetGeometry.clamp(startY+(int)dy,r.top,r.bottom-params.height);
                        updateWindow();
                    }return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if(!touching)return true;
                    cancelTouch();
                    if(dragging) {
                        Rect r=usableScreen();
                        if(folded){rightSide=PetGeometry.nearestRight(params.x,params.width,r.left,r.right);foldedY=params.y;}
                        else{petX=params.x;petY=params.y;}
                        place();updateWindow();savePosition();nextAuto=SystemClock.uptimeMillis()+25000;
                    }else if(!holding&&e.getActionMasked()==MotionEvent.ACTION_UP)performClick();
                    return true;
                default:return true;
            }
        }
    }
}
