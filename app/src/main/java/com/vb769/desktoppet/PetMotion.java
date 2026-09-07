package com.vb769.desktoppet;

/** Pure timing state: actions never change the saved window position. */
final class PetMotion {
    enum Action {
        IDLE(0, ""), WAVE(3200, "我是小凪，今天也陪着你！"),
        JUMP(2600, "看我跳高高！"), STRETCH(3800, "伸个懒腰，舒服～"),
        SLEEP(6500, "眯一会儿……晚点叫我哦。"), HAPPY(3400, "见到你，好开心！");
        final long duration;
        final String words;
        Action(long duration, String words) { this.duration=duration; this.words=words; }
    }
    private Action action=Action.IDLE;
    private long started;
    private int next;
    void start(Action value, long now) { action=value; started=now; }
    Action current(long now) {
        if (action != Action.IDLE && now-started >= action.duration) action=Action.IDLE;
        return action;
    }
    float progress(long now) {
        Action a=current(now);
        return a==Action.IDLE ? 0 : Math.max(0f, Math.min(1f, (float)(now-started)/a.duration));
    }
    Action nextAction(long now) {
        Action a=Action.values()[1 + next++ % 5];
        start(a, now);
        return a;
    }
    void reset() { action=Action.IDLE; }
}
