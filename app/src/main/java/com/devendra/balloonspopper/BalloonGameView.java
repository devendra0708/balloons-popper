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
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BalloonGameView extends View {
    public interface Listener {
        void onExitToMenu();

        void onStarsChanged();
    }

    private static final int MAX_BALLOONS = 9;
    private static final long SPAWN_DELAY_MS = 650L;
    private static final long SPEED_SPAWN_DELAY_MS = 380L;
    private static final long SPEED_DURATION_MS = 30_000L;
    private static final float ANIMAL_BALLOON_CHANCE = 0.35f;
    private static final int POPS_PER_STAR = 5;

    private static final class NamedColor {
        final String name;
        final int color;

        NamedColor(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    private final NamedColor[] namedColors = {
            new NamedColor("Red", Color.rgb(255, 82, 82)),
            new NamedColor("Yellow", Color.rgb(255, 193, 7)),
            new NamedColor("Green", Color.rgb(76, 175, 80)),
            new NamedColor("Blue", Color.rgb(33, 150, 243)),
            new NamedColor("Purple", Color.rgb(156, 39, 176)),
            new NamedColor("Orange", Color.rgb(255, 112, 67)),
            new NamedColor("Cyan", Color.rgb(0, 188, 212))
    };

    private enum BalloonShape {
        CIRCLE("Circle"),
        STAR("Star"),
        HEART("Heart"),
        SQUARE("Square");

        final String label;

        BalloonShape(String label) {
            this.label = label;
        }
    }

    private static final BalloonShape[] ALL_SHAPES = BalloonShape.values();

    private final int[] balloonColors = {
            Color.rgb(255, 82, 82),
            Color.rgb(255, 193, 7),
            Color.rgb(76, 175, 80),
            Color.rgb(33, 150, 243),
            Color.rgb(156, 39, 176),
            Color.rgb(255, 112, 67),
            Color.rgb(0, 188, 212)
    };

    private final AnimalType[] animalTypes = {
            new AnimalType("Cat", "Meow", Color.rgb(255, 170, 90), Color.rgb(255, 130, 60)),
            new AnimalType("Dog", "Woof", Color.rgb(255, 210, 150), Color.rgb(240, 170, 110)),
            new AnimalType("Cow", "Moo", Color.rgb(255, 255, 255), Color.rgb(55, 55, 55)),
            new AnimalType("Duck", "Quack", Color.rgb(255, 225, 70), Color.rgb(255, 150, 40)),
            new AnimalType("Lion", "Roar", Color.rgb(255, 180, 70), Color.rgb(230, 130, 40)),
            new AnimalType("Elephant", "Trumpet", Color.rgb(170, 175, 185), Color.rgb(120, 125, 140)),
            new AnimalType("Monkey", "Ooh ooh", Color.rgb(180, 120, 80), Color.rgb(130, 80, 50)),
            new AnimalType("Horse", "Neigh", Color.rgb(170, 110, 70), Color.rgb(230, 210, 180)),
            new AnimalType("Sheep", "Baa", Color.rgb(245, 245, 245), Color.rgb(255, 180, 190)),
            new AnimalType("Pig", "Oink", Color.rgb(255, 170, 180), Color.rgb(240, 120, 140)),
            new AnimalType("Chicken", "Cluck", Color.rgb(255, 220, 80), Color.rgb(255, 120, 70)),
            new AnimalType("Frog", "Ribbit", Color.rgb(110, 200, 90), Color.rgb(70, 160, 60)),
            new AnimalType("Bee", "Buzz", Color.rgb(255, 210, 50), Color.rgb(40, 40, 40)),
            new AnimalType("Owl", "Hoot", Color.rgb(180, 130, 80), Color.rgb(240, 220, 180)),
            new AnimalType("Penguin", "Honk", Color.rgb(40, 40, 50), Color.rgb(255, 150, 60)),
            new AnimalType("Turtle", "Hello", Color.rgb(110, 170, 90), Color.rgb(80, 130, 70)),
            new AnimalType("Rabbit", "Hop", Color.rgb(255, 240, 230), Color.rgb(255, 170, 190)),
            new AnimalType("Bear", "Growl", Color.rgb(160, 110, 70), Color.rgb(120, 80, 50)),
            new AnimalType("Butterfly", "Flutter", Color.rgb(255, 150, 200), Color.rgb(180, 140, 255))
    };

    private final List<Balloon> balloons = new ArrayList<>();
    private final List<FallingAnimal> fallingAnimals = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }

            long now = SystemClock.uptimeMillis();
            update(now);
            invalidate();
            postDelayed(this, 16L);
        }
    };

    private LinearGradient backgroundGradient;
    private long lastFrameMs;
    private long lastSpawnMs;
    private int nextLetterIndex;
    private int nextAnimalIndex;
    private int score;
    private int popsTowardStar;
    private int nextNumber = 1;
    private char targetLetter = 'A';
    private BalloonShape targetShape = BalloonShape.CIRCLE;
    private NamedColor targetNamedColor = namedColors[0];
    private long roundStartMs;
    private boolean timeUp;
    private String feedbackText;
    private long feedbackUntilMs;
    private boolean running;
    private boolean soundEnabled = true;
    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady;
    private GameMode mode = GameMode.BALLOON_POP;
    private Listener listener;
    private final AppPrefs prefs;
    private final SoundHelper sounds;
    private final RectF backButton = new RectF();
    private final RectF replayButton = new RectF();

    public BalloonGameView(Context context, AppPrefs prefs, SoundHelper sounds) {
        super(context);
        this.prefs = prefs;
        this.sounds = sounds;
        this.soundEnabled = prefs.isSoundEnabled();
        setFocusable(true);
        setBackgroundColor(Color.rgb(167, 231, 255));

        letterPaint.setColor(Color.WHITE);
        letterPaint.setFakeBoldText(true);
        letterPaint.setTextAlign(Paint.Align.CENTER);
        letterPaint.setShadowLayer(8f, 0f, 4f, Color.argb(150, 0, 0, 0));

        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(6f, 0f, 3f, Color.argb(120, 0, 0, 0));

        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
                textToSpeechReady = false;
                return;
            }

            int languageResult = textToSpeech.setLanguage(Locale.US);
            textToSpeechReady = languageResult != TextToSpeech.LANG_MISSING_DATA
                    && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setMode(GameMode mode) {
        this.mode = mode != null ? mode : GameMode.BALLOON_POP;
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public void resetRound() {
        balloons.clear();
        fallingAnimals.clear();
        particles.clear();
        score = 0;
        popsTowardStar = 0;
        nextLetterIndex = 0;
        nextAnimalIndex = 0;
        nextNumber = 1;
        targetLetter = (char) ('A' + random.nextInt(26));
        targetShape = ALL_SHAPES[random.nextInt(ALL_SHAPES.length)];
        targetNamedColor = namedColors[random.nextInt(namedColors.length)];
        roundStartMs = SystemClock.uptimeMillis();
        timeUp = false;
        feedbackText = null;
        lastFrameMs = roundStartMs;
        lastSpawnMs = lastFrameMs - spawnDelayMs();
        invalidate();
    }

    public void resume() {
        if (running) {
            return;
        }

        running = true;
        lastFrameMs = SystemClock.uptimeMillis();
        lastSpawnMs = lastFrameMs - spawnDelayMs();
        post(gameLoop);
    }

    public void pause() {
        running = false;
        removeCallbacks(gameLoop);
    }

    public void shutdown() {
        pause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            textToSpeechReady = false;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        backgroundGradient = new LinearGradient(
                0f,
                0f,
                0f,
                height,
                Color.rgb(167, 231, 255),
                Color.rgb(255, 241, 184),
                Shader.TileMode.CLAMP
        );
        float backSize = Math.max(56f, width * 0.12f);
        backButton.set(20f, 20f, 20f + backSize, 20f + backSize * 0.72f);
        float replayW = Math.min(width * 0.55f, 280f);
        float replayH = Math.max(64f, height * 0.07f);
        replayButton.set(
                (width - replayW) / 2f,
                height * 0.62f,
                (width + replayW) / 2f,
                height * 0.62f + replayH
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        drawClouds(canvas);

        for (Balloon balloon : balloons) {
            drawBalloon(canvas, balloon);
        }

        for (FallingAnimal animal : fallingAnimals) {
            drawFallingAnimal(canvas, animal);
        }

        for (Particle particle : particles) {
            drawParticle(canvas, particle);
        }

        drawHud(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        if (backButton.contains(event.getX(), event.getY())) {
            if (sounds != null) {
                sounds.playTap();
            }
            if (listener != null) {
                listener.onExitToMenu();
            }
            performClick();
            return true;
        }

        if (timeUp && mode == GameMode.SPEED_CHALLENGE && replayButton.contains(event.getX(), event.getY())) {
            if (sounds != null) {
                sounds.playPlay();
            }
            resetRound();
            performClick();
            return true;
        }

        if (timeUp) {
            performClick();
            return true;
        }

        for (int i = balloons.size() - 1; i >= 0; i--) {
            Balloon balloon = balloons.get(i);
            if (!balloon.contains(event.getX(), event.getY())) {
                continue;
            }

            if (isCorrectTarget(balloon)) {
                handleCorrectPop(balloon, i);
            } else if (requiresExactTarget()) {
                handleWrongPop(balloon);
            } else {
                handleCorrectPop(balloon, i);
            }
            performClick();
            return true;
        }

        performClick();
        return true;
    }

    private boolean requiresExactTarget() {
        return mode == GameMode.COLOR_POP
                || mode == GameMode.NUMBER_POP
                || mode == GameMode.LETTER_POP
                || mode == GameMode.SHAPE_POP;
    }

    private boolean isCorrectTarget(Balloon balloon) {
        switch (mode) {
            case COLOR_POP:
                return balloon.color == targetNamedColor.color;
            case NUMBER_POP:
                return balloon.number == nextNumber;
            case LETTER_POP:
                return balloon.letter == targetLetter;
            case SHAPE_POP:
                return balloon.shape == targetShape;
            case BALLOON_POP:
            case SPEED_CHALLENGE:
            default:
                return true;
        }
    }

    private void handleCorrectPop(Balloon balloon, int index) {
        popBalloon(balloon);
        balloons.remove(index);
        score++;
        popsTowardStar++;
        if (popsTowardStar >= POPS_PER_STAR) {
            popsTowardStar = 0;
            prefs.addStars(1);
            if (listener != null) {
                listener.onStarsChanged();
            }
        }

        // Keep real animal recordings clear instead of layering a UI beep over them.
        if (sounds != null && balloon.animalType == null) {
            sounds.playBalloonPop();
        }

        advanceTargetAfterPop(balloon);
    }

    private void handleWrongPop(Balloon balloon) {
        if (sounds != null) {
            sounds.playWrong();
        }
        showFeedback(wrongHint());
        speakText(wrongHint());
    }

    private void advanceTargetAfterPop(Balloon balloon) {
        switch (mode) {
            case NUMBER_POP:
                nextNumber++;
                if (nextNumber > 20) {
                    nextNumber = 1;
                }
                break;
            case LETTER_POP:
                targetLetter = (char) ('A' + random.nextInt(26));
                break;
            case SHAPE_POP:
                targetShape = ALL_SHAPES[random.nextInt(ALL_SHAPES.length)];
                break;
            case COLOR_POP:
                pickNextTargetColor();
                break;
            default:
                break;
        }
    }

    private void pickNextTargetColor() {
        if (namedColors.length <= 1) {
            targetNamedColor = namedColors[0];
            return;
        }
        NamedColor next;
        do {
            next = namedColors[random.nextInt(namedColors.length)];
        } while (next.color == targetNamedColor.color);
        targetNamedColor = next;
    }

    private String wrongHint() {
        switch (mode) {
            case COLOR_POP:
                return "Only " + targetNamedColor.name + "!";
            case NUMBER_POP:
                return "Find " + nextNumber;
            case LETTER_POP:
                return "Find " + targetLetter;
            case SHAPE_POP:
                return "Find " + targetShape.label;
            default:
                return "Try again!";
        }
    }

    private void showFeedback(String text) {
        feedbackText = text;
        feedbackUntilMs = SystemClock.uptimeMillis() + 900L;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void update(long nowMs) {
        float deltaSeconds = Math.min((nowMs - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = nowMs;

        if (mode == GameMode.SPEED_CHALLENGE && !timeUp) {
            if (nowMs - roundStartMs >= SPEED_DURATION_MS) {
                timeUp = true;
                balloons.clear();
                showFeedback("Time's up!");
                speakText("Time's up! You popped " + score);
            }
        }

        if (!timeUp && nowMs - lastSpawnMs >= spawnDelayMs() && balloons.size() < maxBalloons()) {
            spawnBalloon();
            lastSpawnMs = nowMs;
        }

        Iterator<Balloon> balloonIterator = balloons.iterator();
        while (balloonIterator.hasNext()) {
            Balloon balloon = balloonIterator.next();
            balloon.y -= balloon.speed * deltaSeconds;

            if (balloon.y + balloon.height < -40f) {
                balloonIterator.remove();
            }
        }

        Iterator<Particle> particleIterator = particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            particle.ageSeconds += deltaSeconds;
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;
            particle.velocityY += 280f * deltaSeconds;

            if (particle.ageSeconds >= particle.lifeSeconds) {
                particleIterator.remove();
            }
        }

        Iterator<FallingAnimal> animalIterator = fallingAnimals.iterator();
        while (animalIterator.hasNext()) {
            FallingAnimal animal = animalIterator.next();
            animal.ageSeconds += deltaSeconds;

            // Zigzag flutter + little lift pulses so falls feel lively.
            float flutter = (float) Math.sin(animal.ageSeconds * animal.flutterSpeed);
            float bob = (float) Math.sin(animal.ageSeconds * animal.flutterSpeed * 1.35f + 0.8f);
            animal.velocityX += flutter * animal.flutterForce * deltaSeconds;
            animal.velocityY += bob * 160f * deltaSeconds;

            animal.x += animal.velocityX * deltaSeconds;
            animal.y += animal.velocityY * deltaSeconds;
            animal.velocityY += animal.gravity * deltaSeconds;
            // Soft air drag keeps them floating and tumbling.
            animal.velocityX *= (1f - 0.55f * deltaSeconds);
            animal.velocityY *= (1f - 0.08f * deltaSeconds);

            animal.rotationDegrees += animal.rotationVelocity * deltaSeconds;
            animal.rotationVelocity += flutter * animal.spinJitter * deltaSeconds;
            // Keep spin from exploding.
            if (animal.rotationVelocity > 420f) {
                animal.rotationVelocity = 420f;
            } else if (animal.rotationVelocity < -420f) {
                animal.rotationVelocity = -420f;
            }

            if (animal.y - animal.size > getHeight() + 120f) {
                animalIterator.remove();
            }
        }
    }

    private long spawnDelayMs() {
        return mode == GameMode.SPEED_CHALLENGE ? SPEED_SPAWN_DELAY_MS : SPAWN_DELAY_MS;
    }

    private int maxBalloons() {
        return mode == GameMode.SPEED_CHALLENGE ? 12 : MAX_BALLOONS;
    }

    private void spawnBalloon() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float radiusX = randomBetween(width * 0.08f, width * 0.14f);
        float radiusY = radiusX * randomBetween(1.18f, 1.38f);
        float x = randomBetween(radiusX + 16f, width - radiusX - 16f);
        float y = height + radiusY + randomBetween(0f, height * 0.25f);

        int color = pickColorForMode();
        int number = pickNumberForMode();
        BalloonShape shape = pickShapeForMode();
        AnimalType animalType = mode == GameMode.BALLOON_POP && random.nextFloat() < ANIMAL_BALLOON_CHANCE
                ? nextAnimalType()
                : null;
        // Animal balloons stay letter-free so kids focus on the animal sound.
        char letter = animalType != null ? ' ' : pickLetterForMode();
        float speedMultiplier = mode == GameMode.SPEED_CHALLENGE ? 1.35f : 1f;
        float speed = randomBetween(height * 0.10f, height * 0.20f) * speedMultiplier;

        balloons.add(new Balloon(x, y, radiusX, radiusY, color, letter, number, shape, animalType, speed));
    }

    private int pickColorForMode() {
        if (mode == GameMode.COLOR_POP) {
            return random.nextFloat() < 0.42f
                    ? targetNamedColor.color
                    : namedColors[random.nextInt(namedColors.length)].color;
        }
        return balloonColors[random.nextInt(balloonColors.length)];
    }

    private char pickLetterForMode() {
        if (mode == GameMode.LETTER_POP && random.nextFloat() < 0.4f) {
            return targetLetter;
        }
        if (mode == GameMode.NUMBER_POP || mode == GameMode.SHAPE_POP || mode == GameMode.COLOR_POP) {
            return ' ';
        }
        return nextLetter();
    }

    private int pickNumberForMode() {
        if (mode != GameMode.NUMBER_POP) {
            return 0;
        }
        if (random.nextFloat() < 0.45f) {
            return nextNumber;
        }
        int offset = random.nextInt(7) - 2;
        return Math.max(1, Math.min(20, nextNumber + offset));
    }

    private BalloonShape pickShapeForMode() {
        if (mode != GameMode.SHAPE_POP) {
            return BalloonShape.CIRCLE;
        }
        if (random.nextFloat() < 0.4f) {
            return targetShape;
        }
        return ALL_SHAPES[random.nextInt(ALL_SHAPES.length)];
    }

    private char nextLetter() {
        char letter = (char) ('A' + nextLetterIndex);
        nextLetterIndex = (nextLetterIndex + 1) % 26;
        return letter;
    }

    private AnimalType nextAnimalType() {
        AnimalType animalType = animalTypes[nextAnimalIndex];
        nextAnimalIndex = (nextAnimalIndex + 1) % animalTypes.length;
        return animalType;
    }

    private void popBalloon(Balloon balloon) {
        speakPop(balloon);
        if (balloon.animalType != null) {
            boolean floaty = isFloatyAnimal(balloon.animalType.name);
            fallingAnimals.add(new FallingAnimal(
                    balloon.x,
                    balloon.animalY(),
                    balloon.animalSize(),
                    balloon.animalType,
                    randomBetween(-180f, 180f),
                    randomBetween(40f, 140f),
                    randomBetween(-280f, 280f),
                    floaty ? randomBetween(10f, 14f) : randomBetween(7f, 11f),
                    floaty ? randomBetween(520f, 760f) : randomBetween(360f, 560f),
                    floaty ? 520f : 760f,
                    randomBetween(70f, 140f)
            ));
        }

        for (int i = 0; i < 22; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2);
            float speed = randomBetween(120f, 430f);
            float particleSize = randomBetween(6f, 14f);

            particles.add(new Particle(
                    balloon.x,
                    balloon.y,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    particleSize,
                    balloon.color,
                    randomBetween(0.35f, 0.75f)
            ));
        }
    }

    private void speakPop(Balloon balloon) {
        switch (mode) {
            case COLOR_POP:
                speakText(targetNamedColor.name);
                break;
            case NUMBER_POP:
                speakText(String.valueOf(balloon.number));
                break;
            case LETTER_POP:
                speakText(String.valueOf(balloon.letter));
                break;
            case SHAPE_POP:
                speakText(balloon.shape.label);
                break;
            case SPEED_CHALLENGE:
                speakText("Pop");
                break;
            case BALLOON_POP:
            default:
                if (balloon.animalType != null) {
                    if (soundEnabled) {
                        boolean played = sounds.playAnimal(balloon.animalType.name);
                        if (!played) {
                            speakText(balloon.animalType.name);
                        }
                    }
                } else if (balloon.letter != ' ') {
                    speakText(String.valueOf(balloon.letter));
                }
                break;
        }
    }

    private void speakText(String phrase) {
        if (!soundEnabled || !textToSpeechReady || textToSpeech == null || phrase == null) {
            return;
        }
        textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "speak-" + phrase);
    }

    private void drawBackground(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(backgroundGradient);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }

    private void drawClouds(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(145, 255, 255, 255));
        drawCloud(canvas, getWidth() * 0.22f, getHeight() * 0.15f, getWidth() * 0.14f);
        drawCloud(canvas, getWidth() * 0.75f, getHeight() * 0.24f, getWidth() * 0.18f);
        drawCloud(canvas, getWidth() * 0.50f, getHeight() * 0.08f, getWidth() * 0.11f);
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

    private void drawBalloon(Canvas canvas, Balloon balloon) {
        float bottom = balloon.y + balloon.height;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(120, 90, 75, 60));
        Path stringPath = new Path();
        stringPath.moveTo(balloon.x, bottom + 10f);
        stringPath.cubicTo(
                balloon.x - 22f,
                bottom + 50f,
                balloon.x + 24f,
                bottom + 88f,
                balloon.x,
                bottom + 130f
        );
        canvas.drawPath(stringPath, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(balloon.color);
        canvas.drawOval(balloon.bounds(), paint);

        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawOval(
                new RectF(
                        balloon.x - balloon.width * 0.42f,
                        balloon.y - balloon.height * 0.52f,
                        balloon.x - balloon.width * 0.10f,
                        balloon.y - balloon.height * 0.12f
                ),
                paint
        );

        letterPaint.setTextSize(balloon.height * 0.82f);
        Paint.FontMetrics fontMetrics = letterPaint.getFontMetrics();
        float letterBaseline = balloon.y - (fontMetrics.ascent + fontMetrics.descent) / 2f;

        if (balloon.animalType != null) {
            // Animal balloons have no alphabet — the hanging animal is the focus.
        } else if (mode == GameMode.NUMBER_POP) {
            BubblyText.drawCenteredUniform(
                    canvas,
                    letterPaint,
                    getContext(),
                    String.valueOf(balloon.number),
                    balloon.x,
                    letterBaseline,
                    balloon.height * 0.72f,
                    Color.WHITE
            );
        } else if (mode == GameMode.SHAPE_POP) {
            drawBalloonShapeIcon(canvas, balloon);
        } else if (mode == GameMode.COLOR_POP || mode == GameMode.SPEED_CHALLENGE) {
            // Color/speed modes keep balloons clean with no letter clutter.
        } else if (balloon.letter != ' ') {
            BubblyText.drawLetter(
                    canvas,
                    letterPaint,
                    getContext(),
                    balloon.letter,
                    balloon.x,
                    letterBaseline,
                    balloon.height * 0.82f,
                    Color.WHITE
            );
        }

        paint.setColor(darken(balloon.color));
        Path knot = new Path();
        knot.moveTo(balloon.x - balloon.width * 0.12f, bottom - 4f);
        knot.lineTo(balloon.x + balloon.width * 0.12f, bottom - 4f);
        knot.lineTo(balloon.x, bottom + 18f);
        knot.close();
        canvas.drawPath(knot, paint);

        if (balloon.animalType != null) {
            drawHangingAnimal(canvas, balloon);
        }
    }

    private void drawBalloonShapeIcon(Canvas canvas, Balloon balloon) {
        float s = balloon.width * 0.55f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        switch (balloon.shape) {
            case STAR:
                drawStarPath(canvas, balloon.x, balloon.y, s);
                break;
            case HEART:
                drawHeart(canvas, balloon.x, balloon.y, s);
                break;
            case SQUARE:
                canvas.drawRoundRect(
                        new RectF(balloon.x - s * 0.7f, balloon.y - s * 0.7f, balloon.x + s * 0.7f, balloon.y + s * 0.7f),
                        s * 0.15f,
                        s * 0.15f,
                        paint
                );
                break;
            case CIRCLE:
            default:
                canvas.drawCircle(balloon.x, balloon.y, s * 0.7f, paint);
                break;
        }
    }

    private void drawStarPath(Canvas canvas, float cx, float cy, float radius) {
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

    private void drawHeart(Canvas canvas, float cx, float cy, float size) {
        Path heart = new Path();
        heart.moveTo(cx, cy + size * 0.55f);
        heart.cubicTo(cx - size * 1.1f, cy + size * 0.05f, cx - size * 0.7f, cy - size * 0.75f, cx, cy - size * 0.25f);
        heart.cubicTo(cx + size * 0.7f, cy - size * 0.75f, cx + size * 1.1f, cy + size * 0.05f, cx, cy + size * 0.55f);
        heart.close();
        canvas.drawPath(heart, paint);
    }

    private void drawHangingAnimal(Canvas canvas, Balloon balloon) {
        float animalY = balloon.animalY();
        float size = balloon.animalSize();
        float sway = (float) Math.sin(SystemClock.uptimeMillis() / 280.0 + balloon.x * 0.02) * 5f;
        float bob = (float) Math.sin(SystemClock.uptimeMillis() / 320.0 + balloon.x * 0.015) * 4f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.argb(170, 90, 75, 60));
        canvas.drawLine(balloon.x, balloon.y + balloon.height + 12f, balloon.x + sway * 0.35f, animalY + bob - size * 0.92f, paint);

        AnimalPose pose = AnimalPose.hanging(sway, bob);
        drawAnimal(canvas, balloon.animalType, balloon.x + sway * 0.35f, animalY + bob, size, pose);
    }

    private void drawFallingAnimal(Canvas canvas, FallingAnimal animal) {
        float flap = (float) Math.sin(animal.ageSeconds * animal.flutterSpeed * 1.6);
        float kick = (float) Math.sin(animal.ageSeconds * animal.flutterSpeed * 1.2 + 1.2);
        float tumble = (float) Math.sin(animal.ageSeconds * 5.5);
        float speedSquash = Math.min(0.30f, Math.abs(animal.velocityY) / 1300f);
        float stretchX = 1f + speedSquash + Math.abs(flap) * 0.10f;
        float stretchY = 1f - speedSquash * 0.95f + Math.abs(kick) * 0.06f;
        float wobbleX = flap * animal.size * 0.14f + tumble * animal.size * 0.05f;
        float wobbleY = kick * animal.size * 0.06f;

        AnimalPose pose = AnimalPose.falling(
                animal.rotationDegrees + tumble * 18f + flap * 10f,
                flap,
                kick,
                stretchX,
                stretchY,
                animal.ageSeconds
        );
        drawAnimal(canvas, animal.animalType, animal.x + wobbleX, animal.y + wobbleY, animal.size, pose);
    }

    private boolean isFloatyAnimal(String name) {
        return "Bee".equals(name)
                || "Butterfly".equals(name)
                || "Owl".equals(name)
                || "Duck".equals(name)
                || "Chicken".equals(name);
    }

    private void drawAnimal(Canvas canvas, AnimalType animalType, float x, float y, float size, AnimalPose pose) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(pose.rotationDegrees);
        canvas.scale(pose.stretchX, pose.stretchY);

        // Soft ground/air shadow under the character
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(pose.falling ? 35 : 45, 40, 60, 80));
        canvas.drawOval(
                new RectF(-size * 0.42f, size * 0.72f, size * 0.42f, size * 0.92f),
                paint
        );

        AnimalSprites.draw(
                canvas,
                getContext(),
                animalType.name,
                0f,
                size * 0.05f,
                size,
                0f,
                1f,
                1f,
                pose.falling ? pose.flap : (float) Math.sin(SystemClock.uptimeMillis() / 280.0)
        );

        canvas.restore();
    }

    private void drawCat(Canvas canvas, float x, float y, float size, AnimalType animalType, AnimalPose pose) {
        paint.setStyle(Paint.Style.FILL);

        // Chubby curled tail
        paint.setColor(animalType.accentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.14f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Path tail = new Path();
        tail.moveTo(x + size * 0.30f, y + size * 0.28f);
        tail.cubicTo(x + size * 0.72f, y + size * 0.05f, x + size * 0.68f, y - size * 0.28f, x + size * 0.42f, y - size * 0.05f);
        canvas.drawPath(tail, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);

        drawPawsUp(canvas, x, y, size, animalType.bodyColor, Color.rgb(255, 200, 170), pose);

        // Round chubby body + soft outline
        drawCartoonOutline(canvas, x, y + size * 0.22f, size * 0.44f, animalType.bodyColor);
        paint.setColor(Color.rgb(255, 235, 210));
        canvas.drawOval(new RectF(x - size * 0.24f, y + size * 0.08f, x + size * 0.24f, y + size * 0.50f), paint);

        // Big chibi head
        drawCartoonOutline(canvas, x, y - size * 0.38f, size * 0.50f, animalType.bodyColor);

        // Big triangle ears
        drawTriangleEar(canvas, x - size * 0.34f, y - size * 0.68f, size * 0.28f, size * 0.34f, animalType.bodyColor, true);
        drawTriangleEar(canvas, x + size * 0.34f, y - size * 0.68f, size * 0.28f, size * 0.34f, animalType.bodyColor, false);
        drawTriangleEar(canvas, x - size * 0.34f, y - size * 0.66f, size * 0.12f, size * 0.16f, Color.rgb(255, 160, 180), true);
        drawTriangleEar(canvas, x + size * 0.34f, y - size * 0.66f, size * 0.12f, size * 0.16f, Color.rgb(255, 160, 180), false);

        drawCartoonFace(canvas, x, y - size * 0.38f, size, Color.rgb(90, 210, 110), true, pose);
        drawCartoonBlush(canvas, x, y - size * 0.38f, size);

        // Whiskers
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.025f);
        paint.setColor(Color.rgb(90, 90, 90));
        canvas.drawLine(x - size * 0.12f, y - size * 0.32f, x - size * 0.48f, y - size * 0.38f, paint);
        canvas.drawLine(x - size * 0.12f, y - size * 0.28f, x - size * 0.48f, y - size * 0.28f, paint);
        canvas.drawLine(x + size * 0.12f, y - size * 0.32f, x + size * 0.48f, y - size * 0.38f, paint);
        canvas.drawLine(x + size * 0.12f, y - size * 0.28f, x + size * 0.48f, y - size * 0.28f, paint);
        paint.setStyle(Paint.Style.FILL);

        drawCartoonLegs(canvas, x, y, size, animalType.bodyColor, Color.rgb(255, 200, 170), pose);
    }

    private void drawDog(Canvas canvas, float x, float y, float size, AnimalType animalType, AnimalPose pose) {
        paint.setStyle(Paint.Style.FILL);

        // Soft curly puppy tail that wags with pose.
        float wag = pose.falling
                ? pose.flap * size * 0.18f
                : (float) Math.sin(SystemClock.uptimeMillis() / 220.0) * size * 0.08f;
        paint.setColor(animalType.accentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.15f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Path tail = new Path();
        tail.moveTo(x + size * 0.28f, y + size * 0.22f);
        tail.cubicTo(
                x + size * 0.62f + wag,
                y + size * 0.02f,
                x + size * 0.55f + wag,
                y - size * 0.28f,
                x + size * 0.34f + wag * 0.4f,
                y - size * 0.18f
        );
        canvas.drawPath(tail, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);

        drawPawsUp(canvas, x, y, size, animalType.bodyColor, Color.rgb(255, 235, 210), pose);

        // Chubby puppy body
        drawCartoonOutline(canvas, x, y + size * 0.26f, size * 0.44f, animalType.bodyColor);
        paint.setColor(Color.rgb(255, 245, 230));
        canvas.drawOval(new RectF(x - size * 0.24f, y + size * 0.12f, x + size * 0.24f, y + size * 0.52f), paint);

        // Extra-big puppy head (chibi proportions)
        drawCartoonOutline(canvas, x, y - size * 0.40f, size * 0.56f, animalType.bodyColor);

        // Huge soft floppy ears
        paint.setColor(animalType.accentColor);
        canvas.drawOval(new RectF(x - size * 0.72f, y - size * 0.58f, x - size * 0.16f, y + size * 0.18f), paint);
        canvas.drawOval(new RectF(x + size * 0.16f, y - size * 0.58f, x + size * 0.72f, y + size * 0.18f), paint);
        paint.setColor(Color.rgb(255, 190, 160));
        canvas.drawOval(new RectF(x - size * 0.60f, y - size * 0.46f, x - size * 0.24f, y + size * 0.02f), paint);
        canvas.drawOval(new RectF(x + size * 0.24f, y - size * 0.46f, x + size * 0.60f, y + size * 0.02f), paint);

        // Soft cream muzzle
        paint.setColor(Color.rgb(255, 240, 220));
        canvas.drawOval(new RectF(x - size * 0.24f, y - size * 0.34f, x + size * 0.24f, y + size * 0.06f), paint);

        // Tiny shiny nose
        paint.setColor(Color.rgb(70, 45, 40));
        canvas.drawOval(new RectF(x - size * 0.08f, y - size * 0.28f, x + size * 0.08f, y - size * 0.14f), paint);
        paint.setColor(Color.argb(160, 255, 255, 255));
        canvas.drawCircle(x - size * 0.03f, y - size * 0.24f, size * 0.02f, paint);

        // Giant sparkly puppy eyes
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x - size * 0.18f, y - size * 0.52f, size * 0.15f, paint);
        canvas.drawCircle(x + size * 0.18f, y - size * 0.52f, size * 0.15f, paint);
        paint.setColor(Color.rgb(110, 70, 40));
        canvas.drawCircle(x - size * 0.18f, y - size * 0.50f, size * 0.09f, paint);
        canvas.drawCircle(x + size * 0.18f, y - size * 0.50f, size * 0.09f, paint);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(x - size * 0.18f, y - size * 0.49f, size * 0.045f, paint);
        canvas.drawCircle(x + size * 0.18f, y - size * 0.49f, size * 0.045f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x - size * 0.14f, y - size * 0.56f, size * 0.035f, paint);
        canvas.drawCircle(x + size * 0.22f, y - size * 0.56f, size * 0.035f, paint);
        canvas.drawCircle(x - size * 0.20f, y - size * 0.47f, size * 0.015f, paint);
        canvas.drawCircle(x + size * 0.16f, y - size * 0.47f, size * 0.015f, paint);

        drawCartoonBlush(canvas, x, y - size * 0.36f, size * 1.05f);

        if (pose.falling) {
            drawFallSurpriseMouth(canvas, x, y - size * 0.04f, size, pose);
        } else {
            // Happy smile + pink tongue
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.04f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(90, 55, 50));
            Path smile = new Path();
            smile.moveTo(x - size * 0.12f, y - size * 0.08f);
            smile.quadTo(x, y + size * 0.04f, x + size * 0.12f, y - size * 0.08f);
            canvas.drawPath(smile, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setColor(Color.rgb(255, 120, 150));
            canvas.drawOval(new RectF(x - size * 0.07f, y - size * 0.04f, x + size * 0.07f, y + size * 0.14f), paint);
        }

        // Soft pink collar with a heart tag
        paint.setColor(Color.rgb(255, 120, 170));
        canvas.drawRoundRect(
                new RectF(x - size * 0.34f, y + size * 0.02f, x + size * 0.34f, y + size * 0.14f),
                size * 0.08f,
                size * 0.08f,
                paint
        );
        paint.setColor(Color.rgb(255, 90, 140));
        Path heart = new Path();
        float hx = x;
        float hy = y + size * 0.08f;
        float hs = size * 0.08f;
        heart.moveTo(hx, hy + hs * 0.55f);
        heart.cubicTo(hx - hs * 1.1f, hy + hs * 0.05f, hx - hs * 0.7f, hy - hs * 0.75f, hx, hy - hs * 0.25f);
        heart.cubicTo(hx + hs * 0.7f, hy - hs * 0.75f, hx + hs * 1.1f, hy + hs * 0.05f, hx, hy + hs * 0.55f);
        heart.close();
        canvas.drawPath(heart, paint);

        drawCartoonLegs(canvas, x, y, size, animalType.bodyColor, Color.rgb(255, 235, 210), pose);
    }

    private void drawCow(Canvas canvas, float x, float y, float size, AnimalType animalType, AnimalPose pose) {
        paint.setStyle(Paint.Style.FILL);
        drawPawsUp(canvas, x, y, size, animalType.bodyColor, Color.rgb(230, 230, 230), pose);

        drawCartoonOutline(canvas, x, y + size * 0.22f, size * 0.48f, animalType.bodyColor);

        paint.setColor(animalType.accentColor);
        canvas.drawOval(new RectF(x - size * 0.42f, y + size * 0.02f, x - size * 0.05f, y + size * 0.32f), paint);
        canvas.drawOval(new RectF(x + size * 0.02f, y + size * 0.18f, x + size * 0.40f, y + size * 0.46f), paint);
        canvas.drawCircle(x + size * 0.24f, y - size * 0.02f, size * 0.12f, paint);

        drawCartoonOutline(canvas, x, y - size * 0.38f, size * 0.52f, animalType.bodyColor);
        paint.setColor(animalType.accentColor);
        canvas.drawOval(new RectF(x - size * 0.28f, y - size * 0.62f, x + size * 0.02f, y - size * 0.28f), paint);

        paint.setColor(Color.rgb(255, 220, 140));
        Path leftHorn = new Path();
        leftHorn.moveTo(x - size * 0.26f, y - size * 0.72f);
        leftHorn.quadTo(x - size * 0.42f, y - size * 1.05f, x - size * 0.14f, y - size * 0.78f);
        leftHorn.close();
        canvas.drawPath(leftHorn, paint);
        Path rightHorn = new Path();
        rightHorn.moveTo(x + size * 0.26f, y - size * 0.72f);
        rightHorn.quadTo(x + size * 0.42f, y - size * 1.05f, x + size * 0.14f, y - size * 0.78f);
        rightHorn.close();
        canvas.drawPath(rightHorn, paint);

        paint.setColor(Color.rgb(255, 180, 190));
        canvas.drawOval(new RectF(x - size * 0.62f, y - size * 0.52f, x - size * 0.32f, y - size * 0.22f), paint);
        canvas.drawOval(new RectF(x + size * 0.32f, y - size * 0.52f, x + size * 0.62f, y - size * 0.22f), paint);

        paint.setColor(Color.rgb(255, 170, 185));
        canvas.drawOval(new RectF(x - size * 0.26f, y - size * 0.32f, x + size * 0.26f, y + size * 0.02f), paint);
        paint.setColor(Color.rgb(120, 60, 70));
        canvas.drawOval(new RectF(x - size * 0.14f, y - size * 0.22f, x - size * 0.04f, y - size * 0.12f), paint);
        canvas.drawOval(new RectF(x + size * 0.04f, y - size * 0.22f, x + size * 0.14f, y - size * 0.12f), paint);

        drawCartoonEyes(canvas, x, y - size * 0.52f, size, Color.BLACK);
        drawCartoonBlush(canvas, x, y - size * 0.38f, size);
        drawFallSurpriseMouth(canvas, x, y - size * 0.05f, size, pose);
        drawCartoonLegs(canvas, x, y, size, animalType.bodyColor, Color.rgb(50, 50, 50), pose);
    }

    private void drawDuck(Canvas canvas, float x, float y, float size, AnimalType animalType, AnimalPose pose) {
        paint.setStyle(Paint.Style.FILL);

        drawPawsUp(canvas, x, y, size, animalType.bodyColor, Color.rgb(255, 200, 50), pose);

        // Plump front-facing body.
        drawCartoonOutline(canvas, x, y + size * 0.20f, size * 0.46f, animalType.bodyColor);
        paint.setColor(Color.rgb(255, 245, 160));
        canvas.drawOval(new RectF(x - size * 0.26f, y + size * 0.08f, x + size * 0.26f, y + size * 0.50f), paint);

        // Rounded wings flap outward with the falling pose.
        float wingLift = pose.falling ? pose.flap * size * 0.15f : 0f;
        paint.setColor(Color.rgb(255, 198, 45));
        canvas.drawOval(
                new RectF(
                        x - size * 0.62f,
                        y - size * 0.02f - wingLift,
                        x - size * 0.18f,
                        y + size * 0.42f - wingLift
                ),
                paint
        );
        canvas.drawOval(
                new RectF(
                        x + size * 0.18f,
                        y - size * 0.02f + wingLift,
                        x + size * 0.62f,
                        y + size * 0.42f + wingLift
                ),
                paint
        );

        // Large round head and a three-feather tuft.
        drawCartoonOutline(canvas, x, y - size * 0.38f, size * 0.50f, animalType.bodyColor);
        paint.setColor(animalType.bodyColor);
        Path tuft = new Path();
        tuft.moveTo(x - size * 0.16f, y - size * 0.78f);
        tuft.quadTo(x - size * 0.11f, y - size * 1.04f, x, y - size * 0.78f);
        tuft.quadTo(x + size * 0.03f, y - size * 1.08f, x + size * 0.15f, y - size * 0.76f);
        tuft.quadTo(x + size * 0.25f, y - size * 0.94f, x + size * 0.25f, y - size * 0.68f);
        tuft.close();
        canvas.drawPath(tuft, paint);

        // Two huge sparkly eyes.
        drawCartoonEyes(canvas, x, y - size * 0.52f, size, Color.rgb(55, 95, 125));

        // Wide central bill makes the duck unmistakable.
        paint.setColor(animalType.accentColor);
        canvas.drawOval(
                new RectF(
                        x - size * 0.32f,
                        y - size * 0.32f,
                        x + size * 0.32f,
                        y - size * 0.04f
                ),
                paint
        );
        paint.setColor(Color.rgb(240, 120, 20));
        canvas.drawRoundRect(
                new RectF(
                        x - size * 0.25f,
                        y - size * 0.20f,
                        x + size * 0.25f,
                        y - size * 0.12f
                ),
                size * 0.04f,
                size * 0.04f,
                paint
        );
        drawCartoonBlush(canvas, x, y - size * 0.34f, size);

        // Oversized webbed feet kick independently while falling.
        float footKick = pose.falling ? pose.kick * size * 0.12f : 0f;
        paint.setColor(animalType.accentColor);
        canvas.drawOval(
                new RectF(
                        x - size * 0.42f,
                        y + size * 0.50f + footKick,
                        x - size * 0.02f,
                        y + size * 0.74f + footKick
                ),
                paint
        );
        canvas.drawOval(
                new RectF(
                        x + size * 0.02f,
                        y + size * 0.50f - footKick,
                        x + size * 0.42f,
                        y + size * 0.74f - footKick
                ),
                paint
        );
    }

    private void drawCartoonEyes(Canvas canvas, float x, float eyeY, float size, int irisColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x - size * 0.16f, eyeY, size * 0.13f, paint);
        canvas.drawCircle(x + size * 0.16f, eyeY, size * 0.13f, paint);
        paint.setColor(irisColor);
        canvas.drawCircle(x - size * 0.16f, eyeY + size * 0.015f, size * 0.08f, paint);
        canvas.drawCircle(x + size * 0.16f, eyeY + size * 0.015f, size * 0.08f, paint);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(x - size * 0.16f, eyeY + size * 0.02f, size * 0.04f, paint);
        canvas.drawCircle(x + size * 0.16f, eyeY + size * 0.02f, size * 0.04f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x - size * 0.12f, eyeY - size * 0.035f, size * 0.03f, paint);
        canvas.drawCircle(x + size * 0.20f, eyeY - size * 0.035f, size * 0.03f, paint);
    }

    private void drawCartoonFace(Canvas canvas, float x, float headY, float size, int eyeColor, boolean withNose, AnimalPose pose) {
        drawCartoonEyes(canvas, x, headY - size * 0.05f, size, eyeColor);

        if (withNose) {
            paint.setColor(Color.rgb(255, 120, 150));
            Path nose = new Path();
            nose.moveTo(x, headY + size * 0.08f);
            nose.lineTo(x - size * 0.07f, headY + size * 0.01f);
            nose.lineTo(x + size * 0.07f, headY + size * 0.01f);
            nose.close();
            canvas.drawPath(nose, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.04f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(90, 50, 60));
        Path mouth = new Path();
        if (pose.falling) {
            // Surprised cartoon O mouth while falling.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(60, 30, 40));
            canvas.drawOval(
                    new RectF(x - size * 0.09f, headY + size * 0.12f, x + size * 0.09f, headY + size * 0.30f),
                    paint
            );
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(90, 50, 60));
        } else {
            mouth.moveTo(x - size * 0.14f, headY + size * 0.14f);
            mouth.quadTo(x, headY + size * 0.28f, x + size * 0.14f, headY + size * 0.14f);
            canvas.drawPath(mouth, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawCartoonBlush(Canvas canvas, float x, float y, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(130, 255, 110, 150));
        canvas.drawCircle(x - size * 0.30f, y + size * 0.05f, size * 0.09f, paint);
        canvas.drawCircle(x + size * 0.30f, y + size * 0.05f, size * 0.09f, paint);
    }

    private void drawFallSurpriseMouth(Canvas canvas, float x, float y, float size, AnimalPose pose) {
        if (!pose.falling) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(60, 30, 40));
        canvas.drawOval(new RectF(x - size * 0.09f, y, x + size * 0.09f, y + size * 0.16f), paint);
    }

    private void drawCartoonLegs(Canvas canvas, float x, float y, float size, int legColor, int pawColor, AnimalPose pose) {
        paint.setStyle(Paint.Style.FILL);
        float kick = pose.kick * size * (pose.falling ? 0.22f : 0.04f);
        float leftTop = y + size * 0.42f;
        float rightTop = y + size * 0.42f;
        float leftBottom = y + size * 0.78f + kick;
        float rightBottom = y + size * 0.78f - kick;
        float leftX = x - size * 0.19f - (pose.falling ? pose.flap * size * 0.08f : 0f);
        float rightX = x + size * 0.19f + (pose.falling ? pose.flap * size * 0.08f : 0f);

        paint.setColor(legColor);
        canvas.drawRoundRect(new RectF(leftX - size * 0.13f, leftTop, leftX + size * 0.13f, leftBottom), size * 0.12f, size * 0.12f, paint);
        canvas.drawRoundRect(new RectF(rightX - size * 0.13f, rightTop, rightX + size * 0.13f, rightBottom), size * 0.12f, size * 0.12f, paint);
        paint.setColor(pawColor);
        canvas.drawOval(new RectF(leftX - size * 0.17f, leftBottom - size * 0.08f, leftX + size * 0.17f, leftBottom + size * 0.10f), paint);
        canvas.drawOval(new RectF(rightX - size * 0.17f, rightBottom - size * 0.08f, rightX + size * 0.17f, rightBottom + size * 0.10f), paint);
    }

    private void drawPawsUp(Canvas canvas, float x, float y, float size, int furColor, int pawColor, AnimalPose pose) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(furColor);

        float flap = pose.flap * size * (pose.falling ? 0.28f : 0.05f);
        float reach = pose.falling ? size * 0.55f : size * 0.88f;
        float out = pose.falling ? size * 0.55f : size * 0.28f;

        // Left arm
        float lx1 = x - size * 0.18f;
        float ly1 = y - size * 0.05f;
        float lx2 = x - out - flap;
        float ly2 = y - reach + (pose.falling ? flap * 0.4f : 0f);
        drawCartoonArm(canvas, lx1, ly1, lx2, ly2, size, furColor, pawColor);

        // Right arm
        float rx1 = x + size * 0.18f;
        float ry1 = y - size * 0.05f;
        float rx2 = x + out + flap;
        float ry2 = y - reach - (pose.falling ? flap * 0.4f : 0f);
        drawCartoonArm(canvas, rx1, ry1, rx2, ry2, size, furColor, pawColor);
    }

    private void drawCartoonArm(Canvas canvas, float x1, float y1, float x2, float y2, float size, int furColor, int pawColor) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.16f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(furColor);
        canvas.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pawColor);
        canvas.drawCircle(x2, y2, size * 0.13f, paint);
        // Tiny cartoon paw pads
        paint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawCircle(x2 - size * 0.04f, y2 - size * 0.02f, size * 0.03f, paint);
        canvas.drawCircle(x2 + size * 0.04f, y2 - size * 0.02f, size * 0.03f, paint);
        canvas.drawCircle(x2, y2 + size * 0.04f, size * 0.035f, paint);
    }

    private void drawCartoonOutline(Canvas canvas, float cx, float cy, float radius, int fillColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(darken(fillColor));
        canvas.drawCircle(cx, cy, radius * 1.06f, paint);
        paint.setColor(fillColor);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    private void drawTriangleEar(
            Canvas canvas,
            float tipX,
            float tipY,
            float width,
            float height,
            int color,
            boolean left
    ) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        Path ear = new Path();
        if (left) {
            ear.moveTo(tipX, tipY - height);
            ear.lineTo(tipX - width, tipY + height * 0.15f);
            ear.lineTo(tipX + width * 0.35f, tipY + height * 0.25f);
        } else {
            ear.moveTo(tipX, tipY - height);
            ear.lineTo(tipX + width, tipY + height * 0.15f);
            ear.lineTo(tipX - width * 0.35f, tipY + height * 0.25f);
        }
        ear.close();
        canvas.drawPath(ear, paint);
    }

    private void drawParticle(Canvas canvas, Particle particle) {
        float progress = particle.ageSeconds / particle.lifeSeconds;
        int alpha = Math.max(0, 255 - (int) (progress * 255));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color)));
        canvas.drawCircle(particle.x, particle.y, particle.size * (1f - progress * 0.45f), paint);
    }

    private void drawHud(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(230, 255, 255, 255));
        canvas.drawRoundRect(backButton, backButton.height() / 2f, backButton.height() / 2f, paint);
        BubblyText.apply(textPaint, getContext());
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.rgb(70, 110, 160));
        textPaint.setShadowLayer(0f, 0f, 0f, 0);
        textPaint.setTextSize(Math.max(28f, getWidth() * 0.045f));
        canvas.drawText("←", backButton.centerX(), backButton.centerY() + textPaint.getTextSize() * 0.32f, textPaint);

        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                "Pops: " + score,
                backButton.right + Math.max(90f, getWidth() * 0.18f),
                backButton.centerY() + Math.max(34f, getWidth() * 0.06f) * 0.18f,
                Math.max(30f, getWidth() * 0.052f),
                0f,
                0f
        );

        String rightHud;
        if (mode == GameMode.SPEED_CHALLENGE && !timeUp) {
            long remaining = Math.max(0L, SPEED_DURATION_MS - (SystemClock.uptimeMillis() - roundStartMs));
            rightHud = (remaining / 1000L) + "s";
        } else {
            rightHud = prefs.getStars() + " Stars";
        }
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                rightHud,
                getWidth() - Math.max(70f, getWidth() * 0.14f),
                backButton.centerY() + Math.max(26f, getWidth() * 0.04f) * 0.18f,
                Math.max(24f, getWidth() * 0.038f),
                0f,
                0f
        );

        float promptY = backButton.bottom + Math.max(28f, getHeight() * 0.028f);

        if (mode == GameMode.COLOR_POP) {
            drawColorTargetBadge(canvas, promptY);
        } else {
            float promptSize = Math.max(80f, getWidth() * 0.15f);
            // Slightly smaller for the longest speed-challenge line so it still fits.
            if (mode == GameMode.SPEED_CHALLENGE && !timeUp) {
                promptSize = Math.max(52f, getWidth() * 0.095f);
            }
            float bigPromptY = promptY + Math.max(18f, getHeight() * 0.018f);
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    modePrompt(),
                    getWidth() / 2f,
                    bigPromptY,
                    promptSize,
                    4f,
                    SystemClock.uptimeMillis() / 1000f
            );
        }

        if (mode == GameMode.SHAPE_POP) {
            paint.setColor(Color.rgb(171, 71, 188));
            float iconY = promptY + Math.max(90f, getHeight() * 0.095f);
            float iconSize = Math.max(28f, getWidth() * 0.055f);
            canvas.drawCircle(getWidth() / 2f, iconY, iconSize * 1.15f, paint);
            paint.setColor(Color.WHITE);
            switch (targetShape) {
                case STAR:
                    drawStarPath(canvas, getWidth() / 2f, iconY, iconSize);
                    break;
                case HEART:
                    drawHeart(canvas, getWidth() / 2f, iconY, iconSize);
                    break;
                case SQUARE:
                    canvas.drawRoundRect(
                            new RectF(
                                    getWidth() / 2f - iconSize * 0.7f,
                                    iconY - iconSize * 0.7f,
                                    getWidth() / 2f + iconSize * 0.7f,
                                    iconY + iconSize * 0.7f
                            ),
                            6f,
                            6f,
                            paint
                    );
                    break;
                case CIRCLE:
                default:
                    canvas.drawCircle(getWidth() / 2f, iconY, iconSize * 0.7f, paint);
                    break;
            }
        }

        if (feedbackText != null && SystemClock.uptimeMillis() < feedbackUntilMs) {
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    feedbackText,
                    getWidth() / 2f,
                    getHeight() * 0.55f,
                    Math.max(36f, getWidth() * 0.065f),
                    4f,
                    SystemClock.uptimeMillis() / 1000f
            );
        }

        if (timeUp && mode == GameMode.SPEED_CHALLENGE) {
            paint.setColor(Color.argb(150, 20, 40, 70));
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    "Time's up!",
                    getWidth() / 2f,
                    getHeight() * 0.42f,
                    Math.max(44f, getWidth() * 0.08f),
                    4f,
                    SystemClock.uptimeMillis() / 1000f
            );
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    "Pops: " + score,
                    getWidth() / 2f,
                    getHeight() * 0.52f,
                    Math.max(34f, getWidth() * 0.06f),
                    0f,
                    0f
            );

            paint.setColor(Color.rgb(255, 167, 38));
            canvas.drawRoundRect(replayButton, replayButton.height() / 2f, replayButton.height() / 2f, paint);
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    "Play Again",
                    replayButton.centerX(),
                    replayButton.centerY() + Math.max(32f, getWidth() * 0.055f) * 0.32f,
                    Math.max(32f, getWidth() * 0.055f),
                    2f,
                    SystemClock.uptimeMillis() / 1000f
            );
        } else if (score == 0 && feedbackText == null) {
            BubblyText.drawCentered(
                    canvas,
                    textPaint,
                    getContext(),
                    startHint(),
                    getWidth() / 2f,
                    getHeight() * 0.88f,
                    Math.max(28f, getWidth() * 0.05f),
                    2f,
                    SystemClock.uptimeMillis() / 1000f
            );
        }
    }

    private void drawColorTargetBadge(Canvas canvas, float topY) {
        float colorNameSize = Math.max(72f, getWidth() * 0.13f);
        String colorName = targetNamedColor.name.toUpperCase(Locale.US);

        BubblyText.apply(textPaint, getContext());
        textPaint.setTextSize(colorNameSize);
        float nameWidth = textPaint.measureText(colorName);

        float swatchSlot = Math.max(100f, getWidth() * 0.20f);
        float sidePad = Math.max(20f, getWidth() * 0.03f);
        float badgeW = Math.min(getWidth() * 0.94f, swatchSlot + nameWidth + sidePad * 2.2f);
        float badgeH = Math.max(swatchSlot * 0.95f, colorNameSize * 2.1f);
        float left = (getWidth() - badgeW) / 2f;
        float top = topY + Math.max(8f, getHeight() * 0.01f);
        RectF badge = new RectF(left, top, left + badgeW, top + badgeH);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(55, 0, 0, 0));
        canvas.drawRoundRect(
                new RectF(badge.left, badge.top + 8f, badge.right, badge.bottom + 8f),
                badgeH / 2.2f,
                badgeH / 2.2f,
                paint
        );
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(badge, badgeH / 2.2f, badgeH / 2.2f, paint);

        float balloonCx = left + swatchSlot * 0.52f;
        float balloonCy = badge.centerY() - badgeH * 0.02f;
        float balloonR = badgeH * 0.30f;

        // Soft glow ring so the color pops for kids
        paint.setColor(Color.argb(70, Color.red(targetNamedColor.color), Color.green(targetNamedColor.color), Color.blue(targetNamedColor.color)));
        canvas.drawCircle(balloonCx, balloonCy, balloonR * 1.35f, paint);

        paint.setColor(targetNamedColor.color);
        canvas.drawOval(
                new RectF(
                        balloonCx - balloonR,
                        balloonCy - balloonR * 1.2f,
                        balloonCx + balloonR,
                        balloonCy + balloonR * 1.05f
                ),
                paint
        );

        paint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawOval(
                new RectF(
                        balloonCx - balloonR * 0.55f,
                        balloonCy - balloonR * 0.85f,
                        balloonCx - balloonR * 0.05f,
                        balloonCy - balloonR * 0.15f
                ),
                paint
        );

        paint.setColor(darken(targetNamedColor.color));
        Path knot = new Path();
        knot.moveTo(balloonCx - balloonR * 0.22f, balloonCy + balloonR * 0.95f);
        knot.lineTo(balloonCx + balloonR * 0.22f, balloonCy + balloonR * 0.95f);
        knot.lineTo(balloonCx, balloonCy + balloonR * 1.35f);
        knot.close();
        canvas.drawPath(knot, paint);

        // Big color name in the space to the right of the swatch
        float textAreaCenterX = (left + swatchSlot + badge.right) / 2f;
        BubblyText.drawCentered(
                canvas,
                textPaint,
                getContext(),
                colorName,
                textAreaCenterX,
                badge.centerY() + colorNameSize * 0.32f,
                colorNameSize,
                3f,
                SystemClock.uptimeMillis() / 1000f
        );

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(6f, 0f, 3f, Color.argb(120, 0, 0, 0));
    }

    private String modePrompt() {
        switch (mode) {
            case COLOR_POP:
                return "Pop " + targetNamedColor.name.toUpperCase(Locale.US);
            case NUMBER_POP:
                return "Next: " + nextNumber;
            case LETTER_POP:
                return "Pop: " + targetLetter;
            case SHAPE_POP:
                return "Pop: " + targetShape.label;
            case SPEED_CHALLENGE:
                return timeUp ? "Speed Challenge" : "Pop as many as you can!";
            case BALLOON_POP:
            default:
                return mode.title;
        }
    }

    private String startHint() {
        switch (mode) {
            case COLOR_POP:
                return "Tap the matching color!";
            case NUMBER_POP:
                return "Tap numbers in order!";
            case LETTER_POP:
                return "Tap the letter at the top!";
            case SHAPE_POP:
                return "Tap the matching shape!";
            case SPEED_CHALLENGE:
                return "Go fast!";
            default:
                return "Tap the balloons!";
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

    private static final class Balloon {
        final float x;
        float y;
        final float width;
        final float height;
        final int color;
        final char letter;
        final int number;
        final BalloonShape shape;
        final AnimalType animalType;
        final float speed;

        Balloon(
                float x,
                float y,
                float width,
                float height,
                int color,
                char letter,
                int number,
                BalloonShape shape,
                AnimalType animalType,
                float speed
        ) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.letter = letter;
            this.number = number;
            this.shape = shape;
            this.animalType = animalType;
            this.speed = speed;
        }

        RectF bounds() {
            return new RectF(x - width, y - height, x + width, y + height);
        }

        float animalY() {
            return y + height + animalSize() * 1.28f;
        }

        float animalSize() {
            return Math.max(96f, width * 1.22f);
        }

        boolean contains(float touchX, float touchY) {
            float normalizedX = (touchX - x) / width;
            float normalizedY = (touchY - y) / height;
            return normalizedX * normalizedX + normalizedY * normalizedY <= 1f;
        }
    }

    private static final class AnimalType {
        final String name;
        final String voice;
        final int bodyColor;
        final int accentColor;

        AnimalType(String name, String voice, int bodyColor, int accentColor) {
            this.name = name;
            this.voice = voice;
            this.bodyColor = bodyColor;
            this.accentColor = accentColor;
        }
    }

    private static final class AnimalPose {
        final boolean falling;
        final float rotationDegrees;
        final float flap;
        final float kick;
        final float stretchX;
        final float stretchY;
        final float ageSeconds;

        private AnimalPose(
                boolean falling,
                float rotationDegrees,
                float flap,
                float kick,
                float stretchX,
                float stretchY,
                float ageSeconds
        ) {
            this.falling = falling;
            this.rotationDegrees = rotationDegrees;
            this.flap = flap;
            this.kick = kick;
            this.stretchX = stretchX;
            this.stretchY = stretchY;
            this.ageSeconds = ageSeconds;
        }

        static AnimalPose hanging(float sway, float bob) {
            return new AnimalPose(false, sway * 0.35f, (float) Math.sin(bob * 0.4f), 0f, 1f, 1f, 0f);
        }

        static AnimalPose falling(
                float rotationDegrees,
                float flap,
                float kick,
                float stretchX,
                float stretchY,
                float ageSeconds
        ) {
            return new AnimalPose(true, rotationDegrees, flap, kick, stretchX, stretchY, ageSeconds);
        }
    }

    private static final class FallingAnimal {
        float x;
        float y;
        float velocityX;
        float velocityY;
        float rotationDegrees;
        float rotationVelocity;
        float ageSeconds;
        final float size;
        final AnimalType animalType;
        final float flutterSpeed;
        final float flutterForce;
        final float gravity;
        final float spinJitter;

        FallingAnimal(
                float x,
                float y,
                float size,
                AnimalType animalType,
                float velocityX,
                float velocityY,
                float rotationVelocity,
                float flutterSpeed,
                float flutterForce,
                float gravity,
                float spinJitter
        ) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.animalType = animalType;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.rotationVelocity = rotationVelocity;
            this.ageSeconds = 0f;
            this.flutterSpeed = flutterSpeed;
            this.flutterForce = flutterForce;
            this.gravity = gravity;
            this.spinJitter = spinJitter;
        }
    }

    private static final class Particle {
        float x;
        float y;
        float velocityX;
        float velocityY;
        final float size;
        final int color;
        final float lifeSeconds;
        float ageSeconds;

        Particle(
                float x,
                float y,
                float velocityX,
                float velocityY,
                float size,
                int color,
                float lifeSeconds
        ) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.color = color;
            this.lifeSeconds = lifeSeconds;
        }
    }
}
