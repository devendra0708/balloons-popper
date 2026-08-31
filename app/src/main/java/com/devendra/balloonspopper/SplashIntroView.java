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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Playful opening screen: big cartoon animals, alphabet balloons that pop, then home.
 */
public class SplashIntroView extends View {
    public interface Listener {
        void onFinished();
    }

    private static final long INTRO_MS = 5200L;
    private static final char[] LETTERS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<FloatBalloon> balloons = new ArrayList<>();
    private final List<Burst> bursts = new ArrayList<>();
    private final List<BounceAnimal> animals = new ArrayList<>();
    private final SoundHelper sounds;

    private Listener listener;
    private LinearGradient skyGradient;
    private long startMs;
    private boolean running;
    private boolean finished;
    private int nextPopIndex;
    private long nextPopAtMs;
    private float titleAlpha;
    private float subtitleAlpha;

    private final Runnable animLoop = new Runnable() {
        @Override
        public void run() {
            if (!running || finished) {
                return;
            }
            tick();
            invalidate();
            postDelayed(this, 16L);
        }
    };

    public SplashIntroView(Context context, SoundHelper sounds) {
        super(context);
        this.sounds = sounds;
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        finished = false;
        startMs = SystemClock.uptimeMillis();
        nextPopIndex = 0;
        nextPopAtMs = startMs + 550L;
        running = true;
        post(animLoop);
    }

    public void stop() {
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
        spawnScene(w, h);
        startMs = SystemClock.uptimeMillis();
        nextPopAtMs = startMs + 550L;
        nextPopIndex = 0;
    }

    private void spawnScene(int w, int h) {
        balloons.clear();
        bursts.clear();
        animals.clear();

        int[] colors = {
                Color.rgb(255, 95, 140),
                Color.rgb(255, 200, 50),
                Color.rgb(80, 200, 120),
                Color.rgb(70, 170, 250),
                Color.rgb(255, 130, 70),
                Color.rgb(255, 110, 190),
                Color.rgb(120, 220, 230),
                Color.rgb(180, 120, 255)
        };

        // Big alphabet balloons rising from below
        for (int i = 0; i < 10; i++) {
            float radius = randomBetween(w * 0.065f, w * 0.095f);
            float lane = (i + 0.5f) / 10f;
            float x = w * lane + randomBetween(-w * 0.03f, w * 0.03f);
            x = clamp(x, radius + 8f, w - radius - 8f);
            float y = h + radius + randomBetween(20f, h * 0.7f);
            int animalHang = (i % 3 == 0) ? (i * 3) % AnimalSprites.ALL_NAMES.length : -1;
            balloons.add(new FloatBalloon(
                    x,
                    y,
                    radius,
                    colors[i % colors.length],
                    LETTERS[i % LETTERS.length],
                    randomBetween(110f, 175f),
                    randomBetween(0.9f, 1.8f),
                    random.nextFloat() * (float) Math.PI * 2f,
                    animalHang
            ));
        }

        // Large bouncing animals along the grass (mix of the full cast)
        float ground = h * 0.80f;
        float animalSize = Math.min(w, h) * 0.12f;
        int bounceCount = 5;
        for (int i = 0; i < bounceCount; i++) {
            float x = w * (0.10f + i * 0.20f);
            int kind = (i * 4 + random.nextInt(3)) % AnimalSprites.ALL_NAMES.length;
            animals.add(new BounceAnimal(
                    x,
                    ground,
                    animalSize,
                    kind,
                    randomBetween(1.5f, 2.3f),
                    random.nextFloat() * (float) Math.PI * 2f
            ));
        }
    }

    private void tick() {
        long now = SystemClock.uptimeMillis();
        float elapsed = (now - startMs) / 1000f;
        float dt = 0.016f;

        titleAlpha = clamp((elapsed - 0.2f) / 0.55f, 0f, 1f);
        subtitleAlpha = clamp((elapsed - 0.75f) / 0.45f, 0f, 1f);

        for (FloatBalloon b : balloons) {
            if (b.popped) {
                continue;
            }
            b.y -= b.riseSpeed * dt;
            b.x += (float) Math.sin(elapsed * b.wobble + b.phase) * 22f * dt;
            b.sway = (float) Math.sin(elapsed * 2.2f + b.phase) * 8f;
        }

        if (nextPopIndex < balloons.size() && now >= nextPopAtMs) {
            FloatBalloon target = balloons.get(nextPopIndex);
            if (!target.popped && target.y < getHeight() * 0.68f) {
                popBalloon(target);
                nextPopIndex++;
                nextPopAtMs = now + (nextPopIndex < 5 ? 340L : 260L);
            } else if (!target.popped) {
                nextPopAtMs = now + 70L;
            } else {
                nextPopIndex++;
            }
        }

        Iterator<Burst> it = bursts.iterator();
        while (it.hasNext()) {
            Burst burst = it.next();
            burst.age += dt;
            for (Spark s : burst.sparks) {
                s.x += s.vx * dt;
                s.y += s.vy * dt;
                s.vy += 420f * dt;
                s.life -= dt;
                s.rotation += s.spin * dt;
            }
            if (burst.age > 0.95f) {
                it.remove();
            }
        }

        for (BounceAnimal a : animals) {
            float bounce = Math.abs((float) Math.sin(elapsed * a.bounceSpeed + a.phase));
            a.y = a.baseY - bounce * a.size * 1.45f;
            a.tilt = (float) Math.sin(elapsed * a.bounceSpeed * 1.3f + a.phase) * 12f;
            a.arm = (float) Math.sin(elapsed * a.bounceSpeed * 2.2f + a.phase);
        }

        if (!finished && now - startMs >= INTRO_MS) {
            finishIntro();
        }
    }

    private void popBalloon(FloatBalloon balloon) {
        balloon.popped = true;
        bursts.add(createBurst(balloon.x, balloon.y, balloon.color, balloon.radius, balloon.letter));
        if (sounds != null) {
            sounds.playSuccess();
            if (balloon.animalKind >= 0) {
                String name = AnimalSprites.nameForKind(balloon.animalKind);
                boolean played = sounds.playAnimal(name);
                if (!played) {
                    sounds.playSuccess();
                }
            }
        }
    }

    private Burst createBurst(float x, float y, int color, float radius, char letter) {
        List<Spark> sparks = new ArrayList<>();
        int count = 14 + random.nextInt(8);
        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.PI * 2f * i / count + random.nextFloat() * 0.35f);
            float speed = randomBetween(140f, 360f);
            boolean isLetter = i == 0 || (i % 5 == 0);
            sparks.add(new Spark(
                    x,
                    y,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed - randomBetween(50f, 140f),
                    randomBetween(radius * 0.14f, radius * 0.32f),
                    color,
                    randomBetween(0.4f, 0.8f),
                    isLetter ? letter : 0,
                    randomBetween(-180f, 180f)
            ));
        }
        return new Burst(sparks, 0f);
    }

    private void finishIntro() {
        if (finished) {
            return;
        }
        finished = true;
        stop();
        if (listener != null) {
            listener.onFinished();
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

        drawHills(canvas);
        drawSun(canvas, t);
        drawClouds(canvas, t);

        for (FloatBalloon balloon : balloons) {
            if (!balloon.popped) {
                drawBalloon(canvas, balloon, t);
            }
        }

        for (Burst burst : bursts) {
            drawBurst(canvas, burst);
        }

        for (BounceAnimal animal : animals) {
            drawSplashAnimal(canvas, animal, t);
        }

        drawTitle(canvas, t);
        drawTapHint(canvas, t);
    }

    private void drawHills(Canvas canvas) {
        paint.setColor(Color.rgb(120, 210, 110));
        Path hill = new Path();
        hill.moveTo(0f, getHeight());
        hill.lineTo(0f, getHeight() * 0.82f);
        hill.quadTo(getWidth() * 0.25f, getHeight() * 0.72f, getWidth() * 0.5f, getHeight() * 0.80f);
        hill.quadTo(getWidth() * 0.75f, getHeight() * 0.88f, getWidth(), getHeight() * 0.78f);
        hill.lineTo(getWidth(), getHeight());
        hill.close();
        canvas.drawPath(hill, paint);

        paint.setColor(Color.rgb(95, 190, 95));
        Path hill2 = new Path();
        hill2.moveTo(0f, getHeight());
        hill2.lineTo(0f, getHeight() * 0.90f);
        hill2.quadTo(getWidth() * 0.35f, getHeight() * 0.84f, getWidth() * 0.7f, getHeight() * 0.92f);
        hill2.quadTo(getWidth() * 0.88f, getHeight() * 0.96f, getWidth(), getHeight() * 0.90f);
        hill2.lineTo(getWidth(), getHeight());
        hill2.close();
        canvas.drawPath(hill2, paint);
    }

    private void drawSun(Canvas canvas, float t) {
        float cx = getWidth() * 0.88f;
        float cy = getHeight() * 0.11f;
        float r = Math.min(getWidth(), getHeight()) * 0.08f;
        float pulse = 1f + (float) Math.sin(t * 2.2f) * 0.06f;
        paint.setColor(Color.argb(80, 255, 230, 90));
        canvas.drawCircle(cx, cy, r * 1.6f * pulse, paint);
        paint.setColor(Color.rgb(255, 220, 70));
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

    private void drawClouds(Canvas canvas, float t) {
        paint.setColor(Color.argb(210, 255, 255, 255));
        drawCloud(canvas, getWidth() * 0.16f + (float) Math.sin(t * 0.4f) * 20f, getHeight() * 0.13f, getWidth() * 0.12f);
        drawCloud(canvas, getWidth() * 0.48f + (float) Math.cos(t * 0.32f) * 16f, getHeight() * 0.07f, getWidth() * 0.10f);
        drawCloud(canvas, getWidth() * 0.70f + (float) Math.sin(t * 0.28f) * 12f, getHeight() * 0.20f, getWidth() * 0.11f);
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

    private void drawBalloon(Canvas canvas, FloatBalloon balloon, float t) {
        float x = balloon.x;
        float y = balloon.y;
        float r = balloon.radius;
        float bob = balloon.animalKind >= 0 ? (float) Math.sin(t * 3f + balloon.phase) * r * 0.08f : 0f;

        canvas.save();
        canvas.rotate(balloon.sway, x, y);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2.5f, r * 0.05f));
        paint.setColor(Color.argb(150, 90, 75, 60));
        float stringEnd = y + r * (balloon.animalKind >= 0 ? 3.5f : 2.5f);
        canvas.drawLine(x, y + r * 1.15f, x, stringEnd + bob, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(balloon.color);
        canvas.drawOval(new RectF(x - r, y - r * 1.2f, x + r, y + r * 1.05f), paint);
        paint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawOval(new RectF(x - r * 0.55f, y - r * 0.9f, x - r * 0.05f, y - r * 0.2f), paint);

        paint.setColor(darken(balloon.color));
        Path knot = new Path();
        knot.moveTo(x - r * 0.22f, y + r * 0.95f);
        knot.lineTo(x + r * 0.22f, y + r * 0.95f);
        knot.lineTo(x, y + r * 1.32f);
        knot.close();
        canvas.drawPath(knot, paint);

        // Big bubbly alphabet letter on balloon
        BubblyText.drawLetter(
                canvas,
                textPaint,
                getContext(),
                balloon.letter,
                x,
                y + r * 0.38f,
                r * 1.45f,
                Color.WHITE
        );

        if (balloon.animalKind >= 0) {
            drawHangingAnimal(canvas, x, y + r * 2.55f + bob, r * 0.95f, balloon.animalKind, t + balloon.phase);
        }

        canvas.restore();
    }

    private void drawHangingAnimal(Canvas canvas, float x, float y, float size, int kind, float t) {
        float tilt = (float) Math.sin(t * 2.5f) * 8f;
        AnimalSprites.drawByKind(canvas, getContext(), kind, x, y, size * 1.15f, tilt, (float) Math.sin(t * 3f));
    }

    private void drawBurst(Canvas canvas, Burst burst) {
        for (Spark spark : burst.sparks) {
            if (spark.life <= 0f) {
                continue;
            }
            int alpha = (int) (255f * clamp(spark.life / 0.55f, 0f, 1f));
            if (spark.letter != 0) {
                canvas.save();
                canvas.rotate(spark.rotation, spark.x, spark.y);
                BubblyText.drawLetter(
                        canvas,
                        textPaint,
                        getContext(),
                        spark.letter,
                        spark.x,
                        spark.y + spark.size,
                        spark.size * 3.2f,
                        Color.argb(alpha, Color.red(spark.color), Color.green(spark.color), Color.blue(spark.color))
                );
                canvas.restore();
            } else {
                paint.setColor(Color.argb(alpha, Color.red(spark.color), Color.green(spark.color), Color.blue(spark.color)));
                canvas.drawCircle(spark.x, spark.y, spark.size, paint);
            }
        }
    }

    private void drawSplashAnimal(Canvas canvas, BounceAnimal animal, float t) {
        float x = animal.x;
        float y = animal.y;
        float s = animal.size;

        // Soft ground shadow
        paint.setColor(Color.argb(55, 40, 80, 40));
        float shadowScale = 1f - clamp((animal.baseY - animal.y) / (s * 1.45f), 0f, 0.75f);
        canvas.drawOval(
                new RectF(
                        x - s * 0.7f * shadowScale,
                        animal.baseY + s * 0.85f,
                        x + s * 0.7f * shadowScale,
                        animal.baseY + s * 1.05f
                ),
                paint
        );

        AnimalSprites.drawByKind(
                canvas,
                getContext(),
                animal.kind,
                x,
                y,
                s * 1.25f,
                animal.tilt,
                animal.arm
        );
    }

    private void drawTitle(Canvas canvas, float t) {
        if (titleAlpha <= 0.01f) {
            return;
        }
        float bounce = (float) Math.sin(t * 2.6f) * 6f;
        canvas.save();
        // Soft fade by layering — draw fully then rely on early skip for alpha
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                "Balloons Popper!",
                getWidth() / 2f,
                getHeight() * 0.24f + bounce,
                Math.max(46f, getWidth() * 0.09f),
                5f,
                t
        );

        if (subtitleAlpha > 0.05f) {
            float wobble = (float) Math.sin(t * 3.5f) * 4f;
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    "A B C  Pop Pop Yay!",
                    getWidth() / 2f + wobble,
                    getHeight() * 0.305f,
                    Math.max(26f, getWidth() * 0.045f),
                    3f,
                    t + 1f
            );
        }
        canvas.restore();
    }

    private void drawTapHint(Canvas canvas, float t) {
        if (t < 1.1f) {
            return;
        }
        float pulse = 0.65f + 0.35f * (0.5f + 0.5f * (float) Math.sin(t * 4f));
        BubblyText.apply(textPaint, getContext());
        textPaint.setColor(Color.argb((int) (200 * pulse), 90, 110, 170));
        textPaint.setTextSize(Math.max(22f, getWidth() * 0.036f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Tap to skip", getWidth() / 2f, getHeight() * 0.945f, textPaint);
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

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !finished) {
            if (sounds != null) {
                sounds.playTap();
            }
            finishIntro();
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static final class FloatBalloon {
        float x;
        float y;
        final float radius;
        final int color;
        final char letter;
        final float riseSpeed;
        final float wobble;
        final float phase;
        final int animalKind;
        float sway;
        boolean popped;

        FloatBalloon(float x, float y, float radius, int color, char letter, float riseSpeed, float wobble, float phase, int animalKind) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.letter = letter;
            this.riseSpeed = riseSpeed;
            this.wobble = wobble;
            this.phase = phase;
            this.animalKind = animalKind;
        }
    }

    private static final class Spark {
        float x;
        float y;
        float vx;
        float vy;
        final float size;
        final int color;
        float life;
        final char letter;
        float rotation;
        final float spin;

        Spark(float x, float y, float vx, float vy, float size, int color, float life, char letter, float spin) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.letter = letter;
            this.spin = spin;
        }
    }

    private static final class Burst {
        final List<Spark> sparks;
        float age;

        Burst(List<Spark> sparks, float age) {
            this.sparks = sparks;
            this.age = age;
        }
    }

    private static final class BounceAnimal {
        final float x;
        final float baseY;
        float y;
        final float size;
        final int kind;
        final float bounceSpeed;
        final float phase;
        float tilt;
        float arm;

        BounceAnimal(float x, float baseY, float size, int kind, float bounceSpeed, float phase) {
            this.x = x;
            this.baseY = baseY;
            this.y = baseY;
            this.size = size;
            this.kind = kind;
            this.bounceSpeed = bounceSpeed;
            this.phase = phase;
        }
    }
}
