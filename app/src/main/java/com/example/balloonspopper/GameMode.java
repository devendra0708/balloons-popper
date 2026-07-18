package com.example.balloonspopper;

import android.graphics.Color;

public enum GameMode {
    BALLOON_POP(
            "balloon_pop",
            "Balloon Pop",
            "Tap balloons before they float away",
            0,
            Color.rgb(255, 105, 140)
    ),
    COLOR_POP(
            "color_pop",
            "Color Pop",
            "Pop the color shown at the top",
            8,
            Color.rgb(255, 82, 82)
    ),
    NUMBER_POP(
            "number_pop",
            "Number Pop",
            "Pop balloons in order: 1, 2, 3…",
            12,
            Color.rgb(76, 175, 80)
    ),
    LETTER_POP(
            "letter_pop",
            "Letter Pop",
            "Pop the letter shown at the top",
            16,
            Color.rgb(33, 150, 243)
    ),
    SHAPE_POP(
            "shape_pop",
            "Shape Pop",
            "Pop circle, star, heart, and more",
            20,
            Color.rgb(171, 71, 188)
    ),
    SPEED_CHALLENGE(
            "speed_challenge",
            "Speed Challenge",
            "Pop as many as you can before time ends",
            24,
            Color.rgb(255, 167, 38)
    );

    public final String id;
    public final String title;
    public final String blurb;
    public final int starCost;
    public final int accentColor;

    GameMode(String id, String title, String blurb, int starCost, int accentColor) {
        this.id = id;
        this.title = title;
        this.blurb = blurb;
        this.starCost = starCost;
        this.accentColor = accentColor;
    }
}
