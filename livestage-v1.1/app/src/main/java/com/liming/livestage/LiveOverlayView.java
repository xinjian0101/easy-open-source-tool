package com.liming.livestage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class LiveOverlayView extends View {
    private static final int MAX_MESSAGES = 7;
    private static final int MAX_HEARTS = 64;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Deque<ChatMessage> messages = new ArrayDeque<>();
    private final List<HeartParticle> hearts = new ArrayList<>();
    private final Random random = new Random();

    private String hostName = "HOST";
    private String title = "直播训练演示";
    private int viewers = 128;
    private long likes;
    private GiftEvent activeGift;
    private long activeGiftUntil;

    public LiveOverlayView(Context context) {
        super(context);
        textPaint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
    }

    public void setHost(String hostName, String title) {
        this.hostName = hostName == null || hostName.trim().isEmpty() ? "HOST" : hostName.trim();
        this.title = title == null ? "" : title.trim();
        invalidate();
    }

    public void setViewers(int value) {
        viewers = Math.max(1, value);
        invalidate();
    }

    public void setLikes(long value) {
        long normalized = Math.max(0L, value);
        long delta = normalized - likes;
        likes = normalized;
        if (delta > 0) spawnHearts((int) Math.min(10L, delta));
        invalidate();
    }

    public void clearSession() {
        messages.clear();
        hearts.clear();
        activeGift = null;
        activeGiftUntil = 0L;
        likes = 0L;
        invalidate();
    }

    public void addMessage(ChatMessage message) {
        if (message == null) return;
        messages.addLast(message);
        while (messages.size() > MAX_MESSAGES) messages.removeFirst();
        invalidate();
    }

    public void showGift(GiftEvent event) {
        if (event == null) return;
        activeGift = event;
        activeGiftUntil = SystemClock.uptimeMillis() + 3500L;
        spawnHearts(6);
        invalidate();
    }

    private void spawnHearts(int count) {
        int allowed = Math.max(0, Math.min(count, MAX_HEARTS - hearts.size()));
        float baseX = Math.max(dp(40), getWidth() - dp(54));
        float baseY = Math.max(dp(160), getHeight() - dp(160));
        for (int i = 0; i < allowed; i++) {
            hearts.add(new HeartParticle(
                    baseX + random.nextInt(Math.max(1, (int) dp(24))) - dp(12),
                    baseY + random.nextInt(Math.max(1, (int) dp(18))),
                    0.4f + random.nextFloat() * 0.8f,
                    0.8f + random.nextFloat() * 1.2f,
                    SystemClock.uptimeMillis(),
                    1300L + random.nextInt(900)
            ));
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        drawBottomShade(canvas);
        drawHeader(canvas);
        drawWatermark(canvas);
        drawMessages(canvas);
        drawSideStats(canvas);
        drawGift(canvas, now);
        drawHearts(canvas, now);
    }

    private void drawBottomShade(Canvas canvas) {
        paint.setShader(new LinearGradient(0, getHeight() * 0.48f, 0, getHeight(),
                Color.TRANSPARENT, 0xB8000000, Shader.TileMode.CLAMP));
        canvas.drawRect(0, getHeight() * 0.45f, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }

    private void drawHeader(Canvas canvas) {
        float left = dp(14), top = dp(18), avatar = dp(42);
        float headerWidth = Math.min(dp(184), Math.max(dp(142), getWidth() - dp(142)));
        paint.setColor(0xDD202020);
        canvas.drawRoundRect(new RectF(left, top, left + headerWidth, top + dp(52)), dp(26), dp(26), paint);
        paint.setColor(0xFF6C63FF);
        canvas.drawCircle(left + avatar / 2 + dp(4), top + dp(26), avatar / 2, paint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setFakeBoldText(true);
        canvas.drawText(initials(hostName), left + dp(12), top + dp(32), textPaint);
        canvas.drawText(fitText(hostName, Math.max(dp(55), headerWidth - dp(62))), left + dp(54), top + dp(21), textPaint);
        textPaint.setTextSize(dp(11));
        textPaint.setFakeBoldText(false);
        textPaint.setColor(0xFFE0E0E0);
        canvas.drawText(fitText(title, Math.max(dp(55), headerWidth - dp(62))), left + dp(54), top + dp(41), textPaint);

        float liveLeft = Math.max(left + headerWidth + dp(4), getWidth() - dp(118));
        if (liveLeft + dp(108) > getWidth()) liveLeft = Math.max(dp(8), getWidth() - dp(118));
        paint.setColor(0xFFE33D5F);
        canvas.drawRoundRect(new RectF(liveLeft, top + dp(5), liveLeft + dp(50), top + dp(34)), dp(8), dp(8), paint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(12));
        textPaint.setFakeBoldText(true);
        canvas.drawText("DEMO", liveLeft + dp(9), top + dp(25), textPaint);
        paint.setColor(0xB8000000);
        canvas.drawRoundRect(new RectF(liveLeft + dp(54), top + dp(5), getWidth() - dp(10), top + dp(34)), dp(8), dp(8), paint);
        textPaint.setFakeBoldText(false);
        canvas.drawText(formatCount(viewers), liveLeft + dp(62), top + dp(25), textPaint);
    }

    private void drawWatermark(Canvas canvas) {
        String mark = "LIVE STAGE · SIMULATION";
        textPaint.setTextSize(dp(11));
        textPaint.setFakeBoldText(true);
        mark = fitText(mark, Math.max(dp(80), getWidth() - dp(36)));
        float width = textPaint.measureText(mark);
        float left = Math.max(dp(8), (getWidth() - width) / 2f - dp(10));
        float top = dp(82);
        paint.setColor(0x99000000);
        canvas.drawRoundRect(new RectF(left, top, left + width + dp(20), top + dp(28)), dp(14), dp(14), paint);
        textPaint.setColor(0xFFFFD166);
        canvas.drawText(mark, left + dp(10), top + dp(19), textPaint);
    }

    private void drawMessages(Canvas canvas) {
        float x = dp(14), y = getHeight() - dp(110);
        float maxWidth = Math.max(dp(160), Math.min(getWidth() * 0.76f, getWidth() - dp(76)));
        List<ChatMessage> copy = new ArrayList<>(messages);
        for (int i = copy.size() - 1; i >= 0; i--) {
            ChatMessage message = copy.get(i);
            float boxHeight = message.type == ChatMessage.Type.COMMENT ? dp(46) : dp(34);
            y -= boxHeight + dp(6);
            if (y < dp(120)) break;
            paint.setColor(message.type == ChatMessage.Type.SYSTEM ? 0xA8332D1D : 0x8A000000);
            canvas.drawRoundRect(new RectF(x, y, x + maxWidth, y + boxHeight), dp(12), dp(12), paint);
            int accent = message.type == ChatMessage.Type.FOLLOW ? 0xFFFFD166
                    : message.type == ChatMessage.Type.ENTER ? 0xFF8BD3FF
                    : message.type == ChatMessage.Type.SYSTEM ? 0xFFFFD166 : 0xFFB7A8FF;
            textPaint.setTextSize(dp(12));
            textPaint.setFakeBoldText(true);
            textPaint.setColor(accent);
            canvas.drawText(fitText(message.user, maxWidth - dp(20)), x + dp(10), y + dp(17), textPaint);
            textPaint.setFakeBoldText(false);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(dp(13));
            canvas.drawText(fitText(message.text, maxWidth - dp(20)), x + dp(10),
                    y + (message.type == ChatMessage.Type.COMMENT ? dp(37) : dp(24)), textPaint);
        }
    }

    private void drawSideStats(Canvas canvas) {
        float x = getWidth() - dp(48), y = getHeight() - dp(230);
        paint.setColor(0x88000000);
        canvas.drawCircle(x, y, dp(22), paint);
        textPaint.setColor(0xFFFF5C8A);
        textPaint.setTextSize(dp(25));
        textPaint.setFakeBoldText(false);
        canvas.drawText("♥", x - dp(12), y + dp(9), textPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(11));
        textPaint.setFakeBoldText(true);
        String value = formatCount(likes);
        canvas.drawText(value, x - textPaint.measureText(value) / 2f, y + dp(39), textPaint);
    }

    private void drawGift(Canvas canvas, long now) {
        if (activeGift == null || now > activeGiftUntil) { activeGift = null; return; }
        float progress = 1f - (activeGiftUntil - now) / 3500f;
        float slide = progress < 0.15f ? (1f - progress / 0.15f) * dp(160) : 0f;
        float left = dp(12) - slide, top = getHeight() * 0.30f;
        float right = Math.min(getWidth() - dp(8), left + dp(250));
        paint.setColor(0xD91B1B1B);
        canvas.drawRoundRect(new RectF(left, top, right, top + dp(62)), dp(18), dp(18), paint);
        paint.setColor(0xFFFFC857);
        canvas.drawCircle(left + dp(31), top + dp(31), dp(22), paint);
        textPaint.setColor(0xFF5C3B00);
        textPaint.setTextSize(dp(22));
        textPaint.setFakeBoldText(true);
        canvas.drawText("G", left + dp(23), top + dp(39), textPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(13));
        float available = Math.max(dp(70), right - left - dp(72));
        canvas.drawText(fitText(activeGift.user, available), left + dp(62), top + dp(23), textPaint);
        textPaint.setTextSize(dp(15));
        textPaint.setColor(0xFFFFD166);
        canvas.drawText(fitText("送出 " + activeGift.gift + " × " + activeGift.count, available),
                left + dp(62), top + dp(47), textPaint);
        postInvalidateDelayed(16L);
    }

    private void drawHearts(Canvas canvas, long now) {
        Iterator<HeartParticle> iterator = hearts.iterator();
        boolean active = false;
        while (iterator.hasNext()) {
            HeartParticle heart = iterator.next();
            float t = (now - heart.startedAt) / (float) heart.duration;
            if (t >= 1f) { iterator.remove(); continue; }
            active = true;
            float x = heart.startX + (float) Math.sin(t * 9f) * dp(18) * heart.sway;
            float y = heart.startY - t * dp(150) * heart.speed;
            int alpha = (int) (255 * (1f - t));
            textPaint.setColor((alpha << 24) | 0x00FF5C8A);
            textPaint.setTextSize(dp(22 + 10 * heart.sway));
            textPaint.setFakeBoldText(false);
            canvas.drawText("♥", x, y, textPaint);
        }
        if (active) postInvalidateDelayed(16L);
    }

    private String fitText(String text, float maxWidth) {
        if (text == null || maxWidth <= 0f) return "";
        if (textPaint.measureText(text) <= maxWidth) return text;
        String end = "…";
        int count = textPaint.breakText(text, true,
                Math.max(0f, maxWidth - textPaint.measureText(end)), null);
        return count <= 0 ? end : text.substring(0, Math.min(count, text.length())) + end;
    }

    private String formatCount(long value) {
        if (value >= 1000000L) return String.format(Locale.US, "%.1fM", value / 1000000f);
        if (value >= 1000L) return String.format(Locale.US, "%.1fK", value / 1000f);
        return String.valueOf(value);
    }

    private String initials(String text) {
        if (text == null || text.trim().isEmpty()) return "H";
        return text.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    private static final class HeartParticle {
        final float startX, startY, sway, speed;
        final long startedAt, duration;
        HeartParticle(float startX, float startY, float sway, float speed, long startedAt, long duration) {
            this.startX = startX; this.startY = startY; this.sway = sway; this.speed = speed;
            this.startedAt = startedAt; this.duration = duration;
        }
    }
}
