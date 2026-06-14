package com.liming.livestage;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Random;

public final class SimulationEngine {
    private static final int MAX_VIEWERS = 999999;
    public interface Listener {
        void onViewerCountChanged(int count);
        void onLikeCountChanged(long count);
        void onMessage(ChatMessage message);
        void onGift(GiftEvent event);
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final LocalCommentEngine comments = new LocalCommentEngine();
    private final Listener listener;
    private SimulationConfig config;
    private boolean running;
    private int viewers;
    private long likes;
    private long nextComment, nextEnter, nextFollow, nextGift;

    public SimulationEngine(SimulationConfig config, Listener listener) {
        this.config = config;
        this.listener = listener;
        resetCounters();
    }
    public void start() {
        if (running) return;
        running = true;
        schedule();
        listener.onViewerCountChanged(viewers);
        listener.onLikeCountChanged(likes);
        handler.post(tick);
    }
    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }
    public void applyConfig(SimulationConfig value) {
        config = value;
        viewers = clamp(value.initialViewers);
        nextComment = SystemClock.elapsedRealtime() + 1000L;
        listener.onViewerCountChanged(viewers);
        listener.onMessage(new ChatMessage("系统", "场景已切换为“" + value.scene.label + "”", ChatMessage.Type.SYSTEM));
    }
    public void resetSession(SimulationConfig value) {
        config = value;
        resetCounters();
        schedule();
        listener.onViewerCountChanged(viewers);
        listener.onLikeCountChanged(likes);
    }
    public void addManualComment() { listener.onMessage(comments.nextComment(config)); }
    public void addManualFollow() { listener.onMessage(comments.nextFollow(config)); }
    public void addManualGift() {
        listener.onGift(comments.nextGift(config));
        viewers = clamp(viewers + Math.max(2, viewers / 25));
        listener.onViewerCountChanged(viewers);
    }
    public void addManualLikes() {
        likes += 8 + random.nextInt(25);
        listener.onLikeCountChanged(likes);
    }
    private void resetCounters() {
        viewers = clamp(config.initialViewers);
        likes = 0L;
    }
    private void schedule() {
        long now = SystemClock.elapsedRealtime();
        nextComment = now + 1000L;
        nextEnter = now + 1800L;
        nextFollow = now + 7000L;
        nextGift = now + 9000L;
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = SystemClock.elapsedRealtime();
            updateViewers();
            int likeDelta = random.nextInt(7);
            if (likeDelta > 0) {
                likes += likeDelta;
                listener.onLikeCountChanged(likes);
            }
            if (now >= nextComment) {
                listener.onMessage(comments.nextComment(config));
                nextComment = now + Math.max(800L, config.speed.commentIntervalMs + random.nextInt(1200) - 200L);
            }
            if (now >= nextEnter) {
                listener.onMessage(comments.nextEnter(config));
                nextEnter = now + 3500L + random.nextInt(6000);
            }
            if (now >= nextFollow) {
                if (random.nextInt(100) < 70) listener.onMessage(comments.nextFollow(config));
                nextFollow = now + 8000L + random.nextInt(14000);
            }
            if (now >= nextGift) {
                if (random.nextInt(100) < 50) {
                    listener.onGift(comments.nextGift(config));
                    viewers = clamp(viewers + 1 + random.nextInt(Math.max(2, viewers / 40 + 1)));
                    listener.onViewerCountChanged(viewers);
                }
                nextGift = now + 13000L + random.nextInt(22000);
            }
            handler.postDelayed(this, 1000L);
        }
    };
    private void updateViewers() {
        int target = clamp(config.initialViewers);
        int amplitude = Math.max(2, Math.min(5000, viewers / 45));
        int delta = random.nextInt(amplitude * 2 + 1) - amplitude;
        int upper = Math.min(MAX_VIEWERS, Math.max(target + 25, target * 3));
        int lower = Math.max(1, target / 3);
        if (viewers > upper) delta = -Math.max(1, Math.abs(delta));
        else if (viewers < lower) delta = Math.max(1, Math.abs(delta));
        else if (random.nextInt(100) < 53) delta = Math.abs(delta);
        viewers = clamp(viewers + delta);
        listener.onViewerCountChanged(viewers);
    }
    private int clamp(int value) { return Math.max(1, Math.min(MAX_VIEWERS, value)); }
}
