package io.github.p5rrecomp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pause overlay for the native game; settings are saved for the next runtime start. */
public final class PauseActivity extends Activity {
    static { System.loadLibrary("p5r"); }

    static final int RED = P5Theme.P5_RED;
    static final int YELLOW = P5Theme.ACID_YELLOW;
    static final int WHITE = P5Theme.PAPER_WHITE;
    static final int MUTED = P5Theme.MUTED_TEXT;
    static final String MAP_KEY = "NX_GAMEPAD_MAP";
    static final String[] MAP_ACTIONS_ES = {"A", "B", "X", "Y", "L", "R", "ZL", "ZR", "+", "−", "L3", "R3",
            "D-pad izquierda", "D-pad arriba", "D-pad derecha", "D-pad abajo"};
    static final String[] MAP_ACTIONS_EN = {"A", "B", "X", "Y", "L", "R", "ZL", "ZR", "+", "−", "L3", "R3",
            "D-pad left", "D-pad up", "D-pad right", "D-pad down"};

    static final class Choice {
        final String category, key, es, en;
        final String[] values, labelsEs, labelsEn;
        Choice(String category, String key, String es, String en, String[] values, String[] labelsEs, String[] labelsEn) {
            this.category = category;
            this.key = key;
            this.es = es;
            this.en = en;
            this.values = values;
            this.labelsEs = labelsEs;
            this.labelsEn = labelsEn;
        }
    }

    final List<Choice> choices = new ArrayList<>();
    final Map<String, String> config = new LinkedHashMap<>();
    final Map<String, Button> optionButtons = new LinkedHashMap<>();
    final List<Button> categoryButtons = new ArrayList<>();
    final List<Button> verticalNavigation = new ArrayList<>();
    final String[] categories = {
            t("Video", "Video"), t("Rendimiento", "Performance"), t("Efectos", "Effects"),
            t("Interfaz", "Interface"), t("Juego", "Game"), t("Mandos", "Controllers")
    };
    LinearLayout settingsPanel;
    ScrollView categoryScroll, optionScroll;
    Button resumeButton, quitButton, firstOptionButton;
    int selectedCategory;
    AlertDialog choiceDialog;
    ScrollView choiceDialogScroll;
    List<Button> choiceDialogRows;
    Choice choiceDialogChoice;
    Button choiceDialogValue;
    int choiceDialogSelection;

    static String t(String es, String en) { return Lang.t(es, en); }

    int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        loadOptions();
        config.putAll(Launcher.readConfig(new File(Launcher.dataDir(), "config.txt")));
        if (!config.containsKey("NX_HUD_RES"))
            config.put("NX_HUD_RES", "1".equals(config.get("NX_HUD_1080")) ? "1080p" : "720p");
        buildScreen();
    }

    void loadOptions() {
        choices.add(new Choice(categories[0], "RESOLUTION", "Resolución interna", "Internal resolution",
                new String[] {"360p", "480p", "720p", "1080p", "1440p", "2160p", "native"},
                new String[] {"360p", "480p", "720p", "1080p", "1440p (Experimental)", "2160p (Experimental)",
                        "Nativa del teléfono (Experimental)"},
                new String[] {"360p", "480p", "720p", "1080p", "1440p (Experimental)", "2160p (Experimental)",
                        "Phone native (Experimental)"}));
        choices.add(toggle(categories[0], "NX_STRETCH", "Pantalla completa (estirar)", "Fullscreen (stretch)", false));
        choices.add(new Choice(categories[0], "NX_ULTRAWIDE", "Pantalla ancha real (Hor+)", "True widescreen (Hor+)",
                new String[] {"0", "1"}, new String[] {"Desactivada", "Automática · más campo de visión"},
                new String[] {"Off", "Auto · wider field of view"}));
        choices.add(toggle(categories[0], "NX_FSR", "Superresolución FSR", "FSR super resolution", false));
        choices.add(new Choice(categories[0], "NX_HUD_RES", "Resolución de interfaz", "Interface resolution",
                new String[] {"360p", "480p", "720p", "1080p", "1440p", "2160p", "native"},
                new String[] {"360p", "480p", "720p", "1080p", "1440p · Experimental", "2160p · Experimental", "Nativa del teléfono · Experimental"},
                new String[] {"360p", "480p", "720p", "1080p", "1440p · Experimental", "2160p · Experimental", "Phone native · Experimental"}));
        choices.add(new Choice(categories[0], "NX_UW_STRETCH_UI", "Interfaz Hor+", "Hor+ interface", new String[] {"0", "1"},
                new String[] {"Centrada en 16:9", "Pantalla completa"}, new String[] {"Centered 16:9", "Full width"}));

        choices.add(new Choice(categories[1], "GRAPHICS", "Calidad de gráficos", "Graphics quality",
                new String[] {"low", "medium", "high", "sharp"},
                new String[] {"Bajos", "Medios", "Altos", "Nítido"},
                new String[] {"Low", "Medium", "High", "Sharp"}));
        choices.add(new Choice(categories[1], "NX_MAX_ANISO", "Filtro anisotrópico", "Anisotropic filtering",
                new String[] {"auto", "1", "2", "4", "8", "16"},
                new String[] {"Automático", "1x · desactivado", "2x", "4x", "8x", "16x"},
                new String[] {"Auto", "1x · off", "2x", "4x", "8x", "16x"}));
        choices.add(new Choice(categories[1], "NX_FPS_CAP", "Límite de fotogramas", "Frame rate limit",
                new String[] {"30", "60"}, new String[] {"30 fps", "60 fps · experimental"},
                new String[] {"30 fps", "60 fps · experimental"}));
        choices.add(toggle(categories[1], "NX_SHADOWS", "Sombras", "Shadows", false));
        choices.add(new Choice(categories[1], "NX_MALI_LOW_SHADOWS", "Calidad de sombras", "Shadow quality",
                new String[] {"1", "0"}, new String[] {"Baja · más rendimiento", "Original"},
                new String[] {"Low · better performance", "Original"}));

        choices.add(new Choice(categories[2], "NX_NO_CROWD", "Multitudes de NPC", "NPC crowds",
                new String[] {"0", "1"}, new String[] {"Activadas", "Desactivadas · más FPS"},
                new String[] {"Enabled", "Disabled · more FPS"}));
        choices.add(new Choice(categories[2], "NX_DISABLE_BLOOM", "Bloom y brillo", "Bloom and glow",
                new String[] {"0", "1"}, new String[] {"Activados", "Desactivados · más FPS"},
                new String[] {"Enabled", "Disabled · more FPS"}));

        choices.add(toggle(categories[3], "NX_SHOW_FPS", "Contador de FPS", "FPS counter", false));
        choices.add(new Choice(categories[3], "NX_TOUCH_SHOW", "Controles táctiles", "Touch controls",
                new String[] {"1", "0"}, new String[] {"Activados", "Desactivados"}, new String[] {"On", "Off"}));
        choices.add(toggle(categories[3], "NX_TOUCH_HIDE", "Ocultar controles táctiles", "Hide touch controls", false));
        choices.add(new Choice(categories[3], "NX_TOUCH_SIZE", "Tamaño de botones táctiles", "Touch button size",
                new String[] {"80", "100", "125"}, new String[] {"Pequeño", "Normal", "Grande"},
                new String[] {"Small", "Normal", "Large"}));
        choices.add(new Choice(categories[3], "NX_TOUCH_OPACITY", "Opacidad de controles", "Control opacity",
                new String[] {"50", "85", "100"}, new String[] {"Baja", "Normal", "Alta"},
                new String[] {"Low", "Normal", "High"}));
        choices.add(new Choice(categories[3], "NX_STICK_SENSITIVITY", "Sensibilidad de palanca", "Stick sensitivity",
                new String[] {"50", "75", "100", "125", "150", "200"},
                new String[] {"50%", "75%", "100%", "125%", "150%", "200%"},
                new String[] {"50%", "75%", "100%", "125%", "150%", "200%"}));
        choices.add(new Choice(categories[3], "NX_CAMERA_SENSITIVITY", "Sensibilidad de cámara", "Camera sensitivity",
                new String[] {"50", "75", "100", "125", "150", "200", "250"},
                new String[] {"50%", "75%", "100%", "125%", "150%", "200%", "250%"},
                new String[] {"50%", "75%", "100%", "125%", "150%", "200%", "250%"}));

        choices.add(new Choice(categories[4], "language", "Idioma de textos y subtítulos", "Text and subtitle language",
                new String[] {"auto", "en-US", "es", "fr", "de", "it", "ja", "ko", "zh-Hans", "zh-Hant"},
                new String[] {"Automático", "English", "Español", "Français", "Deutsch", "Italiano", "日本語", "한국어", "简体中文", "繁體中文"},
                new String[] {"Automatic", "English", "Español", "Français", "Deutsch", "Italiano", "日本語", "한국어", "简体中文", "繁體中文"}));
        choices.add(new Choice(categories[4], "NX_COLOR_PROFILE", "Perfil de color", "Color profile",
                new String[] {"0", "1", "2"}, new String[] {"Normal", "Vívido", "P5 Pop"},
                new String[] {"Default", "Vivid", "P5 Pop"}));
        choices.add(new Choice(categories[5], "NX_GAMEPAD_LAYOUT", "Distribución A/B", "A/B layout",
                new String[] {"0", "1"}, new String[] {"Nintendo · A a la derecha", "Xbox · A abajo"},
                new String[] {"Nintendo · A on right", "Xbox · A on bottom"}));
    }

    Choice toggle(String category, String key, String es, String en, boolean defaultOn) {
        String off = t("Desactivado", "Off"), on = t("Activado", "On");
        return new Choice(category, key, es, en, new String[] {defaultOn ? "1" : "0", defaultOn ? "0" : "1"},
                defaultOn ? new String[] {on, off} : new String[] {off, on},
                defaultOn ? new String[] {on, off} : new String[] {off, on});
    }

    void buildScreen() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(P5Theme.SCRIM_SETTINGS);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(18), dp(28), dp(16));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("PERSONA 5 ROYAL", YELLOW, 27, true);
        title.setTypeface(P5Theme.displayBold());
        titleBox.addView(title);
        titleBox.addView(text(t("EN PAUSA", "PAUSED"), MUTED, 15, true));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        quitButton = actionButton(t("↪  Salir", "↪  Quit"), false);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { returnToLauncher(); }
        });
        header.addView(quitButton, buttonParams(170, 58));
        resumeButton = actionButton(t("▶  Continuar", "▶  Continue"), false);
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        LinearLayout.LayoutParams resumeParams = buttonParams(220, 58);
        resumeParams.leftMargin = dp(12);
        header.addView(resumeButton, resumeParams);
        root.addView(header);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        bodyParams.topMargin = dp(16);
        bodyParams.bottomMargin = dp(8);
        root.addView(body, bodyParams);

        categoryScroll = new ScrollView(this);
        categoryScroll.setVerticalScrollBarEnabled(true);
        categoryScroll.setScrollbarFadingEnabled(false);
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        categoryScroll.addView(side, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        body.addView(categoryScroll, new LinearLayout.LayoutParams(dp(210), LinearLayout.LayoutParams.MATCH_PARENT));
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setPadding(dp(22), dp(18), dp(22), dp(14));
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(P5Theme.INK_SURFACE);
        panelBg.setCornerRadius(dp(2));
        panelBg.setStroke(dp(1), P5Theme.BORDER_SUBTLE);
        settingsPanel.setBackground(panelBg);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        panelParams.leftMargin = dp(16);
        body.addView(settingsPanel, panelParams);

        for (int i = 0; i < categories.length; i++) {
            final int index = i;
            Button category = actionButton(categories[i], i == selectedCategory);
            category.setTag(Boolean.valueOf(i == selectedCategory));
            category.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            category.setTextSize(17);
            category.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectedCategory = index;
                    refreshCategories();
                    showCategory();
                    scrollCategoryIntoView(category);
                    focusFirstSetting();
                }
            });
            LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(62));
            catParams.bottomMargin = dp(8);
            side.addView(category, catParams);
            categoryButtons.add(category);
        }

        TextView hint = text(t("L1/R1: categorías  ·  ↑/↓: opciones  ·  A: seleccionar  ·  B: volver. Mando, FPS y controles táctiles se aplican al instante.",
                "L1/R1: categories  ·  ↑/↓: options  ·  A: select  ·  B: back. Gamepad, FPS and touch controls apply immediately."), MUTED, 12, false);
        hint.setPadding(dp(4), 0, 0, 0);
        root.addView(hint);

        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        frame.addView(root, full);
        setContentView(frame);
        showCategory();
        focusWhenReady(resumeButton);
    }

    void refreshCategories() {
        for (int i = 0; i < categoryButtons.size(); i++) {
            boolean active = i == selectedCategory;
            Button b = categoryButtons.get(i);
            b.setTag(Boolean.valueOf(active));
            styleMenuButton(b, active || b.hasFocus());
        }
    }

    void showCategory() {
        settingsPanel.removeAllViews();
        TextView heading = text(categories[selectedCategory], YELLOW, 23, true);
        settingsPanel.addView(heading);
        View accent = new View(this);
        accent.setBackgroundColor(RED);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(72), dp(4));
        accentParams.topMargin = dp(10);
        accentParams.bottomMargin = dp(12);
        settingsPanel.addView(accent, accentParams);

        optionScroll = new ScrollView(this);
        optionScroll.setFillViewport(false);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        verticalNavigation.clear();
        verticalNavigation.add(resumeButton);
        verticalNavigation.add(quitButton);
        optionButtons.clear();
        firstOptionButton = null;
        for (final Choice choice : choices) if (choice.category.equals(categories[selectedCategory])) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
            TextView label = text(t(choice.es, choice.en), WHITE, 16, false);
            label.setTypeface(P5Theme.condensedBold());
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            Button value = actionButton(choiceLabel(choice), false);
            value.setTextSize(15);
            value.setMinWidth(dp(150));
            value.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (choice.values.length > 2) showChoicePicker(choice, (Button) v);
                    else advance(choice, (Button) v);
                }
            });
            if ("NX_MALI_LOW_SHADOWS".equals(choice.key) && !"1".equals(config.get("NX_SHADOWS"))) {
                value.setEnabled(false);
                value.setAlpha(0.4f);
            }
            optionButtons.put(choice.key, value);
            if (firstOptionButton == null) firstOptionButton = value;
            verticalNavigation.add(value);
            row.addView(value);
            rows.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));
            View divider = new View(this);
            divider.setBackgroundColor(P5Theme.BORDER_SUBTLE);
            rows.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        }
        if (categories[selectedCategory].equals(categories[5])) addGamepadMapperRow(rows);
        if (categories[selectedCategory].equals(categories[3])) addControlsPositionRow(rows);
        optionScroll.addView(rows);
        settingsPanel.addView(optionScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
    }

    void addGamepadMapperRow(LinearLayout rows) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(t("Asignar botones del mando", "Remap gamepad buttons"), WHITE, 16, false);
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button configure = actionButton(t("Configurar", "Configure"), false);
        configure.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showGamepadMapper(readGamepadMap()); }
        });
        if (firstOptionButton == null) firstOptionButton = configure;
        verticalNavigation.add(configure);
        row.addView(configure);
        rows.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(66)));
    }

    void addControlsPositionRow(LinearLayout rows) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(t("Posición de los controles táctiles", "Touch control positions"), WHITE, 16, false);
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button edit = actionButton(t("Mover", "Move"), false);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(PauseActivity.this, ControlsEditor.class)); }
        });
        if (firstOptionButton == null) firstOptionButton = edit;
        verticalNavigation.add(edit);
        row.addView(edit);
        rows.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(66)));
    }

    int currentChoiceIndex(Choice choice) {
        String current = config.get(choice.key);
        for (int i = 0; i < choice.values.length; i++) if (choice.values[i].equals(current)) return i;
        return 0;
    }

    void advance(Choice choice, Button valueButton) {
        selectChoice(choice, (currentChoiceIndex(choice) + 1) % choice.values.length, valueButton);
    }

    boolean selectChoice(Choice choice, int index, Button valueButton) {
        Map<String, String> previous = new LinkedHashMap<>(config);
        config.put(choice.key, choice.values[index]);
        if ("NX_ULTRAWIDE".equals(choice.key) && "1".equals(config.get(choice.key))) {
            config.put("NX_STRETCH", "0");
        } else if ("NX_STRETCH".equals(choice.key) && "1".equals(config.get(choice.key))) {
            config.put("NX_ULTRAWIDE", "0");
        }
        if ("RESOLUTION".equals(choice.key)) {
            String resolution = config.get(choice.key);
            config.put("resolution", resolution);
            config.put("NX_RENDER_RES", resolution);
            config.put("NX_RES", resolution);
            config.put("NX_HANDHELD", "360p".equals(resolution) || "480p".equals(resolution)
                    || "720p".equals(resolution) ? "1" : "0");
        } else if ("GRAPHICS".equals(choice.key)) {
            config.remove("NX_LOD_BIAS");
            String preset = config.get(choice.key);
            if ("low".equals(preset)) config.put("NX_LOD_BIAS", "1");
            else if ("medium".equals(preset)) config.put("NX_LOD_BIAS", "0.5");
            else if ("sharp".equals(preset)) config.put("NX_LOD_BIAS", "-0.5");
        }
        if (persist()) {
            valueButton.setText(choiceLabel(choice));
            for (String siblingKey : new String[] {"NX_STRETCH", "NX_ULTRAWIDE"}) {
                if (siblingKey.equals(choice.key)) continue;
                Button sibling = optionButtons.get(siblingKey);
                if (sibling != null) {
                    for (Choice candidate : choices)
                        if (siblingKey.equals(candidate.key)) sibling.setText(choiceLabel(candidate));
                }
            }
            applyRuntimeOption(choice.key);
            if ("NX_ULTRAWIDE".equals(choice.key) || "NX_MAX_ANISO".equals(choice.key)
                    || "NX_HUD_RES".equals(choice.key) || "NX_UW_STRETCH_UI".equals(choice.key)
                    || "NX_COLOR_PROFILE".equals(choice.key))
                Toast.makeText(this, t("Este ajuste se aplica al iniciar el juego.",
                        "This setting takes effect the next time the game starts."), Toast.LENGTH_SHORT).show();
            if ("NX_SHADOWS".equals(choice.key)) {
                Button quality = optionButtons.get("NX_MALI_LOW_SHADOWS");
                if (quality != null) {
                    boolean enabled = "1".equals(config.get("NX_SHADOWS"));
                    quality.setEnabled(enabled);
                    quality.setAlpha(enabled ? 1.0f : 0.4f);
                }
            }
            return true;
        } else {
            config.clear();
            config.putAll(previous);
            return false;
        }
    }

    String choiceLabel(Choice choice) {
        String current = config.get(choice.key);
        for (int i = 0; i < choice.values.length; i++)
            if (choice.values[i].equals(current)) return Lang.t(choice.labelsEs[i], choice.labelsEn[i]);
        return Lang.t(choice.labelsEs[0], choice.labelsEn[0]);
    }

    void showChoicePicker(final Choice choice, final Button valueButton) {
        choiceDialogChoice = choice;
        choiceDialogValue = valueButton;
        choiceDialogSelection = currentChoiceIndex(choice);
        choiceDialogRows = new ArrayList<>();
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        choiceDialog = dialog;
        LinearLayout card = dialogCard();
        card.addView(text(Lang.t(choice.es, choice.en).toUpperCase(java.util.Locale.ROOT), YELLOW, 21, true));
        card.addView(dialogAccent());
        TextView description = text(t("Selecciona una opción. Usa ↑/↓ o el deslizador para recorrer la lista.",
                "Choose an option. Use ↑/↓ or the scrollbar to browse the list."), MUTED, 14, false);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.bottomMargin = dp(8);
        card.addView(description, descriptionParams);

        choiceDialogScroll = new ScrollView(this);
        choiceDialogScroll.setFillViewport(false);
        choiceDialogScroll.setVerticalScrollBarEnabled(true);
        choiceDialogScroll.setScrollbarFadingEnabled(false);
        choiceDialogScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < choice.values.length; i++) {
            final int index = i;
            Button option = actionButton(Lang.t(choice.labelsEs[i], choice.labelsEn[i]), i == choiceDialogSelection);
            option.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            option.setPadding(dp(14), 0, dp(10), 0);
            option.setTextSize(15);
            option.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    choiceDialogSelection = index;
                    if (selectChoice(choice, index, valueButton)) dialog.dismiss();
                }
            });
            choiceDialogRows.add(option);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48));
            rowParams.bottomMargin = dp(5);
            list.addView(option, rowParams);
        }
        choiceDialogScroll.addView(list);
        int visibleRows = Math.max(2, Math.min(5, choice.values.length - 1));
        int availableHeight = getResources().getDisplayMetrics().heightPixels - dp(216);
        int listHeight = Math.min(dp(visibleRows * 53), Math.max(dp(96), availableHeight));
        card.addView(choiceDialogScroll, new LinearLayout.LayoutParams(-1, listHeight));

        Button close = actionButton(t("Cerrar", "Close"), false);
        close.setTextSize(14);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-2, dp(44));
        closeParams.topMargin = dp(8);
        closeParams.gravity = Gravity.END;
        card.addView(close, closeParams);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) {
                if (choiceDialog == dialog) {
                    choiceDialog = null;
                    choiceDialogScroll = null;
                    choiceDialogRows = null;
                    choiceDialogChoice = null;
                    choiceDialogValue = null;
                }
            }
        });
        dialog.setView(card);
        dialog.show();
        styleDialogWindow(dialog, 600, Math.round(listHeight / getResources().getDisplayMetrics().density) + 180);
        focusChoiceDialogRow(choiceDialogSelection);
    }

    void focusChoiceDialogRow(int index) {
        if (choiceDialogRows == null || choiceDialogRows.isEmpty()) return;
        choiceDialogSelection = (index + choiceDialogRows.size()) % choiceDialogRows.size();
        dialogCardFocus(choiceDialogRows, choiceDialogSelection);
        final Button target = choiceDialogRows.get(choiceDialogSelection);
        final ScrollView scroll = choiceDialogScroll;
        if (scroll != null) target.post(new Runnable() {
            @Override public void run() {
                android.graphics.Rect rect = new android.graphics.Rect();
                target.getDrawingRect(rect);
                scroll.offsetDescendantRectToMyCoords(target, rect);
                int top = scroll.getScrollY();
                int bottom = top + scroll.getHeight();
                if (rect.top < top) scroll.smoothScrollTo(0, rect.top);
                else if (rect.bottom > bottom) scroll.smoothScrollTo(0, rect.bottom - scroll.getHeight());
            }
        });
    }

    boolean handleChoiceDialogKey(KeyEvent event) {
        if (choiceDialog == null || !choiceDialog.isShowing()) return false;
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN)
                focusChoiceDialogRow(choiceDialogSelection + (key == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1));
            return true;
        }
        if (key == KeyEvent.KEYCODE_BUTTON_A) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && choiceDialogRows != null)
                choiceDialogRows.get(choiceDialogSelection).performClick();
            return true;
        }
        if (key == KeyEvent.KEYCODE_BUTTON_B || key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) choiceDialog.cancel();
            return true;
        }
        return false;
    }

    boolean persist() {
        File dir = Launcher.dataDir();
        if (!dir.exists()) dir.mkdirs();
        config.put("NX_HUD_1080", "720p".equals(config.get("NX_HUD_RES")) ? "0" : "1");
        boolean saved = Launcher.writeConfig(new File(dir, "config.txt"), config);
        if (!saved) Toast.makeText(this, t("No se pudo guardar la configuración", "Could not save settings"),
                Toast.LENGTH_SHORT).show();
        return saved;
    }

    static native void updateRuntimeOption(String key, String value);

    void applyRuntimeOption(String key) {
        if ("NX_GAMEPAD_LAYOUT".equals(key) || "NX_GAMEPAD_MAP".equals(key)
                || "NX_SHOW_FPS".equals(key) || key.startsWith("NX_TOUCH_")
                || "NX_STICK_SENSITIVITY".equals(key) || "NX_CAMERA_SENSITIVITY".equals(key)) {
            String value = config.get(key);
            updateRuntimeOption(key, value == null ? "" : value);
        }
    }

    void showGamepadMapper(final int[] map) {
        final int[] selected = {0};
        final List<Button> rows = new ArrayList<>();
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout card = dialogCard();
        card.addView(text(t("ASIGNAR BOTONES DEL MANDO", "REMAP GAMEPAD BUTTONS"), YELLOW, 21, true));
        card.addView(dialogAccent());
        TextView description = text(t("Selecciona una acción y pulsa el botón que quieres asignar.",
                "Select an action, then press the button to assign."), MUTED, 14, false);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.bottomMargin = dp(8);
        card.addView(description, descriptionParams);

        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(false);
        listScroll.setVerticalScrollBarEnabled(true);
        listScroll.setScrollbarFadingEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        final List<Button> navigation = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            final int index = i;
            Button row = actionButton(mapActionLabel(index, map[index]), i == selected[0]);
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            row.setPadding(dp(16), 0, dp(12), 0);
            row.setTextSize(15);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selected[0] = index;
                    dialogCardFocus(rows, index);
                    captureGamepadButton(map, index, new Runnable() {
                        @Override public void run() {
                            rows.get(index).setText(mapActionLabel(index, map[index]));
                            dialogCardFocus(navigation, selected[0]);
                        }
                    });
                }
            });
            rows.add(row);
            navigation.add(row);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48));
            rowParams.bottomMargin = dp(5);
            list.addView(row, rowParams);
        }
        listScroll.addView(list);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int listHeight = Math.max(dp(100), Math.min(dp(420), screenHeight - dp(240)));
        card.addView(listScroll, new LinearLayout.LayoutParams(-1, listHeight));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(10), 0, 0);
        Button restore = actionButton(t("Restaurar preset", "Restore preset"), false);
        restore.setTextSize(14);
        Button close = actionButton(t("Cerrar", "Close"), false);
        close.setTextSize(14);
        LinearLayout.LayoutParams footerButton = new LinearLayout.LayoutParams(-2, dp(48));
        footerButton.rightMargin = dp(8);
        footer.addView(restore, footerButton);
        footer.addView(close, new LinearLayout.LayoutParams(-2, dp(48)));
        card.addView(footer);
        navigation.add(restore);
        navigation.add(close);

        restore.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Map<String, String> previous = new LinkedHashMap<>(config);
                config.remove(MAP_KEY);
                if (!persist()) {
                    config.clear();
                    config.putAll(previous);
                    return;
                }
                updateRuntimeOption(MAP_KEY, "");
                int[] defaults = readGamepadMap();
                System.arraycopy(defaults, 0, map, 0, map.length);
                for (int i = 0; i < rows.size(); i++) rows.get(i).setText(mapActionLabel(i, map[i]));
                dialogCardFocus(navigation, selected[0]);
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        dialog.setView(card);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface d, int key, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() != 0) return true;
                if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                        || key == KeyEvent.KEYCODE_BUTTON_L1 || key == KeyEvent.KEYCODE_BUTTON_R1) {
                    int direction = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_BUTTON_L1 ? -1 : 1;
                    selected[0] = (selected[0] + direction + navigation.size()) % navigation.size();
                    dialogCardFocus(navigation, selected[0]);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_BUTTON_A) {
                    navigation.get(selected[0]).performClick();
                    return true;
                }
                if (key == KeyEvent.KEYCODE_BUTTON_B) { dialog.cancel(); return true; }
                return false;
            }
        });
        dialog.show();
        styleDialogWindow(dialog, 600, 690);
        focusWhenReady(navigation.get(0));
    }

    String mapActionLabel(int action, int key) {
        return Lang.t(MAP_ACTIONS_ES[action], MAP_ACTIONS_EN[action]) + "   ·   " + gamepadKeyName(key);
    }

    LinearLayout dialogCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(20), dp(22), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(P5Theme.INK_BLACK);
        background.setCornerRadius(dp(1));
        background.setStroke(dp(2), RED);
        card.setBackground(background);
        return card;
    }

    View dialogAccent() {
        View accent = new View(this);
        accent.setBackgroundColor(RED);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(72), dp(4));
        params.topMargin = dp(9);
        params.bottomMargin = dp(12);
        accent.setLayoutParams(params);
        return accent;
    }

    void dialogCardFocus(List<Button> rows, int selected) {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setTag(Boolean.valueOf(i == selected));
            styleMenuButton(rows.get(i), i == selected || rows.get(i).hasFocus());
        }
        rows.get(selected).requestFocus();
    }

    void styleDialogWindow(AlertDialog dialog, int maxWidthDp, int maxHeightDp) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = Math.min(dp(maxWidthDp), getResources().getDisplayMetrics().widthPixels - dp(36));
        int height = Math.min(dp(maxHeightDp), getResources().getDisplayMetrics().heightPixels - dp(36));
        window.setLayout(width, height);
    }

    void captureGamepadButton(final int[] map, final int action, final Runnable onSaved) {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout card = dialogCard();
        card.addView(text(Lang.f("ASIGNAR %s", "ASSIGN %s", Lang.t(MAP_ACTIONS_ES[action], MAP_ACTIONS_EN[action])),
                YELLOW, 21, true));
        card.addView(dialogAccent());
        TextView prompt = text(t("Pulsa el botón que quieres asignar.\nAtrás cancela.",
                "Press the button to assign.\nBack cancels."), WHITE, 17, false);
        prompt.setPadding(0, dp(10), 0, dp(20));
        card.addView(prompt);
        Button cancel = actionButton(t("Cancelar", "Cancel"), false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.cancel(); }
        });
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(50));
        card.addView(cancel, cancelParams);
        dialog.setView(card);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface d, int key, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() != 0) return true;
                if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) { dialog.cancel(); return true; }
                if (!isGamepadKey(event, key)) return false;
                int[] previousMap = map.clone();
                Map<String, String> previousConfig = new LinkedHashMap<>(config);
                int captured = canonicalKey(key);
                int old = map[action];
                for (int i = 0; i < map.length; i++) if (i != action && map[i] == captured) map[i] = old;
                map[action] = captured;
                StringBuilder encoded = new StringBuilder();
                for (int i = 0; i < map.length; i++) {
                    if (i != 0) encoded.append(',');
                    encoded.append(map[i]);
                }
                config.put(MAP_KEY, encoded.toString());
                if (persist()) {
                    updateRuntimeOption(MAP_KEY, encoded.toString());
                    dialog.dismiss();
                    onSaved.run();
                }
                else {
                    System.arraycopy(previousMap, 0, map, 0, map.length);
                    config.clear();
                    config.putAll(previousConfig);
                }
                return true;
            }
        });
        dialog.show();
        styleDialogWindow(dialog, 500, 280);
        focusWhenReady(cancel);
    }

    boolean isGamepadKey(KeyEvent event, int key) {
        if (KeyEvent.isGamepadButton(key)) return true;
        int sources = event.getSource();
        InputDevice device = event.getDevice();
        if (device != null) sources |= device.getSources();
        return (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD;
    }

    int[] readGamepadMap() {
        String encoded = config.get(MAP_KEY);
        if (encoded != null) {
            String[] fields = encoded.split(",", -1);
            if (fields.length == MAP_ACTIONS_ES.length) try {
                int[] result = new int[fields.length];
                boolean valid = true;
                for (int i = 0; i < fields.length; i++) {
                    result[i] = canonicalKey(Integer.parseInt(fields[i]));
                    if (result[i] <= KeyEvent.KEYCODE_UNKNOWN) valid = false;
                    for (int j = 0; j < i; j++) if (result[i] == result[j]) valid = false;
                }
                if (valid) return result;
            } catch (NumberFormatException ignored) {}
        }
        boolean xbox = "1".equals(config.get("NX_GAMEPAD_LAYOUT"));
        return new int[] {xbox ? KeyEvent.KEYCODE_BUTTON_A : KeyEvent.KEYCODE_BUTTON_B,
                xbox ? KeyEvent.KEYCODE_BUTTON_B : KeyEvent.KEYCODE_BUTTON_A,
                xbox ? KeyEvent.KEYCODE_BUTTON_X : KeyEvent.KEYCODE_BUTTON_Y,
                xbox ? KeyEvent.KEYCODE_BUTTON_Y : KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2,
                KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN};
    }

    int canonicalKey(int key) {
        switch (key) {
            case KeyEvent.KEYCODE_BUTTON_1: return KeyEvent.KEYCODE_BUTTON_Y;
            case KeyEvent.KEYCODE_BUTTON_2: return KeyEvent.KEYCODE_BUTTON_B;
            case KeyEvent.KEYCODE_BUTTON_3: return KeyEvent.KEYCODE_BUTTON_A;
            case KeyEvent.KEYCODE_BUTTON_4: return KeyEvent.KEYCODE_BUTTON_X;
            case KeyEvent.KEYCODE_BUTTON_5: case KeyEvent.KEYCODE_BUTTON_C: return KeyEvent.KEYCODE_BUTTON_L2;
            case KeyEvent.KEYCODE_BUTTON_6: case KeyEvent.KEYCODE_BUTTON_Z: return KeyEvent.KEYCODE_BUTTON_R2;
            case KeyEvent.KEYCODE_BUTTON_7: return KeyEvent.KEYCODE_BUTTON_L1;
            case KeyEvent.KEYCODE_BUTTON_8: return KeyEvent.KEYCODE_BUTTON_R1;
            case KeyEvent.KEYCODE_BUTTON_9: return KeyEvent.KEYCODE_BUTTON_SELECT;
            case KeyEvent.KEYCODE_BUTTON_10: case KeyEvent.KEYCODE_BUTTON_MODE: return KeyEvent.KEYCODE_BUTTON_START;
            case KeyEvent.KEYCODE_BUTTON_11: return KeyEvent.KEYCODE_BUTTON_THUMBL;
            case KeyEvent.KEYCODE_BUTTON_12: return KeyEvent.KEYCODE_BUTTON_THUMBR;
            default: return key;
        }
    }

    String gamepadKeyName(int key) {
        return KeyEvent.keyCodeToString(key).replace("KEYCODE_BUTTON_", "")
                .replace("KEYCODE_DPAD_", "D-pad ").replace("KEYCODE_", "");
    }

    void returnToLauncher() {
        Intent i = new Intent(this, Launcher.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (handleChoiceDialogKey(event)) return true;
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BUTTON_L1 || key == KeyEvent.KEYCODE_BUTTON_7
                || key == KeyEvent.KEYCODE_BUTTON_R1 || key == KeyEvent.KEYCODE_BUTTON_8) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                int direction = key == KeyEvent.KEYCODE_BUTTON_L1 || key == KeyEvent.KEYCODE_BUTTON_7 ? -1 : 1;
                selectedCategory = (selectedCategory + direction + categories.length) % categories.length;
                refreshCategories();
                showCategory();
                scrollCategoryIntoView(categoryButtons.get(selectedCategory));
                focusFirstSetting();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0)
                moveVertical(key == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_BUTTON_A) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                View focus = getCurrentFocus();
                if (focus != null && focus.isEnabled() && focus.isClickable()) focus.performClick();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_BUTTON_B || key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() { finish(); }

    Button actionButton(String label, boolean selected) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(selected ? Color.BLACK : WHITE);
        b.setTextSize(16);
        b.setTypeface(P5Theme.condensedBold());
        b.setMinHeight(dp(48));
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setTag(Boolean.valueOf(selected));
        b.setBackground(menuButtonBackground(selected));
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean focused) {
                boolean active = Boolean.TRUE.equals(b.getTag());
                styleMenuButton(b, active || focused);
            }
        });
        // Keep D-pad focus for controllers, while making a touchscreen tap activate immediately.
        // Android's focus-in-touch-mode can otherwise spend the first tap only moving focus on some devices.
        b.setOnTouchListener(new View.OnTouchListener() {
            private boolean pressed;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        pressed = b.isEnabled();
                        if (pressed) {
                            b.requestFocus();
                            b.setPressed(true);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        boolean activate = pressed && b.isEnabled()
                                && event.getX() >= 0 && event.getX() < b.getWidth()
                                && event.getY() >= 0 && event.getY() < b.getHeight();
                        pressed = false;
                        b.setPressed(false);
                        if (activate) b.performClick();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        pressed = false;
                        b.setPressed(false);
                        return true;
                    default:
                        return true;
                }
            }
        });
        return b;
    }

    void styleMenuButton(Button button, boolean selected) {
        button.setTextColor(selected ? Color.BLACK : WHITE);
        button.setBackground(menuButtonBackground(selected));
    }

    android.graphics.drawable.Drawable menuButtonBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? P5Theme.PAPER_WHITE : P5Theme.INK_ELEVATED);
        background.setCornerRadius(dp(1));
        background.setStroke(dp(selected ? 2 : 1), selected ? P5Theme.P5_RED : P5Theme.BORDER_SUBTLE);
        return background;
    }

    void focusFirstSetting() {
        View target = firstOptionButton != null ? firstOptionButton : resumeButton;
        // Transfer focus immediately: posting this lets the previously focused header button
        // (often "Salir") remain highlighted for a frame after an L1/R1 category change.
        if (!target.requestFocus()) focusWhenReady(target);
    }

    void moveVertical(int direction) {
        if (verticalNavigation.isEmpty()) return;
        int current = verticalNavigation.indexOf(getCurrentFocus());
        int next = current < 0 ? (direction > 0 ? 0 : verticalNavigation.size() - 1)
                : (current + direction + verticalNavigation.size()) % verticalNavigation.size();
        final View target = verticalNavigation.get(next);
        target.requestFocus();
        if (optionScroll != null) target.post(new Runnable() {
            @Override public void run() {
                android.graphics.Rect rect = new android.graphics.Rect();
                target.getDrawingRect(rect);
                optionScroll.offsetDescendantRectToMyCoords(target, rect);
                int top = optionScroll.getScrollY();
                int bottom = top + optionScroll.getHeight();
                if (rect.top < top) optionScroll.smoothScrollTo(0, rect.top);
                else if (rect.bottom > bottom) optionScroll.smoothScrollTo(0, rect.bottom - optionScroll.getHeight());
            }
        });
    }

    void scrollCategoryIntoView(final View target) {
        if (categoryScroll == null) return;
        target.post(new Runnable() {
            @Override public void run() {
                android.graphics.Rect rect = new android.graphics.Rect();
                target.getDrawingRect(rect);
                categoryScroll.offsetDescendantRectToMyCoords(target, rect);
                int top = categoryScroll.getScrollY();
                int bottom = top + categoryScroll.getHeight();
                if (rect.top < top) categoryScroll.smoothScrollTo(0, rect.top);
                else if (rect.bottom > bottom) categoryScroll.smoothScrollTo(0, rect.bottom - categoryScroll.getHeight());
            }
        });
    }

    LinearLayout.LayoutParams buttonParams(int width, int height) {
        return new LinearLayout.LayoutParams(dp(width), dp(height));
    }

    TextView text(String value, int color, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setTypeface(bold ? P5Theme.condensedBold() : P5Theme.bodyRegular());
        return v;
    }

    void focusWhenReady(final View v) {
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.post(new Runnable() { @Override public void run() { v.requestFocus(); } });
    }
}
