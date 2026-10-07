package com.jbcgames.sunshine;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SunshineActivity extends SDLActivity {
    private static final String TAG = "SunshineActivity";

    private static String[] splitArgs(String raw) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;

        for (int i = 0; i < raw.length(); ++i) {
            char c = raw.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && !inSingle) {
                escaped = true;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && !inDouble && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    out.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out.toArray(new String[0]);
    }

    private String findDefaultDiscPath() {
        File extFiles = getExternalFilesDir(null);
        if (extFiles != null) {
            File f = new File(extFiles, "GMSE01.iso");
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) return f.getAbsolutePath();
            f = new File(extFiles, "disc.iso");
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) return f.getAbsolutePath();
        }

        File intFiles = getFilesDir();
        if (intFiles != null) {
            File f = new File(intFiles, "GMSE01.iso");
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) return f.getAbsolutePath();
        }

        String[] candidates = new String[] {
            "/sdcard/Download/GMSE01.iso",
            "/sdcard/Download/Super Mario Sunshine (USA).iso",
            "/sdcard/Download/Super Mario Sunshine.iso",
            "/sdcard/sms/GMSE01.iso",
            "/sdcard/sms/Super Mario Sunshine.iso",
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/GMSE01.iso",
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Super Mario Sunshine.iso",
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Super Mario Sunshine (USA).iso",
        };

        for (String path : candidates) {
            File f = new File(path);
            if (f.exists() && f.canRead() && f.length() > 1024 * 1024) {
                return f.getAbsolutePath();
            }
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] args = getArguments();
        if (args.length == 0) {
            mBrokenLibraries = true;
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage("No se encontró la ISO del juego (GMSE01.iso).\n\nPor favor copia el archivo de la ISO del juego a la carpeta Descargas de tu dispositivo:\n/sdcard/Download/GMSE01.iso");
            dlgAlert.setTitle("Juego no encontrado");
            dlgAlert.setPositiveButton("Salir", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
            return;
        }

        // Lock to landscape (adapts to both landscape angles)
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // Keep screen on while playing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();
    }

    @Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        // Enforce sensor landscape regardless of window resizable flag
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            "main"
        };
    }

    @Override
    protected String[] getArguments() {
        Intent intent = getIntent();
        if (intent != null) {
            String[] argv = intent.getStringArrayExtra("sms_argv");
            if (argv != null && argv.length > 0) {
                return argv;
            }

            String rawArgs = intent.getStringExtra("sms_args");
            if (rawArgs != null && !rawArgs.trim().isEmpty()) {
                return splitArgs(rawArgs.trim());
            }

            String discExtra = intent.getStringExtra("sms_disc");
            if (discExtra != null && !discExtra.trim().isEmpty()) {
                return new String[] { discExtra.trim() };
            }
        }

        String autoDisc = findDefaultDiscPath();
        if (autoDisc != null) {
            return new String[] { autoDisc };
        }

        return new String[0];
    }
}
