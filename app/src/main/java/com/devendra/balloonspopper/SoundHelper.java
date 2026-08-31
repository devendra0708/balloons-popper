package com.devendra.balloonspopper;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Build;

public final class SoundHelper {
    private final Context context;
    private ToneGenerator toneGenerator;
    private SoundPool soundPool;
    private int catSoundId;
    private int dogSoundId;
    private int cowSoundId;
    private int duckSoundId;
    private int balloonPopSoundId;
    private boolean soundsLoaded;
    private boolean enabled = true;

    public SoundHelper(Context context) {
        this.context = context.getApplicationContext();
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
        initSoundPool();
    }

    private void initSoundPool() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(4)
                        .setAudioAttributes(attributes)
                        .build();
            } else {
                soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
            }
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status == 0) {
                    soundsLoaded = true;
                }
            });
            catSoundId = soundPool.load(this.context, R.raw.cat_meow, 1);
            dogSoundId = soundPool.load(this.context, R.raw.dog_bark, 1);
            cowSoundId = soundPool.load(this.context, R.raw.cow_moo, 1);
            duckSoundId = soundPool.load(this.context, R.raw.duck_quack, 1);
            balloonPopSoundId = soundPool.load(this.context, R.raw.balloon_pop, 1);
        } catch (RuntimeException ignored) {
            soundPool = null;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void playTap() {
        play(ToneGenerator.TONE_PROP_BEEP, 80);
    }

    public void playPlay() {
        play(ToneGenerator.TONE_CDMA_CONFIRM, 120);
    }

    public void playUnlock() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 160);
    }

    public void playLocked() {
        play(ToneGenerator.TONE_PROP_NACK, 100);
    }

    public void playWrong() {
        play(ToneGenerator.TONE_PROP_NACK, 90);
    }

    public void playSuccess() {
        play(ToneGenerator.TONE_PROP_ACK, 90);
    }

    public void playBalloonPop() {
        if (!enabled || soundPool == null || balloonPopSoundId == 0) {
            return;
        }
        soundPool.play(balloonPopSoundId, 0.88f, 0.88f, 1, 0, 1f);
    }

    public boolean playAnimal(String animalName) {
        if (!enabled || soundPool == null || animalName == null) {
            return false;
        }

        int soundId;
        switch (animalName) {
            case "Cat":
                soundId = catSoundId;
                break;
            case "Dog":
                soundId = dogSoundId;
                break;
            case "Cow":
                soundId = cowSoundId;
                break;
            case "Duck":
                soundId = duckSoundId;
                break;
            default:
                return false;
        }

        if (soundId == 0) {
            return false;
        }

        // Play immediately at full volume; SoundPool is low-latency for game SFX.
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        return true;
    }

    private void play(int tone, int durationMs) {
        if (!enabled || toneGenerator == null) {
            return;
        }
        try {
            toneGenerator.startTone(tone, durationMs);
        } catch (RuntimeException ignored) {
            // Ignore audio glitches on older devices.
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
