package com.devendra.balloonspopper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class GameSelectView extends View {
    public interface Listener {
        void onBack();

        void onSelectMode(GameMode mode);

        void onNeedMoreStars(GameMode mode);

        void onUnlocked(GameMode mode);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AppPrefs prefs;
    private final SoundHelper sounds;
    private Listener listener;

    private LinearGradient skyGradient;
    private final RectF[] cardRects = new RectF[GameMode.values().length];
    private final RectF backButton = new RectF();
    private float scrollY;
    private float maxScroll;
    private float lastTouchY;
    private boolean dragging;
    private boolean running;
    private long startMs;
    private String toastMessage;
    private long toastUntilMs;

    private final Runnable animLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            invalidate();
            postDelayed(this, 16L);
        }
    };

    public GameSelectView(Context context, AppPrefs prefs, SoundHelper sounds) {
        super(context);
        this.prefs = prefs;
        this.sounds = sounds;
        setFocusable(true);
        startMs = SystemClock.uptimeMillis();

        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < cardRects.length; i++) {
            cardRects[i] = new RectF();
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void refresh() {
        invalidate();
    }

    public void showToast(String message) {
        toastMessage = message;
        toastUntilMs = System.currentTimeMillis() + 1800L;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        pause();
        super.onDetachedFromWindow();
    }

    public void resume() {
        if (running) {
            return;
        }
        running = true;
        post(animLoop);
    }

    public void pause() {
        running = false;
        removeCallbacks(animLoop);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        skyGradient = new LinearGradient(
                0f, 0f, 0f, h,
                Color.rgb(95, 195, 255),
                Color.rgb(255, 225, 140),
                Shader.TileMode.CLAMP
        );

        float margin = Math.max(20f, w * 0.05f);
        float cardH = Math.max(138f, h * 0.152f);
        float gap = Math.max(16f, h * 0.018f);
        float top = h * 0.24f;

        for (int i = 0; i < GameMode.values().length; i++) {
            float y = top + i * (cardH + gap);
            cardRects[i].set(margin, y, w - margin, y + cardH);
        }

        float contentBottom = cardRects[cardRects.length - 1].bottom + margin + h * 0.08f;
        maxScroll = Math.max(0f, contentBottom - h);
        scrollY = Math.min(scrollY, maxScroll);

        float backSize = Math.max(58f, w * 0.125f);
        backButton.set(margin, h * 0.03f, margin + backSize, h * 0.03f + backSize * 0.78f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float t = (SystemClock.uptimeMillis() - startMs) / 1000f;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(skyGradient);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);

        drawHills(canvas);
        drawSun(canvas, t);
        drawCloud(canvas, getWidth() * 0.18f + (float) Math.sin(t * 0.35f) * 14f, getHeight() * 0.09f, getWidth() * 0.11f);
        drawCloud(canvas, getWidth() * 0.72f + (float) Math.cos(t * 0.28f) * 12f, getHeight() * 0.14f, getWidth() * 0.13f);
        drawMiniBalloons(canvas, t);

        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                "Pick a Game",
                getWidth() / 2f,
                getHeight() * 0.115f,
                Math.max(64f, getWidth() * 0.115f),
                4f,
                t
        );
        drawStarsBadge(canvas, t);

        canvas.save();
        canvas.translate(0f, -scrollY);
        GameMode[] modes = GameMode.values();
        for (int i = 0; i < modes.length; i++) {
            drawCard(canvas, modes[i], cardRects[i], t + i * 0.35f);
        }
        canvas.restore();

        drawBackButton(canvas, t);
        drawToast(canvas);
    }

    private void drawHills(Canvas canvas) {
        paint.setColor(Color.rgb(120, 210, 110));
        Path hill = new Path();
        hill.moveTo(0f, getHeight());
        hill.lineTo(0f, getHeight() * 0.90f);
        hill.quadTo(getWidth() * 0.3f, getHeight() * 0.84f, getWidth() * 0.55f, getHeight() * 0.91f);
        hill.quadTo(getWidth() * 0.8f, getHeight() * 0.96f, getWidth(), getHeight() * 0.88f);
        hill.lineTo(getWidth(), getHeight());
        hill.close();
        canvas.drawPath(hill, paint);
    }

    private void drawSun(Canvas canvas, float t) {
        float cx = getWidth() * 0.88f;
        float cy = getHeight() * 0.08f;
        float r = Math.min(getWidth(), getHeight()) * 0.055f;
        float pulse = 1f + (float) Math.sin(t * 2f) * 0.05f;
        paint.setColor(Color.argb(70, 255, 230, 90));
        canvas.drawCircle(cx, cy, r * 1.55f * pulse, paint);
        paint.setColor(Color.rgb(255, 220, 70));
        canvas.drawCircle(cx, cy, r * pulse, paint);
    }

    private void drawMiniBalloons(Canvas canvas, float t) {
        int[] colors = {
                Color.rgb(255, 105, 140),
                Color.rgb(80, 200, 120),
                Color.rgb(70, 170, 250)
        };
        for (int i = 0; i < 3; i++) {
            float x = getWidth() * (0.12f + i * 0.12f) + (float) Math.sin(t * 1.2f + i) * 10f;
            float y = getHeight() * (0.055f + i * 0.012f) + (float) Math.cos(t * 1.4f + i) * 6f;
            float r = getWidth() * 0.028f;
            paint.setColor(colors[i]);
            canvas.drawOval(new RectF(x - r, y - r * 1.15f, x + r, y + r), paint);
            paint.setColor(Color.argb(120, 255, 255, 255));
            canvas.drawOval(new RectF(x - r * 0.55f, y - r * 0.85f, x - r * 0.1f, y - r * 0.25f), paint);
        }
    }

    private void drawStarsBadge(Canvas canvas, float t) {
        String label = prefs.getStars() + " Stars";
        float textSize = Math.max(32f, getWidth() * 0.05f);
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextSize(textSize);
        float textW = textPaint.measureText(label);
        float starSlot = Math.max(52f, getWidth() * 0.1f);
        float badgeW = Math.min(getWidth() * 0.72f, starSlot + textW + getWidth() * 0.08f);
        float badgeH = Math.max(54f, textSize * 1.85f);
        float left = (getWidth() - badgeW) / 2f;
        float top = getHeight() * 0.15f + (float) Math.sin(t * 1.8f) * 2.5f;
        RectF badge = new RectF(left, top, left + badgeW, top + badgeH);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 200, 120, 20));
        canvas.drawRoundRect(
                new RectF(badge.left, badge.top + 5f, badge.right, badge.bottom + 5f),
                badgeH / 2f,
                badgeH / 2f,
                paint
        );
        paint.setColor(Color.rgb(255, 236, 150));
        canvas.drawRoundRect(badge, badgeH / 2f, badgeH / 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(4f, badgeH * 0.07f));
        paint.setColor(Color.rgb(255, 170, 40));
        canvas.drawRoundRect(badge, badgeH / 2f, badgeH / 2f, paint);
        paint.setStyle(Paint.Style.FILL);

        float starCx = left + starSlot * 0.52f;
        float starCy = top + badgeH / 2f;
        float starR = badgeH * 0.34f;
        paint.setColor(Color.rgb(255, 196, 30));
        canvas.drawCircle(starCx, starCy, starR, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(starR * 1.15f);
        canvas.drawText("★", starCx, starCy + starR * 0.38f, textPaint);

        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                label,
                (left + starSlot + badge.right) / 2f,
                top + badgeH * 0.66f,
                textSize,
                2f,
                t
        );
    }

    private void drawBackButton(Canvas canvas, float t) {
        float pulse = 1f + (float) Math.sin(t * 2.8f) * 0.02f;
        float cx = backButton.centerX();
        float cy = backButton.centerY();
        float hw = backButton.width() / 2f * pulse;
        float hh = backButton.height() / 2f * pulse;
        RectF pulsed = new RectF(cx - hw, cy - hh, cx + hw, cy + hh);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(60, 255, 90, 130));
        canvas.drawRoundRect(
                new RectF(pulsed.left, pulsed.top + 5f, pulsed.right, pulsed.bottom + 5f),
                pulsed.height() / 2f,
                pulsed.height() / 2f,
                paint
        );
        paint.setColor(Color.rgb(255, 95, 145));
        canvas.drawRoundRect(pulsed, pulsed.height() / 2f, pulsed.height() / 2f, paint);
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(
                new RectF(pulsed.left + 8f, pulsed.top + 7f, pulsed.right - 8f, pulsed.top + pulsed.height() * 0.42f),
                pulsed.height() / 3f,
                pulsed.height() / 3f,
                paint
        );

        // Thick custom back arrow (clearer than a thin text glyph)
        float arrowCx = pulsed.centerX();
        float arrowCy = pulsed.centerY();
        float tipX = arrowCx - pulsed.width() * 0.18f;
        float shaftEndX = arrowCx + pulsed.width() * 0.16f;
        float headSpread = pulsed.height() * 0.22f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(8f, pulsed.height() * 0.16f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.WHITE);
        canvas.drawLine(tipX, arrowCy, shaftEndX, arrowCy, paint);
        Path head = new Path();
        head.moveTo(tipX + headSpread * 0.95f, arrowCy - headSpread);
        head.lineTo(tipX, arrowCy);
        head.lineTo(tipX + headSpread * 0.95f, arrowCy + headSpread);
        canvas.drawPath(head, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawToast(Canvas canvas) {
        if (toastMessage == null || System.currentTimeMillis() >= toastUntilMs) {
            toastMessage = null;
            return;
        }
        float toastW = Math.min(getWidth() * 0.86f, 480f);
        float toastH = Math.max(64f, getHeight() * 0.07f);
        RectF toast = new RectF(
                (getWidth() - toastW) / 2f,
                getHeight() * 0.88f,
                (getWidth() + toastW) / 2f,
                getHeight() * 0.88f + toastH
        );
        paint.setColor(Color.argb(230, 40, 70, 110));
        canvas.drawRoundRect(toast, toastH / 2f, toastH / 2f, paint);
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                toastMessage,
                toast.centerX(),
                toast.centerY() + Math.max(30f, getWidth() * 0.045f) * 0.28f,
                Math.max(28f, getWidth() * 0.042f),
                0f,
                0f
        );
        postInvalidateDelayed(50L);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = y;
                dragging = false;
                if (backButton.contains(x, y)) {
                    sounds.playTap();
                    if (listener != null) {
                        listener.onBack();
                    }
                    performClick();
                    return true;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dy = lastTouchY - y;
                if (Math.abs(dy) > 8f || dragging) {
                    dragging = true;
                    scrollY = clamp(scrollY + dy, 0f, maxScroll);
                    lastTouchY = y;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    float contentY = y + scrollY;
                    GameMode[] modes = GameMode.values();
                    for (int i = 0; i < modes.length; i++) {
                        if (cardRects[i].contains(x, contentY)) {
                            handleCardTap(modes[i]);
                            break;
                        }
                    }
                }
                performClick();
                return true;

            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void handleCardTap(GameMode mode) {
        if (listener == null) {
            return;
        }

        if (prefs.isUnlocked(mode)) {
            sounds.playPlay();
            listener.onSelectMode(mode);
            return;
        }

        if (prefs.unlock(mode)) {
            sounds.playUnlock();
            listener.onUnlocked(mode);
            invalidate();
            return;
        }

        sounds.playLocked();
        listener.onNeedMoreStars(mode);
    }

    private void drawCard(Canvas canvas, GameMode mode, RectF rect, float t) {
        boolean unlocked = prefs.isUnlocked(mode);
        float bob = (float) Math.sin(t * 2.2f) * 2.5f;
        RectF card = new RectF(rect.left, rect.top + bob, rect.right, rect.bottom + bob);
        float radius = card.height() / 2f;

        // Soft shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(45, 30, 60, 90));
        canvas.drawRoundRect(
                new RectF(card.left, card.top + 8f, card.right, card.bottom + 8f),
                radius,
                radius,
                paint
        );

        // Soft tinted banner body from mode accent (fully rounded ends)
        int accent = mode.accentColor;
        paint.setColor(Color.rgb(
                Math.min(255, Color.red(accent) + 90),
                Math.min(255, Color.green(accent) + 90),
                Math.min(255, Color.blue(accent) + 90)
        ));
        canvas.drawRoundRect(card, radius, radius, paint);

        // Cream face panel — keep ends fully round
        float inset = 7f;
        RectF inner = new RectF(card.left + inset, card.top + inset, card.right - inset, card.bottom - inset);
        float innerRadius = inner.height() / 2f;
        paint.setColor(Color.argb(245, 255, 252, 245));
        canvas.drawRoundRect(inner, innerRadius, innerRadius, paint);

        // Colored end cap on the left (rounded pill tip)
        paint.setColor(accent);
        canvas.drawRoundRect(
                new RectF(inner.left, inner.top, inner.left + inner.height() * 0.55f, inner.bottom),
                innerRadius,
                innerRadius,
                paint
        );

        float iconSize = card.height() * 0.56f;
        RectF icon = new RectF(
                card.left + card.height() * 0.22f,
                card.centerY() - iconSize / 2f,
                card.left + card.height() * 0.22f + iconSize,
                card.centerY() + iconSize / 2f
        );
        paint.setColor(accent);
        canvas.drawRoundRect(icon, icon.height() / 2f, icon.height() / 2f, paint);
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(
                new RectF(icon.left + 8f, icon.top + 8f, icon.right - 8f, icon.top + icon.height() * 0.42f),
                icon.height() / 3f,
                icon.height() / 3f,
                paint
        );
        drawModeIcon(canvas, mode, icon);

        float textLeft = icon.right + 16f;
        float textRight = card.right - card.height() * 0.35f;
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setStyle(Paint.Style.FILL);
        float titleSize = Math.max(38f, getWidth() * 0.058f);
        textPaint.setTextSize(titleSize);
        float tx = textLeft;
        String title = mode.title;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            if (ch.charAt(0) != ' ') {
                textPaint.setColor(BubblyText.candyColor(i));
                canvas.drawText(ch, tx, card.top + card.height() * 0.42f, textPaint);
            }
            tx += textPaint.measureText(ch);
        }

        textPaint.setTextSize(Math.max(24f, getWidth() * 0.038f));
        textPaint.setColor(Color.rgb(90, 105, 130));
        float blurbMaxWidth = Math.max(40f, textRight - textLeft);
        float blurbStartY = card.top + card.height() * 0.62f;
        float blurbLineHeight = textPaint.getTextSize() * 1.15f;
        drawWrappedBlurb(canvas, mode.blurb, textLeft, blurbStartY, blurbMaxWidth, blurbLineHeight, 2);

        if (unlocked) {
            // Play chevron chip
            float chipR = card.height() * 0.18f;
            float chipCx = card.right - card.height() * 0.32f;
            float chipCy = card.centerY();
            paint.setColor(accent);
            canvas.drawCircle(chipCx, chipCy, chipR, paint);
            paint.setColor(Color.WHITE);
            Path play = new Path();
            play.moveTo(chipCx - chipR * 0.25f, chipCy - chipR * 0.4f);
            play.lineTo(chipCx + chipR * 0.4f, chipCy);
            play.lineTo(chipCx - chipR * 0.25f, chipCy + chipR * 0.4f);
            play.close();
            canvas.drawPath(play, paint);
        } else {
            paint.setColor(Color.argb(150, 255, 255, 255));
            canvas.drawRoundRect(card, radius, radius, paint);

            float lockCx = textRight;
            float lockCy = card.centerY() - 6f;
            paint.setColor(Color.rgb(255, 196, 30));
            canvas.drawRoundRect(new RectF(lockCx - 22f, lockCy - 6f, lockCx + 22f, lockCy + 24f), 12f, 12f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(Color.rgb(230, 150, 30));
            canvas.drawCircle(lockCx, lockCy - 10f, 13f, paint);
            paint.setStyle(Paint.Style.FILL);

            BubblyText.apply(textPaint, getContext());
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.rgb(120, 80, 20));
            textPaint.setTextSize(Math.max(24f, getWidth() * 0.036f));
            canvas.drawText("★ " + mode.starCost, lockCx, card.bottom - 18f, textPaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawWrappedBlurb(
            Canvas canvas,
            String text,
            float x,
            float startY,
            float maxWidth,
            float lineHeight,
            int maxLines
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (textPaint.measureText(candidate) <= maxWidth || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            canvas.drawText(line.toString(), x, startY + lineIndex * lineHeight, textPaint);
            lineIndex++;
            if (lineIndex >= maxLines - 1) {
                // Last line: pack remaining words, truncate with ellipsis if needed
                StringBuilder rest = new StringBuilder(word);
                for (int j = i + 1; j < words.length; j++) {
                    rest.append(' ').append(words[j]);
                }
                String last = rest.toString();
                while (last.length() > 1 && textPaint.measureText(last + "…") > maxWidth) {
                    last = last.substring(0, last.length() - 1).trim();
                }
                if (textPaint.measureText(last) > maxWidth || rest.toString().length() > last.length()) {
                    last = last + "…";
                }
                canvas.drawText(last, x, startY + lineIndex * lineHeight, textPaint);
                return;
            }
            line.setLength(0);
            line.append(word);
        }
        if (line.length() > 0 && lineIndex < maxLines) {
            canvas.drawText(line.toString(), x, startY + lineIndex * lineHeight, textPaint);
        }
    }

    private void drawModeIcon(Canvas canvas, GameMode mode, RectF icon) {
        float cx = icon.centerX();
        float cy = icon.centerY();
        float s = icon.width() * 0.22f;
        paint.setColor(Color.WHITE);
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextAlign(Paint.Align.CENTER);

        switch (mode) {
            case BALLOON_POP:
                canvas.drawOval(new RectF(cx - s, cy - s * 1.25f, cx + s, cy + s * 0.9f), paint);
                paint.setColor(Color.argb(120, 255, 255, 255));
                canvas.drawOval(new RectF(cx - s * 0.55f, cy - s * 0.95f, cx - s * 0.05f, cy - s * 0.25f), paint);
                break;
            case COLOR_POP:
                paint.setColor(Color.rgb(255, 230, 230));
                canvas.drawCircle(cx - s * 0.7f, cy, s * 0.7f, paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(cx + s * 0.55f, cy, s * 0.85f, paint);
                paint.setColor(Color.rgb(255, 210, 80));
                canvas.drawCircle(cx, cy + s * 0.55f, s * 0.55f, paint);
                break;
            case NUMBER_POP:
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(icon.width() * 0.38f);
                canvas.drawText("123", cx, cy + textPaint.getTextSize() * 0.32f, textPaint);
                break;
            case LETTER_POP:
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(icon.width() * 0.52f);
                canvas.drawText("A", cx, cy + textPaint.getTextSize() * 0.32f, textPaint);
                break;
            case SHAPE_POP:
                drawStar(canvas, cx, cy, s * 1.15f, Color.WHITE);
                break;
            case SPEED_CHALLENGE:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6f);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, s * 1.1f, paint);
                canvas.drawLine(cx, cy, cx, cy - s * 0.7f, paint);
                canvas.drawLine(cx, cy, cx + s * 0.55f, cy + s * 0.2f, paint);
                paint.setStyle(Paint.Style.FILL);
                break;
            default:
                break;
        }
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius, int color) {
        paint.setColor(color);
        Path star = new Path();
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(-90 + i * 72);
            double b = Math.toRadians(-90 + i * 72 + 36);
            float x1 = cx + (float) Math.cos(a) * radius;
            float y1 = cy + (float) Math.sin(a) * radius;
            float x2 = cx + (float) Math.cos(b) * radius * 0.45f;
            float y2 = cy + (float) Math.sin(b) * radius * 0.45f;
            if (i == 0) {
                star.moveTo(x1, y1);
            } else {
                star.lineTo(x1, y1);
            }
            star.lineTo(x2, y2);
        }
        star.close();
        canvas.drawPath(star, paint);
    }

    private void drawCloud(Canvas canvas, float x, float y, float size) {
        paint.setColor(Color.argb(200, 255, 255, 255));
        canvas.drawCircle(x - size * 0.35f, y, size * 0.42f, paint);
        canvas.drawCircle(x, y - size * 0.15f, size * 0.55f, paint);
        canvas.drawCircle(x + size * 0.42f, y, size * 0.46f, paint);
        canvas.drawRoundRect(
                new RectF(x - size * 0.72f, y, x + size * 0.72f, y + size * 0.38f),
                size * 0.18f,
                size * 0.18f,
                paint
        );
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
