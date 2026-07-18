package com.example.balloonspopper;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeScreenView extends View {
    public interface Listener {
        void onPlay();

        void onHowToPlay();

        void onParents();

        void onToggleSound();
    }

    private static final String[] SILLY_LINES = {
            "Pop! Pop! Wheee!",
            "Animals go flying!",
            "Ready for silly fun?",
            "Tap tap tap!",
            "Balloons are ticklish!",
            "Whoosh to the sky!",
            "Giggle mode: ON"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<DecorBalloon> decorBalloons = new ArrayList<>();
    private final AppPrefs prefs;
    private final SoundHelper sounds;
    private Listener listener;

    private LinearGradient skyGradient;
    private RectF playButton = new RectF();
    private RectF howButton = new RectF();
    private RectF parentsButton = new RectF();
    private RectF soundButton = new RectF();
    private long startMs;
    private boolean running;

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

    public HomeScreenView(Context context, AppPrefs prefs, SoundHelper sounds) {
        super(context);
        this.prefs = prefs;
        this.sounds = sounds;
        setFocusable(true);
        startMs = SystemClock.uptimeMillis();

        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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
                Color.rgb(120, 210, 255),
                Color.rgb(255, 230, 150),
                Shader.TileMode.CLAMP
        );

        float cx = w / 2f;

        // Size the main CTA from bubbly text so the pink background never clips it.
        float playTextSize = Math.max(48f, w * 0.088f);
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextSize(playTextSize);
        float playTextW = textPaint.measureText("LET'S POP!");
        float playW = Math.min(w * 0.92f, playTextW + w * 0.18f);
        float playH = Math.max(playTextSize * 2.35f, h * 0.11f);
        playButton.set(cx - playW / 2f, h * 0.62f, cx + playW / 2f, h * 0.62f + playH);

        // Bottom row buttons sized to their labels.
        float smallTextSize = Math.max(26f, w * 0.04f);
        textPaint.setTextSize(smallTextSize);
        String[] smallLabels = {"Help?", "Grown-ups", "🔊 Yay!"};
        float maxLabelW = 0f;
        for (String label : smallLabels) {
            maxLabelW = Math.max(maxLabelW, textPaint.measureText(label));
        }
        float smallW = Math.min(w * 0.30f, maxLabelW + w * 0.08f);
        float smallH = Math.max(smallTextSize * 2.3f, h * 0.07f);
        float gap = Math.max(10f, w * 0.018f);
        float rowY = playButton.bottom + Math.max(16f, h * 0.02f);
        float total = smallW * 3f + gap * 2f;
        // Keep row on-screen if needed
        if (total > w - 16f) {
            smallW = (w - 16f - gap * 2f) / 3f;
            total = smallW * 3f + gap * 2f;
        }
        float startX = cx - total / 2f;
        howButton.set(startX, rowY, startX + smallW, rowY + smallH);
        parentsButton.set(startX + smallW + gap, rowY, startX + smallW * 2f + gap, rowY + smallH);
        soundButton.set(startX + smallW * 2f + gap * 2f, rowY, startX + smallW * 3f + gap * 2f, rowY + smallH);

        decorBalloons.clear();
        int[] colors = {
                Color.rgb(255, 105, 140),
                Color.rgb(255, 193, 7),
                Color.rgb(76, 175, 80),
                Color.rgb(66, 165, 245),
                Color.rgb(171, 71, 188),
                Color.rgb(255, 112, 67),
                Color.rgb(0, 188, 212)
        };
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'P', 'Z'};
        // Mix of many animals under decor balloons (-1 = none)
        int[] animalKinds = {0, 4, 7, 11, -1, 14, 16, 18, 5, 9, -1};

        for (int i = 0; i < 11; i++) {
            float radius = randomBetween(w * 0.04f, w * 0.07f);
            float x;
            if (i % 2 == 0) {
                x = randomBetween(radius + 8f, w * 0.34f);
            } else {
                x = randomBetween(w * 0.66f, w - radius - 8f);
            }
            float y = randomBetween(h * 0.10f, h * 0.52f);
            decorBalloons.add(new DecorBalloon(
                    x,
                    y,
                    radius,
                    colors[i % colors.length],
                    letters[i % letters.length],
                    animalKinds[i % animalKinds.length],
                    randomBetween(0.5f, 1.4f),
                    random.nextFloat() * (float) Math.PI * 2f
            ));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float t = (SystemClock.uptimeMillis() - startMs) / 1000f;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(skyGradient);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);

        drawSun(canvas, t);
        drawClouds(canvas, t);
        drawDecorBalloons(canvas, t);
        drawTitle(canvas, t);
        drawSillyLine(canvas, t);
        drawMascot(canvas, t);
        drawStarsBadge(canvas, t);
        drawPlayButton(canvas, t);
        drawSmallButton(canvas, howButton, "Help?", Color.rgb(100, 181, 246), t);
        drawSmallButton(canvas, parentsButton, "Grown-ups", Color.rgb(129, 199, 132), t);
        drawSmallButton(
                canvas,
                soundButton,
                prefs.isSoundEnabled() ? "🔊 Yay!" : "🔇 Shh",
                prefs.isSoundEnabled() ? Color.rgb(255, 183, 77) : Color.rgb(176, 176, 176),
                t
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || listener == null) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();
        if (playButton.contains(x, y)) {
            sounds.playPlay();
            listener.onPlay();
        } else if (howButton.contains(x, y)) {
            sounds.playTap();
            listener.onHowToPlay();
        } else if (parentsButton.contains(x, y)) {
            sounds.playTap();
            listener.onParents();
        } else if (soundButton.contains(x, y)) {
            listener.onToggleSound();
            sounds.playTap();
            invalidate();
        }
        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void drawSun(Canvas canvas, float t) {
        float cx = getWidth() * 0.88f;
        float cy = getHeight() * 0.10f;
        float r = Math.min(getWidth(), getHeight()) * 0.07f;
        float pulse = 1f + (float) Math.sin(t * 2f) * 0.05f;

        paint.setColor(Color.argb(70, 255, 220, 80));
        canvas.drawCircle(cx, cy, r * 1.55f * pulse, paint);
        paint.setColor(Color.rgb(255, 215, 70));
        canvas.drawCircle(cx, cy, r * pulse, paint);

        paint.setColor(Color.rgb(70, 50, 20));
        canvas.drawCircle(cx - r * 0.28f, cy - r * 0.08f, r * 0.08f, paint);
        canvas.drawCircle(cx + r * 0.28f, cy - r * 0.08f, r * 0.08f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Path smile = new Path();
        smile.moveTo(cx - r * 0.28f, cy + r * 0.18f);
        smile.quadTo(cx, cy + r * 0.42f, cx + r * 0.28f, cy + r * 0.18f);
        canvas.drawPath(smile, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawTitle(Canvas canvas, float t) {
        float bounce = (float) Math.sin(t * 2.4f) * 6f;
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                "Balloons Popper!",
                getWidth() / 2f,
                getHeight() * 0.11f + bounce,
                Math.max(69f, getWidth() * 0.132f),
                5f,
                t
        );

        float emojiBounce = (float) Math.sin(t * 3.1f + 1f) * 8f;
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Math.max(36f, getWidth() * 0.075f));
        textPaint.setColor(Color.WHITE);
        canvas.drawText("🎈  🐼  🎈", getWidth() / 2f, getHeight() * 0.185f + emojiBounce, textPaint);
    }

    private void drawSillyLine(Canvas canvas, float t) {
        int index = ((int) (t / 2.4f)) % SILLY_LINES.length;
        float wobble = (float) Math.sin(t * 4f) * 4f;
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                SILLY_LINES[index],
                getWidth() / 2f + wobble,
                getHeight() * 0.235f,
                Math.max(26f, getWidth() * 0.044f),
                2.5f,
                t
        );
    }

    private void drawStarsBadge(Canvas canvas, float t) {
        float textSize = Math.max(34f, getWidth() * 0.052f);
        String label = "Yay! " + prefs.getStars() + " sparkly stars!";

        BubblyText.apply(textPaint, getContext());
        textPaint.setTextSize(textSize);
        float textWidth = textPaint.measureText(label);

        float starSlot = Math.max(72f, getWidth() * 0.14f);
        float sidePad = Math.max(18f, getWidth() * 0.03f);
        float badgeW = Math.min(getWidth() * 0.94f, starSlot + textWidth + sidePad * 2.4f);
        float badgeH = Math.max(starSlot * 0.92f, textSize * 2.15f);
        float left = (getWidth() - badgeW) / 2f;
        float top = getHeight() * 0.265f + (float) Math.sin(t * 1.8f) * 3f;
        RectF badge = new RectF(left, top, left + badgeW, top + badgeH);

        // Soft drop shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 200, 120, 20));
        canvas.drawRoundRect(
                new RectF(badge.left, badge.top + 8f, badge.right, badge.bottom + 8f),
                badgeH / 2f,
                badgeH / 2f,
                paint
        );

        // Warm golden banner sized to the text
        paint.setColor(Color.rgb(255, 236, 150));
        canvas.drawRoundRect(badge, badgeH / 2f, badgeH / 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(5f, badgeH * 0.07f));
        paint.setColor(Color.rgb(255, 170, 40));
        canvas.drawRoundRect(badge, badgeH / 2f, badgeH / 2f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Star chip on the left — kept clear of the label
        float starCx = left + starSlot * 0.52f;
        float starCy = top + badgeH / 2f;
        float starR = badgeH * 0.36f;
        paint.setColor(Color.rgb(255, 196, 30));
        canvas.drawCircle(starCx, starCy, starR, paint);
        paint.setColor(Color.rgb(255, 240, 120));
        canvas.drawCircle(starCx - starR * 0.22f, starCy - starR * 0.22f, starR * 0.34f, paint);
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(starR * 1.2f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("★", starCx, starCy + starR * 0.40f, textPaint);

        // Label centered in the space to the RIGHT of the star chip
        float textAreaLeft = left + starSlot;
        float textAreaCenterX = (textAreaLeft + badge.right) / 2f;
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                label,
                textAreaCenterX,
                top + badgeH * 0.66f,
                textSize,
                2f,
                t
        );
    }

    private void drawPlayButton(Canvas canvas, float t) {
        float pulse = 1f + (float) Math.sin(t * 3.2f) * 0.015f;
        float cx = playButton.centerX();
        float cy = playButton.centerY();
        float halfW = playButton.width() / 2f * pulse;
        float halfH = playButton.height() / 2f * pulse;
        RectF pulsed = new RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 255, 80, 120));
        canvas.drawRoundRect(
                new RectF(pulsed.left, pulsed.top + 10f, pulsed.right, pulsed.bottom + 10f),
                pulsed.height() / 2f,
                pulsed.height() / 2f,
                paint
        );
        paint.setColor(Color.rgb(255, 70, 130));
        canvas.drawRoundRect(pulsed, pulsed.height() / 2f, pulsed.height() / 2f, paint);
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(
                new RectF(pulsed.left + 14f, pulsed.top + 12f, pulsed.right - 14f, pulsed.top + pulsed.height() * 0.42f),
                pulsed.height() / 3f,
                pulsed.height() / 3f,
                paint
        );

        float playTextSize = Math.max(48f, getWidth() * 0.088f);
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                "LET'S POP!",
                pulsed.centerX(),
                pulsed.centerY() + playTextSize * 0.34f,
                playTextSize,
                2f,
                t
        );
    }

    private void drawSmallButton(Canvas canvas, RectF rect, String label, int color, float t) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(55, 0, 0, 0));
        canvas.drawRoundRect(
                new RectF(rect.left, rect.top + 5f, rect.right, rect.bottom + 5f),
                rect.height() / 2.2f,
                rect.height() / 2.2f,
                paint
        );
        paint.setColor(color);
        canvas.drawRoundRect(rect, rect.height() / 2.2f, rect.height() / 2.2f, paint);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(
                new RectF(rect.left + 8f, rect.top + 7f, rect.right - 8f, rect.top + rect.height() * 0.42f),
                rect.height() / 3f,
                rect.height() / 3f,
                paint
        );

        float textSize = Math.max(26f, getWidth() * 0.04f);
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                label,
                rect.centerX(),
                rect.centerY() + textSize * 0.34f,
                textSize,
                1.5f,
                t
        );
    }

    private void drawMascot(Canvas canvas, float t) {
        float x = getWidth() / 2f;
        float y = getHeight() * 0.42f + (float) Math.sin(t * 1.6f) * 12f;
        float s = Math.min(getWidth(), getHeight()) * 0.12f;
        float tilt = (float) Math.sin(t * 1.3f) * 6f;

        canvas.save();
        canvas.rotate(tilt, x, y);

        paint.setColor(Color.rgb(255, 120, 150));
        canvas.drawOval(new RectF(x - s * 0.9f, y - s * 1.2f, x + s * 0.9f, y + s * 0.55f), paint);
        paint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawOval(new RectF(x - s * 0.58f, y - s * 1.0f, x - s * 0.02f, y - s * 0.35f), paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(x - s * 0.30f, y - s * 0.28f, s * 0.20f, paint);
        canvas.drawCircle(x + s * 0.30f, y - s * 0.28f, s * 0.20f, paint);
        paint.setColor(Color.rgb(40, 40, 40));
        canvas.drawCircle(x - s * 0.30f, y - s * 0.24f, s * 0.09f, paint);
        canvas.drawCircle(x + s * 0.30f, y - s * 0.24f, s * 0.09f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x - s * 0.26f, y - s * 0.28f, s * 0.035f, paint);
        canvas.drawCircle(x + s * 0.34f, y - s * 0.28f, s * 0.035f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.09f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(80, 40, 50));
        Path smile = new Path();
        smile.moveTo(x - s * 0.32f, y + s * 0.02f);
        smile.quadTo(x, y + s * 0.34f, x + s * 0.32f, y + s * 0.02f);
        canvas.drawPath(smile, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);

        paint.setColor(Color.argb(130, 255, 140, 160));
        canvas.drawCircle(x - s * 0.52f, y - s * 0.02f, s * 0.14f, paint);
        canvas.drawCircle(x + s * 0.52f, y - s * 0.02f, s * 0.14f, paint);

        // Silly tongue
        paint.setColor(Color.rgb(255, 120, 140));
        canvas.drawOval(new RectF(x - s * 0.10f, y + s * 0.18f, x + s * 0.10f, y + s * 0.38f), paint);

        paint.setColor(Color.rgb(230, 80, 110));
        Path knot = new Path();
        knot.moveTo(x - s * 0.18f, y + s * 0.52f);
        knot.lineTo(x + s * 0.18f, y + s * 0.52f);
        knot.lineTo(x, y + s * 0.82f);
        knot.close();
        canvas.drawPath(knot, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setColor(Color.argb(150, 90, 75, 60));
        Path string = new Path();
        string.moveTo(x, y + s * 0.82f);
        string.cubicTo(x - 22f, y + s * 1.2f, x + 22f, y + s * 1.5f, x, y + s * 1.75f);
        canvas.drawPath(string, paint);
        paint.setStyle(Paint.Style.FILL);

        canvas.restore();

        // Speech bubble
        float bubbleX = x + s * 1.55f;
        float bubbleY = y - s * 0.9f + (float) Math.sin(t * 2.5f) * 5f;
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(
                new RectF(bubbleX - s * 0.95f, bubbleY - s * 0.45f, bubbleX + s * 0.95f, bubbleY + s * 0.35f),
                s * 0.25f,
                s * 0.25f,
                paint
        );
        Path pointer = new Path();
        pointer.moveTo(bubbleX - s * 0.55f, bubbleY + s * 0.2f);
        pointer.lineTo(x + s * 0.7f, y - s * 0.2f);
        pointer.lineTo(bubbleX - s * 0.25f, bubbleY + s * 0.3f);
        pointer.close();
        canvas.drawPath(pointer, paint);

        textPaint.setColor(Color.rgb(255, 90, 130));
        textPaint.setTextSize(Math.max(20f, getWidth() * 0.034f));
        canvas.drawText("Pop me!", bubbleX, bubbleY + s * 0.08f, textPaint);
    }

    private void drawClouds(Canvas canvas, float t) {
        paint.setColor(Color.argb(190, 255, 255, 255));
        drawCloud(canvas, getWidth() * 0.16f + (float) Math.sin(t * 0.35f) * 16f, getHeight() * 0.14f, getWidth() * 0.13f);
        drawCloud(canvas, getWidth() * 0.78f + (float) Math.cos(t * 0.28f) * 14f, getHeight() * 0.20f, getWidth() * 0.15f);
        drawCloud(canvas, getWidth() * 0.48f + (float) Math.sin(t * 0.2f) * 10f, getHeight() * 0.07f, getWidth() * 0.10f);
        drawCloud(canvas, getWidth() * 0.30f, getHeight() * 0.28f, getWidth() * 0.08f);
    }

    private void drawCloud(Canvas canvas, float x, float y, float size) {
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

    private void drawDecorBalloons(Canvas canvas, float t) {
        for (DecorBalloon balloon : decorBalloons) {
            float x = balloon.x + (float) Math.sin(t * balloon.speed + balloon.phase) * 22f;
            float y = balloon.y + (float) Math.cos(t * balloon.speed * 0.85f + balloon.phase) * 16f;
            float sway = (float) Math.sin(t * balloon.speed + balloon.phase) * 4f;

            canvas.save();
            canvas.rotate(sway, x, y);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            paint.setColor(Color.argb(130, 90, 75, 60));
            float stringEnd = y + balloon.radius * (balloon.animalKind >= 0 ? 3.4f : 2.5f);
            canvas.drawLine(x, y + balloon.radius * 1.15f, x, stringEnd, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(balloon.color);
            canvas.drawOval(
                    new RectF(x - balloon.radius, y - balloon.radius * 1.2f, x + balloon.radius, y + balloon.radius * 1.05f),
                    paint
            );
            paint.setColor(Color.argb(100, 255, 255, 255));
            canvas.drawOval(
                    new RectF(x - balloon.radius * 0.55f, y - balloon.radius * 0.9f, x - balloon.radius * 0.05f, y - balloon.radius * 0.2f),
                    paint
            );

            paint.setColor(darken(balloon.color));
            Path knot = new Path();
            knot.moveTo(x - balloon.radius * 0.2f, y + balloon.radius * 0.95f);
            knot.lineTo(x + balloon.radius * 0.2f, y + balloon.radius * 0.95f);
            knot.lineTo(x, y + balloon.radius * 1.3f);
            knot.close();
            canvas.drawPath(knot, paint);

            BubblyText.drawLetter(
                    canvas,
                    textPaint,
                    getContext(),
                    balloon.letter,
                    x,
                    y + balloon.radius * 0.35f,
                    balloon.radius * 1.35f,
                    Color.WHITE
            );

            if (balloon.animalKind >= 0) {
                AnimalSprites.drawByKind(
                        canvas,
                        getContext(),
                        balloon.animalKind,
                        x,
                        y + balloon.radius * 2.55f,
                        balloon.radius * 1.25f,
                        sway * 0.6f,
                        0f
                );
            }

            canvas.restore();
        }
    }

    private int darken(int color) {
        return Color.rgb(
                Math.max(0, (int) (Color.red(color) * 0.72f)),
                Math.max(0, (int) (Color.green(color) * 0.72f)),
                Math.max(0, (int) (Color.blue(color) * 0.72f))
        );
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static final class DecorBalloon {
        final float x;
        final float y;
        final float radius;
        final int color;
        final char letter;
        final int animalKind;
        final float speed;
        final float phase;

        DecorBalloon(float x, float y, float radius, int color, char letter, int animalKind, float speed, float phase) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.letter = letter;
            this.animalKind = animalKind;
            this.speed = speed;
            this.phase = phase;
        }
    }
}
