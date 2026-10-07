package com.jbcgames.sunshine;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class AdvancedSettingsActivity extends Activity {

    private Spinner spRenderScale;
    private Spinner spFrameRate;
    private Spinner spWidescreen;
    private Spinner spWidescreenHud;
    private Spinner spAspect;
    private Spinner spDrawDistance;
    private Spinner spMsaa;
    private Spinner spAniso;

    private TextView tvFrameRateNote;

    private Switch swDisableShadows;
    private Switch swDisableReflections;
    private Switch swDisableWaterAnim;
    private Switch swDisableShimmer;
    private Switch swFxaa;

    private Button btnPresetLow;
    private Button btnPresetBalanced;
    private Button btnPresetHigh;
    private Button btnPresetUltra;
    private Button btnSave;
    private Button btnCancel;

    private static final String[] RENDER_SCALES = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x", "3.0x", "4.0x", "6.0x", "8.0x"};
    private static final String[] FRAME_RATES = {"30 FPS", "60 FPS"};
    private static final String[] WIDESCREEN_OPTIONS = {
        "Auto (Pantalla Nativa)", "16:9 (Panorámico Estándar)", "16:10 (Tablets / Laptops)",
        "21:9 (Ultrawide)", "32:9 (Super Ultrawide)", "4:3 (Original GameCube)"
    };
    private static final String[] WIDESCREEN_HUD_OPTIONS = {
        "Bordes de Pantalla (Edges)", "Centrado (Área 4:3)"
    };
    private static final String[] ASPECT_OPTIONS = {
        "Mantener Proporción (Keep)", "Estirar Pantalla (Stretch)"
    };
    private static final String[] DRAW_DISTANCES = {
        "0.5x (Bajo)", "0.7x (Medio)", "1.0x (Normal)", "1.5x (Alto)",
        "2.0x (Muy Alto)", "3.0x (Extremo)", "4.0x (Super)", "5.0x (Ultra 5x)"
    };
    private static final String[] MSAA_OPTIONS = {"0x (Desactivado)", "2x", "4x", "8x"};
    private static final String[] ANISO_OPTIONS = {"0x (Desactivado)", "2x", "4x", "8x", "16x"};

    private final Map<String, String> mSettings = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_settings);

        spRenderScale = findViewById(R.id.spRenderScale);
        spFrameRate = findViewById(R.id.spFrameRate);
        spWidescreen = findViewById(R.id.spWidescreen);
        spWidescreenHud = findViewById(R.id.spWidescreenHud);
        spAspect = findViewById(R.id.spAspect);
        spDrawDistance = findViewById(R.id.spDrawDistance);
        spMsaa = findViewById(R.id.spMsaa);
        spAniso = findViewById(R.id.spAniso);

        tvFrameRateNote = findViewById(R.id.tvFrameRateNote);

        swDisableShadows = findViewById(R.id.swDisableShadows);
        swDisableReflections = findViewById(R.id.swDisableReflections);
        swDisableWaterAnim = findViewById(R.id.swDisableWaterAnim);
        swDisableShimmer = findViewById(R.id.swDisableShimmer);
        swFxaa = findViewById(R.id.swFxaa);

        btnPresetLow = findViewById(R.id.btnPresetLow);
        btnPresetBalanced = findViewById(R.id.btnPresetBalanced);
        btnPresetHigh = findViewById(R.id.btnPresetHigh);
        btnPresetUltra = findViewById(R.id.btnPresetUltra);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        setupSpinners();
        setupSwitchListeners();

        btnPresetLow.setOnClickListener(v -> applyPresetLow());
        btnPresetBalanced.setOnClickListener(v -> applyPresetBalanced());
        btnPresetHigh.setOnClickListener(v -> applyPresetHigh());
        btnPresetUltra.setOnClickListener(v -> applyPresetUltra());

        btnSave.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "Ajustes guardados correctamente.", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnCancel.setOnClickListener(v -> finish());

        loadSettings();
    }

    private void setupSpinners() {
        spRenderScale.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, RENDER_SCALES));
        spFrameRate.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, FRAME_RATES));
        spWidescreen.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, WIDESCREEN_OPTIONS));
        spWidescreenHud.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, WIDESCREEN_HUD_OPTIONS));
        spAspect.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, ASPECT_OPTIONS));
        spDrawDistance.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, DRAW_DISTANCES));
        spMsaa.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, MSAA_OPTIONS));
        spAniso.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_white, ANISO_OPTIONS));

        spFrameRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (tvFrameRateNote != null) {
                    tvFrameRateNote.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (tvFrameRateNote != null) {
                    tvFrameRateNote.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupSwitchListeners() {
        swDisableShadows.setOnCheckedChangeListener((btn, isChecked) -> {
            swDisableShadows.setText(isChecked ? "🔴 Sombras: DESACTIVADAS (Mayor Rendimiento)" : "🟢 Sombras: ACTIVADAS (Original / Mayor Calidad)");
        });

        swDisableReflections.setOnCheckedChangeListener((btn, isChecked) -> {
            swDisableReflections.setText(isChecked ? "🔴 Reflejos: DESACTIVADOS (Mayor Rendimiento)" : "🟢 Reflejos: ACTIVADOS (Original / Mayor Calidad)");
        });

        swDisableWaterAnim.setOnCheckedChangeListener((btn, isChecked) -> {
            swDisableWaterAnim.setText(isChecked ? "🔴 Agua Compleja: DESACTIVADA (Mayor Rendimiento)" : "🟢 Agua Compleja: ACTIVADA (Original / Mayor Calidad)");
        });

        swDisableShimmer.setOnCheckedChangeListener((btn, isChecked) -> {
            swDisableShimmer.setText(isChecked ? "🔴 Efecto Brillo: DESACTIVADO (Mayor Rendimiento)" : "🟢 Efecto Brillo: ACTIVADO (Original / Mayor Calidad)");
        });

        swFxaa.setOnCheckedChangeListener((btn, isChecked) -> {
            swFxaa.setText(isChecked ? "🟢 Antialiasing FXAA: ACTIVADO (Filtro Suave)" : "🔴 Antialiasing FXAA: DESACTIVADO");
        });
    }

    private File getSettingsFile() {
        File dir = getExternalFilesDir(null);
        if (dir == null) dir = getFilesDir();
        return new File(dir, "settings.txt");
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private String getAutoAspect() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = Math.max(dm.widthPixels, dm.heightPixels);
        int h = Math.min(dm.widthPixels, dm.heightPixels);
        int g = gcd(w, h);
        if (g > 0) {
            return (w / g) + ":" + (h / g);
        }
        return "16:9";
    }

    private void loadSettings() {
        File f = getSettingsFile();
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        mSettings.put(key, val);
                    }
                }
            } catch (IOException ignored) {}
        }

        // Apply loaded settings to UI
        setSpinnerByValue(spRenderScale, mSettings.getOrDefault("render_scale", "0.5"), RENDER_SCALES);
        setSpinnerByValue(spFrameRate, mSettings.getOrDefault("frame_rate", "30") + " FPS", FRAME_RATES);

        // Widescreen loading
        String ws = mSettings.getOrDefault("widescreen", "auto");
        if ("off".equalsIgnoreCase(ws) || "0".equals(ws)) {
            spWidescreen.setSelection(5); // 4:3
        } else if ("16:9".equalsIgnoreCase(ws) || "1".equals(ws) || "on".equalsIgnoreCase(ws)) {
            spWidescreen.setSelection(1); // 16:9
        } else if ("16:10".equalsIgnoreCase(ws)) {
            spWidescreen.setSelection(2); // 16:10
        } else if ("21:9".equalsIgnoreCase(ws)) {
            spWidescreen.setSelection(3); // 21:9
        } else if ("32:9".equalsIgnoreCase(ws)) {
            spWidescreen.setSelection(4); // 32:9
        } else {
            spWidescreen.setSelection(0); // Auto
        }

        // Widescreen HUD
        String wsHud = mSettings.getOrDefault("widescreen_hud", "edges");
        spWidescreenHud.setSelection("centre".equalsIgnoreCase(wsHud) ? 1 : 0);

        // Aspect
        String aspect = mSettings.getOrDefault("aspect", "keep");
        spAspect.setSelection("stretch".equalsIgnoreCase(aspect) ? 1 : 0);

        setSpinnerByValue(spDrawDistance, mSettings.getOrDefault("draw_distance", "0.7"), DRAW_DISTANCES);
        setSpinnerByValue(spMsaa, mSettings.getOrDefault("msaa", "0"), MSAA_OPTIONS);
        setSpinnerByValue(spAniso, mSettings.getOrDefault("anisotropic", "0"), ANISO_OPTIONS);

        swDisableShadows.setChecked("1".equals(mSettings.getOrDefault("disable_shadows", "1")));
        swDisableReflections.setChecked("1".equals(mSettings.getOrDefault("disable_reflections", "1")));
        swDisableWaterAnim.setChecked("1".equals(mSettings.getOrDefault("disable_water_anim", "1")));
        swDisableShimmer.setChecked("1".equals(mSettings.getOrDefault("disable_shimmer", "1")));
        swFxaa.setChecked("1".equals(mSettings.getOrDefault("fxaa", "0")));

        // Trigger text updates
        swDisableShadows.setText(swDisableShadows.isChecked() ? "🔴 Sombras: DESACTIVADAS (Mayor Rendimiento)" : "🟢 Sombras: ACTIVADAS (Original / Mayor Calidad)");
        swDisableReflections.setText(swDisableReflections.isChecked() ? "🔴 Reflejos: DESACTIVADOS (Mayor Rendimiento)" : "🟢 Reflejos: ACTIVADOS (Original / Mayor Calidad)");
        swDisableWaterAnim.setText(swDisableWaterAnim.isChecked() ? "🔴 Agua Compleja: DESACTIVADA (Mayor Rendimiento)" : "🟢 Agua Compleja: ACTIVADA (Original / Mayor Calidad)");
        swDisableShimmer.setText(swDisableShimmer.isChecked() ? "🔴 Efecto Brillo: DESACTIVADO (Mayor Rendimiento)" : "🟢 Efecto Brillo: ACTIVADO (Original / Mayor Calidad)");
        swFxaa.setText(swFxaa.isChecked() ? "🟢 Antialiasing FXAA: ACTIVADO (Filtro Suave)" : "🔴 Antialiasing FXAA: DESACTIVADO");

        // Visibility of 60 FPS warning
        if (tvFrameRateNote != null) {
            tvFrameRateNote.setVisibility(spFrameRate.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void setSpinnerByValue(Spinner spinner, String targetVal, String[] options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(targetVal) || options[i].startsWith(targetVal + "x") || options[i].startsWith(targetVal + " ")) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private void applyPresetLow() {
        setSpinnerByValue(spRenderScale, "0.5", RENDER_SCALES);
        setSpinnerByValue(spFrameRate, "30", FRAME_RATES);
        spWidescreen.setSelection(1); // 16:9
        spWidescreenHud.setSelection(1); // centre
        spAspect.setSelection(0); // keep
        setSpinnerByValue(spDrawDistance, "0.7", DRAW_DISTANCES);
        setSpinnerByValue(spMsaa, "0", MSAA_OPTIONS);
        setSpinnerByValue(spAniso, "0", ANISO_OPTIONS);

        swDisableShadows.setChecked(true);
        swDisableReflections.setChecked(true);
        swDisableWaterAnim.setChecked(true);
        swDisableShimmer.setChecked(true);
        swFxaa.setChecked(false);
    }

    private void applyPresetBalanced() {
        setSpinnerByValue(spRenderScale, "0.75", RENDER_SCALES);
        setSpinnerByValue(spFrameRate, "30", FRAME_RATES);
        spWidescreen.setSelection(0); // Auto
        spWidescreenHud.setSelection(0); // edges
        spAspect.setSelection(0); // keep
        setSpinnerByValue(spDrawDistance, "1.0", DRAW_DISTANCES);
        setSpinnerByValue(spMsaa, "0", MSAA_OPTIONS);
        setSpinnerByValue(spAniso, "2", ANISO_OPTIONS);

        swDisableShadows.setChecked(false);
        swDisableReflections.setChecked(false);
        swDisableWaterAnim.setChecked(false);
        swDisableShimmer.setChecked(true);
        swFxaa.setChecked(false);
    }

    private void applyPresetHigh() {
        setSpinnerByValue(spRenderScale, "1.0", RENDER_SCALES);
        setSpinnerByValue(spFrameRate, "60", FRAME_RATES);
        spWidescreen.setSelection(0); // Auto
        spWidescreenHud.setSelection(0); // edges
        spAspect.setSelection(0); // keep
        setSpinnerByValue(spDrawDistance, "1.5", DRAW_DISTANCES);
        setSpinnerByValue(spMsaa, "2", MSAA_OPTIONS);
        setSpinnerByValue(spAniso, "4", ANISO_OPTIONS);

        swDisableShadows.setChecked(false);
        swDisableReflections.setChecked(false);
        swDisableWaterAnim.setChecked(false);
        swDisableShimmer.setChecked(false);
        swFxaa.setChecked(true);
    }

    private void applyPresetUltra() {
        setSpinnerByValue(spRenderScale, "8.0", RENDER_SCALES);
        setSpinnerByValue(spFrameRate, "60", FRAME_RATES);
        spWidescreen.setSelection(0); // Auto
        spWidescreenHud.setSelection(0); // edges
        spAspect.setSelection(0); // keep
        setSpinnerByValue(spDrawDistance, "5.0", DRAW_DISTANCES);
        setSpinnerByValue(spMsaa, "8", MSAA_OPTIONS);
        setSpinnerByValue(spAniso, "16", ANISO_OPTIONS);

        swDisableShadows.setChecked(false);
        swDisableReflections.setChecked(false);
        swDisableWaterAnim.setChecked(false);
        swDisableShimmer.setChecked(false);
        swFxaa.setChecked(true);
    }

    private void saveSettings() {
        String renderScale = RENDER_SCALES[spRenderScale.getSelectedItemPosition()].replace("x", "");
        String frameRate = spFrameRate.getSelectedItemPosition() == 1 ? "60" : "30";

        int wsIdx = spWidescreen.getSelectedItemPosition();
        String widescreen = wsIdx == 0 ? getAutoAspect() :
                            wsIdx == 1 ? "16:9" :
                            wsIdx == 2 ? "16:10" :
                            wsIdx == 3 ? "21:9" :
                            wsIdx == 4 ? "32:9" : "off";

        String widescreenHud = spWidescreenHud.getSelectedItemPosition() == 0 ? "edges" : "centre";
        String aspect = spAspect.getSelectedItemPosition() == 0 ? "keep" : "stretch";

        int drawIdx = spDrawDistance.getSelectedItemPosition();
        String drawDist = drawIdx == 0 ? "0.5" : drawIdx == 1 ? "0.7" : drawIdx == 2 ? "1.0" :
                         drawIdx == 3 ? "1.5" : drawIdx == 4 ? "2.0" : drawIdx == 5 ? "3.0" :
                         drawIdx == 6 ? "4.0" : "5.0";

        int msaaIdx = spMsaa.getSelectedItemPosition();
        String msaa = msaaIdx == 0 ? "0" : msaaIdx == 1 ? "2" : msaaIdx == 2 ? "4" : "8";

        int anisoIdx = spAniso.getSelectedItemPosition();
        String aniso = anisoIdx == 0 ? "0" : anisoIdx == 1 ? "2" : anisoIdx == 2 ? "4" : anisoIdx == 3 ? "8" : "16";

        File saveDirFile = new File(getExternalFilesDir(null), "card-a");
        if (!saveDirFile.exists()) {
            saveDirFile.mkdirs();
        }

        mSettings.put("render_scale", renderScale);
        mSettings.put("frame_rate", frameRate);
        mSettings.put("widescreen", widescreen);
        mSettings.put("widescreen_hud", widescreenHud);
        mSettings.put("aspect", aspect);
        mSettings.put("draw_distance", drawDist);
        mSettings.put("msaa", msaa);
        mSettings.put("anisotropic", aniso);

        mSettings.put("disable_shadows", swDisableShadows.isChecked() ? "1" : "0");
        mSettings.put("disable_reflections", swDisableReflections.isChecked() ? "1" : "0");
        mSettings.put("disable_water_anim", swDisableWaterAnim.isChecked() ? "1" : "0");
        mSettings.put("disable_shimmer", swDisableShimmer.isChecked() ? "1" : "0");
        mSettings.put("fxaa", swFxaa.isChecked() ? "1" : "0");

        mSettings.put("fast_peek", "1");
        mSettings.put("copy_writeback", "0");
        mSettings.put("shader_cache", "1");
        mSettings.put("vsync", "0");
        mSettings.put("save_dir", saveDirFile.getAbsolutePath());

        File f = getSettingsFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("# Super Mario Sunshine settings generated by Android Launcher");
            for (Map.Entry<String, String> entry : mSettings.entrySet()) {
                pw.println(entry.getKey() + " = " + entry.getValue());
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error guardando settings.txt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
