package com.jbcgames.sunshine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.Executors;

public class LauncherActivity extends Activity {
    private static final int REQUEST_PICK_ISO = 1001;

    private TextView tvStatus;
    private TextView tvPath;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private Button btnPlay;
    private Button btnSelectIso;
    private Button btnAdvancedSettings;

    private File mFoundIsoFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        tvStatus = findViewById(R.id.tvStatus);
        tvPath = findViewById(R.id.tvPath);
        tvProgress = findViewById(R.id.tvProgress);
        progressBar = findViewById(R.id.progressBar);
        btnPlay = findViewById(R.id.btnPlay);
        btnSelectIso = findViewById(R.id.btnSelectIso);
        btnAdvancedSettings = findViewById(R.id.btnAdvancedSettings);

        btnPlay.setOnClickListener(v -> launchGame());

        btnSelectIso.setOnClickListener(v -> openFilePicker());

        btnAdvancedSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, AdvancedSettingsActivity.class));
        });

        checkIsoAndRefreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIsoAndRefreshUi();
    }

    private File findIsoFile() {
        // 1. External files directory (App private storage: Android/data/com.jbcgames.sunshine/files/GMSE01.iso)
        File extFiles = getExternalFilesDir(null);
        if (extFiles != null) {
            File f = new File(extFiles, "GMSE01.iso");
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) return f;
            f = new File(extFiles, "disc.iso");
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) return f;
        }

        // 2. Internal files directory
        File intFiles = getFilesDir();
        if (intFiles != null) {
            File f = new File(intFiles, "GMSE01.iso");
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) return f;
        }

        // 3. Common public storage paths (if readable)
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
            if (f.exists() && f.canRead() && f.length() > 10 * 1024 * 1024) {
                return f;
            }
        }

        return null;
    }

    private void checkIsoAndRefreshUi() {
        mFoundIsoFile = findIsoFile();
        if (mFoundIsoFile != null) {
            tvStatus.setText("✅ ISO lista para jugar");
            tvStatus.setTextColor(0xFF4CAF50); // Green
            double sizeGb = mFoundIsoFile.length() / (1024.0 * 1024.0 * 1024.0);
            tvPath.setText(String.format(Locale.getDefault(),
                "Archivo: %s (%.2f GB)\nRuta: %s",
                mFoundIsoFile.getName(), sizeGb, mFoundIsoFile.getAbsolutePath()));
            btnPlay.setVisibility(View.VISIBLE);
            btnPlay.setEnabled(true);
            btnSelectIso.setText("📂 SELECCIONAR OTRA ISO");
        } else {
            tvStatus.setText("⚠️ No se ha encontrado la ISO del juego");
            tvStatus.setTextColor(0xFFFFC107); // Yellow/Amber
            tvPath.setText("No se encontró ningún archivo GMSE01.iso accesible.\n\nPor favor pulsa el botón de abajo para seleccionar el archivo ISO de Super Mario Sunshine desde tu teléfono y copiarlo al almacenamiento interno de la app.");
            btnPlay.setVisibility(View.GONE);
            btnSelectIso.setText("📂 SELECCIONAR ISO DEL DISPOSITIVO");
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_PICK_ISO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_ISO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                copyIsoFromUri(uri);
            }
        }
    }

    private void copyIsoFromUri(Uri uri) {
        tvProgress.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        btnPlay.setEnabled(false);
        btnSelectIso.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            File destDir = getExternalFilesDir(null);
            if (destDir == null) {
                destDir = getFilesDir();
            }
            File destFile = new File(destDir, "GMSE01.iso");
            File tempFile = new File(destDir, "GMSE01.iso.tmp");

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempFile)) {

                if (in == null) {
                    throw new IOException("No se pudo abrir el archivo seleccionado.");
                }

                long totalBytes = -1;
                try {
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            totalBytes = cursor.getLong(sizeIndex);
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) {}

                byte[] buffer = new byte[128 * 1024]; // 128 KB
                long bytesCopied = 0;
                int bytesRead;
                long lastUpdate = System.currentTimeMillis();

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    bytesCopied += bytesRead;

                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 100) {
                        lastUpdate = now;
                        final long current = bytesCopied;
                        final long total = totalBytes;
                        runOnUiThread(() -> {
                            if (total > 0) {
                                int pct = (int) ((current * 100) / total);
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress(pct);
                                tvProgress.setText(String.format(Locale.getDefault(),
                                    "Copiando ISO a almacenamiento interno... %d%% (%.1f MB / %.1f MB)",
                                    pct, current / (1024.0 * 1024.0), total / (1024.0 * 1024.0)));
                            } else {
                                progressBar.setIndeterminate(true);
                                tvProgress.setText(String.format(Locale.getDefault(),
                                    "Copiando ISO... %.1f MB copiados",
                                    current / (1024.0 * 1024.0)));
                            }
                        });
                    }
                }
                out.flush();

                if (destFile.exists()) {
                    destFile.delete();
                }
                if (!tempFile.renameTo(destFile)) {
                    throw new IOException("No se pudo reemplazar el archivo final.");
                }

                runOnUiThread(() -> {
                    tvProgress.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "ISO copiada con éxito.", Toast.LENGTH_SHORT).show();
                    checkIsoAndRefreshUi();
                });

            } catch (Exception e) {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                runOnUiThread(() -> {
                    tvProgress.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    btnPlay.setEnabled(true);
                    btnSelectIso.setEnabled(true);
                    new AlertDialog.Builder(this)
                        .setTitle("Error al copiar la ISO")
                        .setMessage("Ocurrió un error al copiar el archivo: " + e.getMessage())
                        .setPositiveButton("Aceptar", null)
                        .show();
                });
            }
        });
    }

    private void launchGame() {
        if (mFoundIsoFile != null && mFoundIsoFile.exists()) {
            Intent intent = new Intent(this, SunshineActivity.class);
            intent.putExtra("sms_disc", mFoundIsoFile.getAbsolutePath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Por favor selecciona una ISO válida primero.", Toast.LENGTH_SHORT).show();
        }
    }
}
