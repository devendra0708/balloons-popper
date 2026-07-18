package com.example.balloonspopper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Kids-friendly bubbly rainbow text: rounded font + candy colors + soft outline.
 */
public final class BubblyText {
    private static final int[] CANDY = {
            Color.rgb(255, 90, 130),   // pink
            Color.rgb(255, 160, 50),   // orange
            Color.rgb(255, 210, 50),   // yellow
            Color.rgb(90, 200, 110),   // green
            Color.rgb(70, 175, 255),   // sky
            Color.rgb(160, 120, 255),  // lilac
            Color.rgb(255, 110, 190)   // magenta
    };

    private static Typeface bubblyTypeface;

    private BubblyText() {
    }

    public static Typeface typeface(Context context) {
        if (bubblyTypeface == null) {
            try {
                bubblyTypeface = Typeface.createFromAsset(
                        context.getAssets(),
                        "fonts/fredoka_semibold.ttf"
                );
            } catch (RuntimeException ignored) {
                bubblyTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
            }
        }
        return bubblyTypeface;
    }

    public static void apply(Paint paint, Context context) {
        paint.setTypeface(typeface(context));
        paint.setFakeBoldText(false);
        paint.setAntiAlias(true);
    }

    public static int candyColor(int index) {
        int i = index % CANDY.length;
        if (i < 0) {
            i += CANDY.length;
        }
        return CANDY[i];
    }

    /**
     * Draws centered bubbly rainbow text. Optional bounce animates letters up/down.
     */
    public static void drawCentered(
            Canvas canvas,
            Paint paint,
            Context context,
            String text,
            float centerX,
            float baselineY,
            float textSize,
            float bounce,
            float timeSeconds
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        apply(paint, context);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStyle(Paint.Style.FILL);

        float totalWidth = paint.measureText(text);
        float x = centerX - totalWidth / 2f;
        float outline = Math.max(3f, textSize * 0.12f);

        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float letterW = paint.measureText(ch);
            if (ch.charAt(0) == ' ') {
                x += letterW;
                continue;
            }

            float bob = bounce <= 0f
                    ? 0f
                    : (float) Math.sin(timeSeconds * 4.2f + i * 0.55f) * bounce;
            float y = baselineY + bob;
            int fill = candyColor(i);

            // Soft dark outline for bubble sticker look
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(outline);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(Color.argb(200, 40, 50, 90));
            canvas.drawText(ch, x, y, paint);

            // White puffy middle ring
            paint.setStrokeWidth(outline * 0.55f);
            paint.setColor(Color.WHITE);
            canvas.drawText(ch, x, y, paint);

            // Candy fill
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            canvas.drawText(ch, x, y, paint);

            // Tiny highlight
            paint.setColor(Color.argb(90, 255, 255, 255));
            canvas.drawText(ch, x, y - textSize * 0.04f, paint);

            x += letterW;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAlpha(255);
    }

    /**
     * Draws centered bubbly text with one shared fill color (good for balloon numbers).
     */
    public static void drawCenteredUniform(
            Canvas canvas,
            Paint paint,
            Context context,
            String text,
            float centerX,
            float baselineY,
            float textSize,
            int fillColor
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        apply(paint, context);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        float outline = Math.max(3f, textSize * 0.12f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(outline);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.argb(210, 30, 40, 80));
        canvas.drawText(text, centerX, baselineY, paint);

        paint.setStrokeWidth(outline * 0.5f);
        paint.setColor(Color.WHITE);
        canvas.drawText(text, centerX, baselineY, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawText(text, centerX, baselineY, paint);

        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawText(text, centerX, baselineY - textSize * 0.04f, paint);
        paint.setAlpha(255);
    }

    /**
     * Single bubbly letter (for alphabet balloons).
     */
    public static void drawLetter(
            Canvas canvas,
            Paint paint,
            Context context,
            char letter,
            float centerX,
            float baselineY,
            float textSize,
            int fillColor
    ) {
        apply(paint, context);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        String ch = String.valueOf(letter);
        float outline = Math.max(3f, textSize * 0.14f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(outline);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.argb(210, 30, 40, 80));
        canvas.drawText(ch, centerX, baselineY, paint);

        paint.setStrokeWidth(outline * 0.5f);
        paint.setColor(Color.WHITE);
        canvas.drawText(ch, centerX, baselineY, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawText(ch, centerX, baselineY, paint);

        paint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawText(ch, centerX, baselineY - textSize * 0.05f, paint);
        paint.setAlpha(255);
    }
}
