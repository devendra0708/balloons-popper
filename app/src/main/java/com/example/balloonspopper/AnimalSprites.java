package com.example.balloonspopper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.SparseArray;

/**
 * Soft illustrated animal sprites for a preschool / Play Store style look.
 */
public final class AnimalSprites {
    public static final String[] ALL_NAMES = {
            "Cat", "Dog", "Cow", "Duck",
            "Lion", "Elephant", "Monkey", "Horse", "Sheep", "Pig",
            "Chicken", "Frog", "Bee", "Owl", "Penguin", "Turtle",
            "Rabbit", "Bear", "Butterfly"
    };

    private static final SparseArray<Bitmap> CACHE = new SparseArray<>();
    private static final Matrix MATRIX = new Matrix();
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private AnimalSprites() {
    }

    public static void draw(
            Canvas canvas,
            Context context,
            String animalName,
            float centerX,
            float centerY,
            float size,
            float rotationDegrees,
            float stretchX,
            float stretchY,
            float flap
    ) {
        Bitmap bitmap = bitmapFor(context, animalName);
        if (bitmap == null) {
            return;
        }

        float target = size * 2.35f;
        float scale = target / Math.max(bitmap.getWidth(), bitmap.getHeight());
        // Livelier bob / flutter while falling or bouncing.
        float bobX = flap * size * 0.08f;
        float bobY = flap * size * 0.12f;

        MATRIX.reset();
        MATRIX.postTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
        MATRIX.postScale(scale * stretchX, scale * stretchY);
        MATRIX.postRotate(rotationDegrees);
        MATRIX.postTranslate(centerX + bobX, centerY + bobY);
        canvas.drawBitmap(bitmap, MATRIX, PAINT);
    }

    public static void drawSimple(
            Canvas canvas,
            Context context,
            String animalName,
            float centerX,
            float centerY,
            float size
    ) {
        draw(canvas, context, animalName, centerX, centerY, size, 0f, 1f, 1f, 0f);
    }

    public static void drawByKind(
            Canvas canvas,
            Context context,
            int kind,
            float centerX,
            float centerY,
            float size,
            float tilt,
            float flap
    ) {
        draw(canvas, context, nameForKind(kind), centerX, centerY, size, tilt, 1f, 1f, flap);
    }

    public static String nameForKind(int kind) {
        if (kind < 0) {
            return ALL_NAMES[0];
        }
        return ALL_NAMES[kind % ALL_NAMES.length];
    }

    private static Bitmap bitmapFor(Context context, String animalName) {
        int resId = resIdFor(animalName);
        if (resId == 0) {
            return null;
        }
        Bitmap cached = CACHE.get(resId);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap decoded = BitmapFactory.decodeResource(context.getResources(), resId);
        if (decoded != null) {
            CACHE.put(resId, decoded);
        }
        return decoded;
    }

    private static int resIdFor(String animalName) {
        if (animalName == null) {
            return 0;
        }
        switch (animalName) {
            case "Cat":
                return R.drawable.animal_cat;
            case "Dog":
                return R.drawable.animal_dog;
            case "Cow":
                return R.drawable.animal_cow;
            case "Duck":
                return R.drawable.animal_duck;
            case "Lion":
                return R.drawable.animal_lion;
            case "Elephant":
                return R.drawable.animal_elephant;
            case "Monkey":
                return R.drawable.animal_monkey;
            case "Horse":
                return R.drawable.animal_horse;
            case "Sheep":
                return R.drawable.animal_sheep;
            case "Pig":
                return R.drawable.animal_pig;
            case "Chicken":
                return R.drawable.animal_chicken;
            case "Frog":
                return R.drawable.animal_frog;
            case "Bee":
                return R.drawable.animal_bee;
            case "Owl":
                return R.drawable.animal_owl;
            case "Penguin":
                return R.drawable.animal_penguin;
            case "Turtle":
                return R.drawable.animal_turtle;
            case "Rabbit":
                return R.drawable.animal_rabbit;
            case "Bear":
                return R.drawable.animal_bear;
            case "Butterfly":
                return R.drawable.animal_butterfly;
            default:
                return 0;
        }
    }
}
