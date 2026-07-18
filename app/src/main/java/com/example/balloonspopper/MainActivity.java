package com.example.balloonspopper;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private enum Screen {
        SPLASH,
        HOME,
        SELECT,
        GAME
    }

    private AppPrefs prefs;
    private SoundHelper sounds;
    private SplashIntroView splashView;
    private HomeScreenView homeView;
    private GameSelectView selectView;
    private BalloonGameView gameView;
    private Screen screen = Screen.SPLASH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        prefs = new AppPrefs(this);
        sounds = new SoundHelper(this);
        sounds.setEnabled(prefs.isSoundEnabled());

        splashView = new SplashIntroView(this, sounds);
        homeView = new HomeScreenView(this, prefs, sounds);
        selectView = new GameSelectView(this, prefs, sounds);
        gameView = new BalloonGameView(this, prefs, sounds);

        splashView.setListener(new SplashIntroView.Listener() {
            @Override
            public void onFinished() {
                showHome();
            }
        });

        homeView.setListener(new HomeScreenView.Listener() {
            @Override
            public void onPlay() {
                showSelect();
            }

            @Override
            public void onHowToPlay() {
                showHowToPlay();
            }

            @Override
            public void onParents() {
                showParents();
            }

            @Override
            public void onToggleSound() {
                boolean enabled = !prefs.isSoundEnabled();
                prefs.setSoundEnabled(enabled);
                sounds.setEnabled(enabled);
                gameView.setSoundEnabled(enabled);
            }
        });

        selectView.setListener(new GameSelectView.Listener() {
            @Override
            public void onBack() {
                showHome();
            }

            @Override
            public void onSelectMode(GameMode mode) {
                showGame(mode);
            }

            @Override
            public void onNeedMoreStars(GameMode mode) {
                selectView.showToast("Need ⭐ " + mode.starCost + " stars");
            }

            @Override
            public void onUnlocked(GameMode mode) {
                selectView.showToast("Unlocked! Tap again to play");
                selectView.refresh();
            }
        });

        gameView.setListener(new BalloonGameView.Listener() {
            @Override
            public void onExitToMenu() {
                showSelect();
            }

            @Override
            public void onStarsChanged() {
                // Stars persist through AppPrefs; select screen refreshes on return.
            }
        });

        showSplash();
    }

    private void showSplash() {
        pauseGameIfNeeded();
        screen = Screen.SPLASH;
        setContentView(splashView);
        splashView.start();
    }

    private void showHome() {
        pauseGameIfNeeded();
        if (splashView != null) {
            splashView.stop();
        }
        screen = Screen.HOME;
        setContentView(homeView);
        homeView.resume();
        homeView.invalidate();
    }

    private void showSelect() {
        pauseGameIfNeeded();
        if (homeView != null) {
            homeView.pause();
        }
        screen = Screen.SELECT;
        selectView.refresh();
        setContentView(selectView);
        selectView.resume();
    }

    private void showGame(GameMode mode) {
        if (homeView != null) {
            homeView.pause();
        }
        if (selectView != null) {
            selectView.pause();
        }
        screen = Screen.GAME;
        gameView.setMode(mode);
        gameView.resetRound();
        setContentView(gameView);
        gameView.resume();
    }

    private void pauseGameIfNeeded() {
        if (gameView != null) {
            gameView.pause();
        }
        if (homeView != null) {
            homeView.pause();
        }
        if (splashView != null) {
            splashView.stop();
        }
        if (selectView != null) {
            selectView.pause();
        }
    }

    private void showHowToPlay() {
        new AlertDialog.Builder(this)
                .setTitle("How to Play")
                .setMessage("Tap the balloons before they float away!\n\n"
                        + "Some balloons have animals.\n"
                        + "Earn stars and unlock new games.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showParents() {
        new AlertDialog.Builder(this)
                .setTitle("Parents")
                .setMessage("Balloons Popper is a simple learning play app.\n\n"
                        + "• No ads or in-app purchases\n"
                        + "• Sound can be turned off on Home\n"
                        + "• Stars unlock extra game modes\n"
                        + "• Designed for short, calm play sessions")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (screen == Screen.SPLASH) {
            showHome();
        } else if (screen == Screen.GAME) {
            showSelect();
        } else if (screen == Screen.SELECT) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screen == Screen.SPLASH) {
            splashView.start();
        } else if (screen == Screen.HOME) {
            homeView.resume();
        } else if (screen == Screen.SELECT) {
            selectView.resume();
        } else if (screen == Screen.GAME) {
            gameView.resume();
        }
    }

    @Override
    protected void onPause() {
        pauseGameIfNeeded();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (splashView != null) {
            splashView.stop();
        }
        if (homeView != null) {
            homeView.pause();
        }
        if (gameView != null) {
            gameView.shutdown();
        }
        if (sounds != null) {
            sounds.release();
        }
        super.onDestroy();
    }
}
