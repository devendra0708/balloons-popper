package com.example.balloonspopper;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String PREFS = "balloons_popper_prefs";
    private static final String KEY_STARS = "stars";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_UNLOCK_PREFIX = "unlock_";

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getStars() {
        return prefs.getInt(KEY_STARS, 24);
    }

    public void setStars(int stars) {
        prefs.edit().putInt(KEY_STARS, Math.max(0, stars)).apply();
    }

    public void addStars(int amount) {
        setStars(getStars() + amount);
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public boolean isUnlocked(GameMode mode) {
        if (mode.starCost <= 0) {
            return true;
        }
        return prefs.getBoolean(KEY_UNLOCK_PREFIX + mode.id, false);
    }

    public boolean unlock(GameMode mode) {
        if (isUnlocked(mode)) {
            return true;
        }
        int stars = getStars();
        if (stars < mode.starCost) {
            return false;
        }
        setStars(stars - mode.starCost);
        prefs.edit().putBoolean(KEY_UNLOCK_PREFIX + mode.id, true).apply();
        return true;
    }
}
