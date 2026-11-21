package io.github.p5rrecomp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Direction A (Persona 5 Pause / Calling Card) Canvas-drawn UI primitives:
 * sharp hand-cut paper polygons, 0px-blur offset ink shadows, zero rounded corners.
 */
final class P5Views {

    // Synced with Launcher's NX_GAMEPAD_LAYOUT ("1" = Xbox/PC: A=Confirm, B=Back; "0" = Nintendo: B=Confirm, A=Back)
    private static boolean sXboxGamepadLayout = false;
    private static boolean sControllerMode = false;

    static void setXboxGamepadLayout(boolean xbox) {
        sXboxGamepadLayout = xbox;
    }

    static boolean isXboxGamepadLayout() {
        return sXboxGamepadLayout;
    }

    static void setControllerModeActive(boolean active) {
        sControllerMode = active;
    }

    static boolean isControllerModeActive() {
        return sControllerMode;
    }

    static boolean isConfirmKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) return true;
        return sXboxGamepadLayout ? (keyCode == KeyEvent.KEYCODE_BUTTON_A) : (keyCode == KeyEvent.KEYCODE_BUTTON_B);
    }

    static boolean isBackKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) return true;
        return sXboxGamepadLayout ? (keyCode == KeyEvent.KEYCODE_BUTTON_B) : (keyCode == KeyEvent.KEYCODE_BUTTON_A);
    }

    // ---- 1. Ransom-Cut / Calling-Card Masthead ("PERSONA 5" / "ROYAL") ----

    static final class CutTitleView extends View {
        private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path bgSlash = new Path();
        private final Path royalPoly = new Path();
        private static final float[] ANGLES = {-4.5f, 3.0f, -2.5f, 4.0f, -3.5f, 2.5f, -4.0f, 0f, 5.0f};

        CutTitleView(Context context) {
            super(context);
            textPaint.setTypeface(P5Theme.displayBold());
            textPaint.setTextAlign(Paint.Align.CENTER);
            borderPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            int h = P5Theme.dpx(getContext(), 58);
            setMeasuredDimension(w, h);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();

            // Red jagged backing slash behind the title
            bgSlash.reset();
            bgSlash.moveTo(2 * d, 12 * d);
            bgSlash.lineTo(w - 14 * d, 4 * d);
            bgSlash.lineTo(w - 22 * d, 36 * d);
            bgSlash.lineTo(0, 42 * d);
            bgSlash.close();
            boxPaint.setStyle(Paint.Style.FILL);
            boxPaint.setColor(P5Theme.P5_RED);
            c.drawPath(bgSlash, boxPaint);

            // Row 1: "PERSONA 5" in alternating cut-paper tiles
            String line1 = "PERSONA 5";
            float tileW = Math.min(22.5f * d, (w - 20 * d) / 9.2f);
            float tileH = 26f * d;
            float x = 6 * d;
            float y = 7 * d;
            textPaint.setTextSize(tileH * 0.72f);
            borderPaint.setStrokeWidth(1.8f * d);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textBaselineOff = -(fm.ascent + fm.descent) * 0.5f;

            for (int i = 0; i < line1.length(); i++) {
                char ch = line1.charAt(i);
                if (ch == ' ') {
                    x += tileW * 0.45f;
                    continue;
                }
                boolean isFive = (ch == '5');
                boolean whiteTile = (i % 2 == 0) && !isFive;
                float tw = isFive ? tileW * 1.18f : tileW;
                float th = isFive ? tileH * 1.15f : tileH;
                float ty = isFive ? y - 2 * d : y + ((i % 2 == 0) ? 0f : 2f * d);

                c.save();
                c.rotate(ANGLES[i % ANGLES.length], x + tw * 0.5f, ty + th * 0.5f);
                // Hard offset shadow
                boxPaint.setColor(P5Theme.INK_BLACK);
                c.drawRect(x + 2.5f * d, ty + 2.5f * d, x + tw + 2.5f * d, ty + th + 2.5f * d, boxPaint);
                // Tile fill
                boxPaint.setColor(isFive ? P5Theme.P5_RED : (whiteTile ? P5Theme.PAPER_WHITE : P5Theme.INK_BLACK));
                c.drawRect(x, ty, x + tw, ty + th, boxPaint);
                // Crisp border
                borderPaint.setColor(isFive ? P5Theme.PAPER_WHITE : (whiteTile ? P5Theme.INK_BLACK : P5Theme.PAPER_WHITE));
                c.drawRect(x, ty, x + tw, ty + th, borderPaint);
                // Letter
                textPaint.setColor(whiteTile ? P5Theme.INK_BLACK : P5Theme.PAPER_WHITE);
                c.drawText(String.valueOf(ch), x + tw * 0.5f, ty + th * 0.5f + textBaselineOff, textPaint);
                c.restore();
                x += tw + 1.2f * d;
            }

            // Row 2: Slanted "ROYAL" cut-paper ribbon optically centered underneath "PERSONA 5"
            float rw = 96 * d, rh = 18 * d;
            float wordStart = 6 * d;
            float wordEnd = x - 1.2f * d;
            float rx = (wordStart + wordEnd - rw) * 0.5f;
            float ry = 36 * d;
            P5Theme.setSlantedQuad(royalPoly, rx, ry, rx + rw, ry + rh, 5 * d, 5 * d);
            boxPaint.setColor(P5Theme.PAPER_WHITE);
            c.save();
            c.translate(2 * d, 2 * d);
            c.drawPath(royalPoly, boxPaint);
            c.restore();
            boxPaint.setColor(P5Theme.INK_BLACK);
            c.drawPath(royalPoly, boxPaint);

            textPaint.setTextSize(12.5f * d);
            textPaint.setColor(P5Theme.ACID_YELLOW);
            Paint.FontMetrics fm2 = textPaint.getFontMetrics();
            float b2 = -(fm2.ascent + fm2.descent) * 0.5f;
            c.drawText("R O Y A L", rx + rw * 0.5f, ry + rh * 0.5f + b2, textPaint);
        }
    }

    // ---- 2. Slanted Left-Rail Navigation Tab (P5 Pause Menu Style) ----

    static final class NavTabView extends View {
        private final String label;
        private final String subTag;
        private boolean active = false;
        private boolean pressed = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mainPoly = new Path();
        private final Path backSlash = new Path();
        private final Path tickPoly = new Path();

        NavTabView(Context context, String label, String subTag) {
            super(context);
            this.label = label;
            this.subTag = subTag;
            setClickable(true);
            setFocusable(false);
            textPaint.setTypeface(P5Theme.displayBold());
        }

        void setActive(boolean a, boolean animate) {
            if (this.active == a && animate) return;
            this.active = a;
            float targetX = a ? P5Theme.dp(getContext(), 9f) : 0f;
            if (animate) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(this, "translationX", targetX);
                anim.setDuration(150);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.start();
            } else {
                setTranslationX(targetX);
            }
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                pressed = true;
                invalidate();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed = false;
                invalidate();
            }
            return super.onTouchEvent(e);
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previouslyFocusedRect) {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
            invalidate();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            int h = P5Theme.dpx(getContext(), 40);
            setMeasuredDimension(w, h);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float skew = 11f * d;
            float padR = 14f * d;
            float padV = 3f * d;

            if (active) {
                // Jagged crimson backing slash behind active item
                P5Theme.setCallingCardQuad(backSlash, 2 * d, padV - 1 * d, w - padR + 5 * d, h - padV + 2 * d,
                        4 * d, -1 * d, 2 * d, 2 * d, -6 * d, 1 * d, -2 * d, -1 * d);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(P5Theme.P5_RED);
                c.drawPath(backSlash, paint);

                // Stark paper-white active polygon
                P5Theme.setSlantedQuad(mainPoly, 4 * d, padV, w - padR, h - padV, skew * 0.45f, skew);
                paint.setColor(pressed ? P5Theme.ACID_YELLOW : P5Theme.PAPER_WHITE);
                c.drawPath(mainPoly, paint);

                // Ink outline
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f * d);
                paint.setColor(P5Theme.INK_BLACK);
                c.drawPath(mainPoly, paint);
            } else if (isFocused()) {
                // Dark elevated slab with bold acid-yellow frame + white text (never white-on-white)
                P5Theme.setSlantedQuad(mainPoly, 4 * d, padV + 1 * d, w - padR - 4 * d, h - padV - 1 * d,
                        skew * 0.35f, skew * 0.85f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(225, 28, 26, 36));
                c.drawPath(mainPoly, paint);

                P5Theme.setSlantedQuad(tickPoly, 4 * d, padV + 1 * d, 11 * d, h - padV - 1 * d, 2 * d, 2 * d);
                paint.setColor(P5Theme.ACID_YELLOW);
                c.drawPath(tickPoly, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.2f * d);
                paint.setColor(P5Theme.ACID_YELLOW);
                c.drawPath(mainPoly, paint);
            } else {
                // Subtle dark cut slab when idle
                P5Theme.setSlantedQuad(mainPoly, 4 * d, padV + 1 * d, w - padR - 6 * d, h - padV - 1 * d,
                        skew * 0.35f, skew * 0.85f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(pressed ? P5Theme.P5_DEEP_RED : Color.argb(150, 22, 22, 28));
                c.drawPath(mainPoly, paint);

                // Sharp crimson left tick bar
                P5Theme.setSlantedQuad(tickPoly, 4 * d, padV + 1 * d, 10 * d, h - padV - 1 * d, 2 * d, 2 * d);
                paint.setColor(P5Theme.P5_RED);
                c.drawPath(tickPoly, paint);
            }

            // Tab label
            textPaint.setTextSize((active ? 17.5f : 15.5f) * d);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(active ? P5Theme.INK_BLACK : P5Theme.PAPER_WHITE);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = h * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            c.drawText(label, 19 * d, base, textPaint);

            // Optional compact right index tag ("01", "02"...)
            if (subTag != null && !subTag.isEmpty()) {
                textPaint.setTextSize(10f * d);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                textPaint.setColor(active ? P5Theme.P5_RED : P5Theme.MUTED_TEXT);
                c.drawText(subTag, w - padR - 14 * d, base, textPaint);
            }
        }
    }

    // ---- 3. Signature Cut-Paper Slash Action Button (PLAY / Secondary Actions) ----

    static final class SlashButton extends View {
        private String text;
        private final boolean primary; // true = massive crimson/white PLAY slab, false = compact action button
        private boolean warningStyle = false;
        private boolean primarySlashVisible = true;
        private int primaryLabelSizeDp = -1;
        private boolean pressed = false;
        private boolean controllerFocused = false;
        private boolean heroSheenEnabled = false;
        private float sheenPhase = 0f;
        private ValueAnimator sheenAnim;
        private int fixedHeightDp;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path poly = new Path();
        private final Path shadowPoly = new Path();
        private final Path accentPoly = new Path();
        private final Path sheenPoly = new Path();

        SlashButton(Context context, String text, boolean primary, int heightDp) {
            super(context);
            this.text = text;
            this.primary = primary;
            this.fixedHeightDp = heightDp;
            setClickable(true);
            setFocusable(false);
            textPaint.setTypeface(primary ? P5Theme.displayBold() : P5Theme.condensedBold());
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setText(String t) {
            this.text = t == null ? "" : t;
            invalidate();
        }

        void setPrimarySlashVisible(boolean visible) {
            primarySlashVisible = visible;
            invalidate();
        }

        void setPrimaryLabelSizeDp(int sizeDp) {
            primaryLabelSizeDp = sizeDp;
            invalidate();
        }

        void setWarningStyle(boolean warn) {
            if (this.warningStyle != warn) {
                this.warningStyle = warn;
                updateSheenAnim();
                invalidate();
            }
        }

        void setControllerFocused(boolean f) {
            if (this.controllerFocused == f) return;
            this.controllerFocused = f;
            animate().cancel();
            animate().scaleX(f ? 1.035f : 1f).scaleY(f ? 1.035f : 1f).setDuration(90).start();
            invalidate();
        }

        void enableHeroSheen(boolean enabled) {
            this.heroSheenEnabled = enabled;
            updateSheenAnim();
        }

        private void updateSheenAnim() {
            boolean want = heroSheenEnabled && primary && !warningStyle && isAttachedToWindow();
            if (want) {
                if (sheenAnim == null) {
                    sheenAnim = ValueAnimator.ofFloat(0f, 1f);
                    sheenAnim.setDuration(3400);
                    sheenAnim.setRepeatCount(ValueAnimator.INFINITE);
                    sheenAnim.setInterpolator(new LinearInterpolator());
                    sheenAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator a) {
                            sheenPhase = (Float) a.getAnimatedValue();
                            invalidate();
                        }
                    });
                }
                if (!sheenAnim.isStarted()) sheenAnim.start();
            } else {
                if (sheenAnim != null) sheenAnim.cancel();
                sheenPhase = 0f;
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            updateSheenAnim();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (sheenAnim != null) sheenAnim.cancel();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (!isEnabled()) return false;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                pressed = true;
                setScaleX(0.96f);
                setScaleY(0.96f);
                if (!heroSheenEnabled) P5Feedback.click(getContext());
                invalidate();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed = false;
                setScaleX(1f);
                setScaleY(1f);
                invalidate();
            }
            return super.onTouchEvent(e);
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previouslyFocusedRect) {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
            invalidate();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            float d = P5Theme.dp(getContext(), 1f);
            int h = Math.round(fixedHeightDp * d);
            int wMode = MeasureSpec.getMode(wSpec);
            int wSize = MeasureSpec.getSize(wSpec);
            int w;
            if (wMode == MeasureSpec.EXACTLY) {
                w = wSize;
            } else {
                textPaint.setTextSize((primary ? 20f : 13.5f) * d);
                float tw = textPaint.measureText(text) + (primary ? 64 * d : 34 * d);
                int minW = Math.round((primary ? 170 : 88) * d);
                w = Math.max(minW, Math.round(tw));
                if (wMode == MeasureSpec.AT_MOST) w = Math.min(w, wSize);
            }
            setMeasuredDimension(w, h);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float breath = (heroSheenEnabled && primary && !warningStyle && !pressed)
                    ? (1.1f * d * (float) Math.sin(sheenPhase * Math.PI * 2.0)) : 0f;
            float sh = pressed ? 1.5f * d : (primary ? (5.2f * d + breath) : 3.5f * d);
            float skew = (primary ? 14f : 8f) * d;
            float l = 2 * d, t = 2 * d, r = w - 6.5f * d - 2 * d, b = h - 6.5f * d - 2 * d;
            if (!primary) {
                r = w - sh - 2 * d;
                b = h - sh - 2 * d;
            }

            // Hard 0px-blur offset shadow
            P5Theme.setSlantedQuad(shadowPoly, l + sh, t + sh, r + sh, b + sh, skew, skew);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(primary ? P5Theme.INK_BLACK : P5Theme.INK_BLACK);
            c.drawPath(shadowPoly, paint);

            // Main fill
            P5Theme.setSlantedQuad(poly, l, t, r, b, skew, skew);
            int fillColor;
            int txtColor;
            int strokeColor;
            if (primary) {
                if (warningStyle) {
                    fillColor = pressed ? P5Theme.PAPER_WHITE : P5Theme.ACID_YELLOW;
                    txtColor = P5Theme.INK_BLACK;
                    strokeColor = P5Theme.INK_BLACK;
                } else {
                    fillColor = pressed ? P5Theme.PAPER_WHITE : P5Theme.P5_RED;
                    txtColor = pressed ? P5Theme.INK_BLACK : P5Theme.PAPER_WHITE;
                    strokeColor = P5Theme.PAPER_WHITE;
                }
            } else {
                fillColor = pressed ? P5Theme.P5_RED : P5Theme.PAPER_WHITE;
                txtColor = pressed ? P5Theme.PAPER_WHITE : P5Theme.INK_BLACK;
                strokeColor = P5Theme.INK_BLACK;
            }
            if (isFocused() || controllerFocused) strokeColor = P5Theme.ACID_YELLOW;

            paint.setColor(fillColor);
            c.drawPath(poly, paint);

            // Bold diagonal corner slash inside primary PLAY button
            if (primary && !warningStyle && primarySlashVisible) {
                accentPoly.reset();
                accentPoly.moveTo(l + skew + 6 * d, t);
                accentPoly.lineTo(l + skew + 16 * d, t);
                accentPoly.lineTo(l + 16 * d, b);
                accentPoly.lineTo(l + 6 * d, b);
                accentPoly.close();
                paint.setColor(pressed ? P5Theme.P5_RED : P5Theme.PAPER_WHITE);
                c.drawPath(accentPoly, paint);

                // Subtle periodic diagonal sheen sweep across hero PLAY button
                if (heroSheenEnabled && !pressed && sheenPhase < 0.28f) {
                    float u = sheenPhase / 0.28f;
                    float sx = l - 36f * d + (r - l + 72f * d) * u;
                    float sw = 24f * d;
                    c.save();
                    c.clipPath(poly);
                    sheenPoly.reset();
                    sheenPoly.moveTo(sx + skew, t);
                    sheenPoly.lineTo(sx + skew + sw, t);
                    sheenPoly.lineTo(sx + sw, b);
                    sheenPoly.lineTo(sx, b);
                    sheenPoly.close();
                    paint.setColor(Color.argb(62, 246, 245, 240));
                    c.drawPath(sheenPoly, paint);
                    c.restore();
                }
            }

            // Crisp border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(((isFocused() || controllerFocused) ? 3.2f : (primary ? 2.8f : 1.8f)) * d);
            paint.setColor(strokeColor);
            c.drawPath(poly, paint);

            // Label
            float primaryTextSize = primaryLabelSizeDp > 0
                    ? primaryLabelSizeDp : (fixedHeightDp >= 54 ? 21f : 16f);
            textPaint.setTextSize((primary ? primaryTextSize : 13f) * d);
            textPaint.setColor(txtColor);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = (t + b) * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            float cx = (l + r) * 0.5f + (primary && !warningStyle && primarySlashVisible ? 4 * d : 0f);
            c.drawText(text.toUpperCase(java.util.Locale.ROOT), cx, base, textPaint);
        }
    }

    // ---- 4. Direct Segmented Angled Control (Tap exact option directly) ----

    static final class SegmentedControl extends View {
        interface OnPickListener {
            void onPick(int index);
        }

        private String[] labels;
        private int selected = 0;
        private final boolean offOnToggle;
        private boolean controllerFocused = false;
        private OnPickListener listener;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path segPath = new Path();

        SegmentedControl(Context context, String[] labels, int selected, boolean offOnToggle, OnPickListener listener) {
            super(context);
            this.labels = labels;
            this.selected = selected;
            this.offOnToggle = offOnToggle;
            this.listener = listener;
            setClickable(true);
            setFocusable(false);
            textPaint.setTypeface(P5Theme.condensedBold());
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setLabelsAndPick(String[] labels, int pick) {
            this.labels = labels;
            this.selected = pick;
            invalidate();
        }

        void setControllerFocused(boolean f) {
            if (this.controllerFocused == f) return;
            this.controllerFocused = f;
            invalidate();
        }

        boolean step(int delta) {
            if (!isEnabled() || labels.length <= 1) return false;
            int next;
            if (labels.length == 2) {
                next = (selected + 1) % 2;
            } else {
                next = Math.max(0, Math.min(labels.length - 1, selected + delta));
                if (next == selected) return false;
            }
            selected = next;
            P5Feedback.click(getContext());
            invalidate();
            if (listener != null) listener.onPick(selected);
            return true;
        }

        void cycle() {
            if (!isEnabled() || labels.length <= 1) return;
            selected = (selected + 1) % labels.length;
            P5Feedback.click(getContext());
            invalidate();
            if (listener != null) listener.onPick(selected);
        }

        void setSelectedIndex(int idx) {
            this.selected = idx;
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (!isEnabled() || labels == null || labels.length == 0) return false;
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float w = getWidth();
                if (w > 0) {
                    int idx = (int) ((e.getX() / w) * labels.length);
                    idx = Math.max(0, Math.min(labels.length - 1, idx));
                    if (idx != selected) {
                        selected = idx;
                        P5Feedback.tick(getContext());
                        invalidate();
                        if (listener != null) listener.onPick(idx);
                    } else if (labels.length == 2) {
                        // Tapping the already-active side of a 2-option toggle flips it cleanly
                        selected = 1 - selected;
                        P5Feedback.tick(getContext());
                        invalidate();
                        if (listener != null) listener.onPick(selected);
                    }
                }
                performClick();
                return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            float d = P5Theme.dp(getContext(), 1f);
            int count = labels != null ? labels.length : 2;
            int minSegW = (count <= 2) ? 74 : (count == 3 ? 62 : 52);
            int desiredW = Math.round((count * minSegW + 14) * d);
            int wMode = MeasureSpec.getMode(wSpec);
            int wSize = MeasureSpec.getSize(wSpec);
            int w = (wMode == MeasureSpec.EXACTLY) ? wSize
                    : (wMode == MeasureSpec.AT_MOST ? Math.min(desiredW, wSize) : desiredW);
            setMeasuredDimension(w, Math.round(36 * d));
        }

        @Override
        protected void onDraw(Canvas c) {
            if (labels == null || labels.length == 0) return;
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float skew = 6f * d;
            float pad = 1.5f * d;
            float usableW = w - skew - pad * 2f;
            float segW = usableW / labels.length;

            // Outer dark ink frame
            P5Theme.setSlantedQuad(segPath, pad, pad, w - pad, h - pad, skew, skew);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(segPath, paint);

            // Draw inactive segments first, then the active segment LAST so its white border is never painted over
            for (int pass = 0; pass < 2; pass++) {
                boolean drawActive = (pass == 1);
                for (int i = 0; i < labels.length; i++) {
                    boolean act = (i == selected);
                    if (act != drawActive) continue;
                    float sl = pad + i * segW;
                    float sr = sl + segW + skew;
                    boolean actOff = act && offOnToggle && i == 0;
                    P5Theme.setSlantedQuad(segPath, sl, pad, sr, h - pad, skew, skew);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(actOff ? Color.rgb(46, 46, 56) : (act ? P5Theme.P5_RED : P5Theme.INK_ELEVATED));
                    c.drawPath(segPath, paint);

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth((act ? (controllerFocused ? 2.4f : 2.0f) : 1.3f) * d);
                    paint.setColor(act ? (controllerFocused ? P5Theme.ACID_YELLOW : P5Theme.PAPER_WHITE) : P5Theme.BORDER_SUBTLE);
                    c.drawPath(segPath, paint);

                    // Compact label
                    String txt = labels[i];
                    float ts = (labels.length >= 5 ? 11.5f : 12.5f) * d;
                    textPaint.setTextSize(ts);
                    textPaint.setColor(act ? P5Theme.PAPER_WHITE : P5Theme.MUTED_TEXT);
                    Paint.FontMetrics fm = textPaint.getFontMetrics();
                    float base = h * 0.5f - (fm.ascent + fm.descent) * 0.5f;
                    c.drawText(txt, sl + (segW + skew) * 0.5f, base, textPaint);
                }
            }
        }
    }

    // ---- 5. Compact Slanted Spec Tag (Hero Home Screen) ----

    static final class SpecBadge extends View {
        private String key;
        private String value;
        private boolean accent;
        private boolean pressed = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path poly = new Path();

        SpecBadge(Context context, String key, String value, boolean accent) {
            super(context);
            this.key = key;
            this.value = value;
            this.accent = accent;
            setClickable(true);
            setFocusable(false);
            textPaint.setTypeface(P5Theme.monoBold());
        }

        void setValue(String v, boolean accent) {
            this.value = v;
            this.accent = accent;
            requestLayout();
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (!isEnabled()) return false;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                pressed = true;
                setScaleX(0.95f);
                setScaleY(0.95f);
                invalidate();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed = false;
                setScaleX(1f);
                setScaleY(1f);
                invalidate();
            }
            return super.onTouchEvent(e);
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            float d = P5Theme.dp(getContext(), 1f);
            textPaint.setTextSize(11f * d);
            float kw = textPaint.measureText(key);
            float vw = textPaint.measureText(value);
            int w = Math.round(kw + vw + 28 * d);
            int h = Math.round(28 * d);
            setMeasuredDimension(w, h);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float skew = 6f * d;

            // Shadow
            P5Theme.setSlantedQuad(poly, 2.5f * d, 2.5f * d, w, h, skew, skew);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(poly, paint);

            // Body
            P5Theme.setSlantedQuad(poly, 0, 0, w - 2.5f * d, h - 2.5f * d, skew, skew);
            int bg = pressed ? P5Theme.P5_RED : (accent ? P5Theme.PAPER_WHITE : P5Theme.INK_BLACK);
            paint.setColor(bg);
            c.drawPath(poly, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * d);
            paint.setColor(pressed ? P5Theme.PAPER_WHITE : (accent ? P5Theme.INK_BLACK : P5Theme.PAPER_WHITE));
            c.drawPath(poly, paint);

            textPaint.setTextSize(10.5f * d);
            textPaint.setTextAlign(Paint.Align.LEFT);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = (h - 2.5f * d) * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            float x = 10 * d;
            textPaint.setColor(pressed ? P5Theme.ACID_YELLOW : (accent ? P5Theme.P5_RED : P5Theme.MUTED_TEXT));
            c.drawText(key, x, base, textPaint);
            x += textPaint.measureText(key) + 5 * d;
            textPaint.setColor(pressed ? P5Theme.PAPER_WHITE : (accent ? P5Theme.INK_BLACK : P5Theme.PAPER_WHITE));
            c.drawText(value, x, base, textPaint);
        }
    }

    // ---- 6b. Compact P5 Category Group Sub-Header ----

    static final class GroupSubHeaderView extends View {
        private final String label;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path tickPoly = new Path();

        GroupSubHeaderView(Context context, String label) {
            super(context);
            this.label = label == null ? "" : label.toUpperCase(java.util.Locale.ROOT);
            textPaint.setTypeface(P5Theme.monoBold());
            textPaint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(MeasureSpec.getSize(wSpec), P5Theme.dpx(getContext(), 22));
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float cy = h * 0.56f;

            // Slanted crimson tick mark
            P5Theme.setSlantedQuad(tickPoly, 2f * d, cy - 5f * d, 8.5f * d, cy + 5f * d, 2.5f * d, 2.5f * d);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.P5_RED);
            c.drawPath(tickPoly, paint);

            // Uppercase group label
            textPaint.setTextSize(10.5f * d);
            textPaint.setColor(Color.argb(215, 246, 245, 240));
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = cy - (fm.ascent + fm.descent) * 0.5f;
            float tx = 14f * d;
            c.drawText(label, tx, base, textPaint);

            // Subtle hairline rule extending to the right
            float ruleStart = tx + textPaint.measureText(label) + 8f * d;
            if (ruleStart < w - 8f * d) {
                paint.setColor(Color.argb(72, 246, 245, 240));
                c.drawRect(ruleStart, cy - 0.6f * d, w - 4f * d, cy + 0.6f * d, paint);
            }
        }
    }

    // ---- 6c. Slanted Inline Sub-Tab Bar (Placed beside SectionHeaderView, LT/RT navigable) ----

    static final class SubTabBar extends View {
        interface OnSubTabListener {
            void onPick(int index);
        }

        private final String[] labels;
        private int selected = 0;
        private boolean showTriggers = false;
        private final OnSubTabListener listener;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint trigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path poly = new Path();
        private float[] pillLefts;
        private float[] pillRights;

        SubTabBar(Context context, String[] labels, int selected, OnSubTabListener listener) {
            super(context);
            this.labels = labels;
            this.selected = selected;
            this.listener = listener;
            this.pillLefts = new float[labels.length];
            this.pillRights = new float[labels.length];
            setClickable(true);
            setFocusable(false);
            textPaint.setTypeface(P5Theme.condensedBold());
            textPaint.setTextAlign(Paint.Align.CENTER);
            trigPaint.setTypeface(P5Theme.condensedBold());
            trigPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setSelected(int idx) {
            if (idx >= 0 && idx < labels.length && idx != selected) {
                selected = idx;
                invalidate();
            }
        }

        void setShowTriggers(boolean show) {
            if (this.showTriggers != show) {
                this.showTriggers = show;
                requestLayout();
                invalidate();
            }
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            float d = P5Theme.dp(getContext(), 1f);
            textPaint.setTextSize(11.5f * d);
            float total = 4 * d;
            if (showTriggers) total += 26 * d * 2f + 8 * d;
            for (int i = 0; i < labels.length; i++) {
                float pw = Math.max(54 * d, textPaint.measureText(labels[i]) + 22 * d);
                total += pw + (i > 0 ? 4 * d : 0f);
            }
            int w = Math.round(total + 6 * d);
            int h = P5Theme.dpx(getContext(), 32);
            setMeasuredDimension(resolveSize(w, wSpec), h);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                float tx = e.getX();
                for (int i = 0; i < labels.length; i++) {
                    if (tx >= pillLefts[i] && tx <= pillRights[i]) {
                        if (i != selected) {
                            selected = i;
                            P5Feedback.tab(getContext());
                            invalidate();
                            if (listener != null) listener.onPick(i);
                        }
                        return true;
                    }
                }
            }
            return super.onTouchEvent(e);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float skew = 6f * d;
            float top = 3 * d, bot = h - 3 * d;

            textPaint.setTextSize(11.5f * d);
            trigPaint.setTextSize(9.5f * d);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = (top + bot) * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            Paint.FontMetrics tfm = trigPaint.getFontMetrics();
            float tBase = (top + bot) * 0.5f - (tfm.ascent + tfm.descent) * 0.5f;

            float x = 2 * d;
            if (showTriggers) {
                float tw = 22 * d;
                P5Theme.setSlantedQuad(poly, x, top + 3 * d, x + tw, bot - 3 * d, 3.5f * d, 3.5f * d);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(selected > 0 ? P5Theme.ACID_YELLOW : Color.argb(90, 246, 245, 240));
                c.drawPath(poly, paint);
                trigPaint.setColor(P5Theme.INK_BLACK);
                c.drawText("LT", x + tw * 0.5f, tBase, trigPaint);
                x += tw + 5 * d;
            }

            for (int i = 0; i < labels.length; i++) {
                boolean act = (i == selected);
                float pw = Math.max(54 * d, textPaint.measureText(labels[i]) + 22 * d);
                pillLefts[i] = x;
                pillRights[i] = x + pw;

                P5Theme.setSlantedQuad(poly, x, top, x + pw, bot, skew, skew);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(act ? P5Theme.PAPER_WHITE : Color.argb(185, 20, 20, 26));
                c.drawPath(poly, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth((act ? 2.0f : 1.2f) * d);
                paint.setColor(act ? P5Theme.P5_RED : P5Theme.BORDER_SUBTLE);
                c.drawPath(poly, paint);

                textPaint.setColor(act ? P5Theme.INK_BLACK : Color.argb(210, 246, 245, 240));
                c.drawText(labels[i], x + pw * 0.5f, base, textPaint);
                x += pw + 4 * d;
            }

            if (showTriggers) {
                x += 1 * d;
                float tw = 22 * d;
                P5Theme.setSlantedQuad(poly, x, top + 3 * d, x + tw, bot - 3 * d, 3.5f * d, 3.5f * d);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(selected < labels.length - 1 ? P5Theme.ACID_YELLOW : Color.argb(90, 246, 245, 240));
                c.drawPath(poly, paint);
                trigPaint.setColor(P5Theme.INK_BLACK);
                c.drawText("RT", x + tw * 0.5f, tBase, trigPaint);
            }
        }
    }

    // ---- 6. Angled Section Header Banner ----

    static final class SectionHeaderView extends View {
        private String title;
        private String subtitle;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path poly = new Path();

        SectionHeaderView(Context context, String title, String subtitle) {
            super(context);
            this.title = title;
            this.subtitle = subtitle;
        }

        void setHeader(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
            invalidate();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(MeasureSpec.getSize(wSpec), P5Theme.dpx(getContext(), 44));
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();

            // Red accent bar on left
            P5Theme.setSlantedQuad(poly, 0, 5 * d, 10 * d, h - 7 * d, 4 * d, 4 * d);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.P5_RED);
            c.drawPath(poly, paint);

            // Subtitle on right (measure first so long titles never collide with it)
            float subReserve = 0f;
            if (subtitle != null && !subtitle.isEmpty()) {
                textPaint.setTypeface(P5Theme.monoBold());
                textPaint.setTextSize(10.5f * d);
                subReserve = textPaint.measureText(subtitle) + 14 * d;
            }

            // Title text in bold display italic (auto-fit if long)
            String upper = title.toUpperCase(java.util.Locale.ROOT);
            textPaint.setTypeface(P5Theme.displayBold());
            float titleSize = 21f * d;
            textPaint.setTextSize(titleSize);
            float availW = Math.max(40 * d, w - 22 * d - subReserve);
            float measured = textPaint.measureText(upper);
            if (measured > availW) {
                titleSize = Math.max(13f * d, titleSize * (availW / measured));
                textPaint.setTextSize(titleSize);
            }
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(P5Theme.PAPER_WHITE);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = (h - 4 * d) * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            c.drawText(upper, 17 * d, base, textPaint);

            // Subtitle on right
            if (subtitle != null && !subtitle.isEmpty()) {
                textPaint.setTypeface(P5Theme.monoBold());
                textPaint.setTextSize(10.5f * d);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                textPaint.setColor(P5Theme.ACID_YELLOW);
                Paint.FontMetrics fmSub = textPaint.getFontMetrics();
                float subBase = (h - 4 * d) * 0.5f - (fmSub.ascent + fmSub.descent) * 0.5f;
                c.drawText(subtitle, w - 6 * d, subBase, textPaint);
            }

            // Sharp bottom cut rule
            paint.setColor(P5Theme.PAPER_WHITE);
            c.drawRect(0, h - 2.5f * d, w, h - 1f * d, paint);
        }
    }

    // ---- 7. Angled Ink Container Row Background ----

    static final class AngledRowLayout extends LinearLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path poly = new Path();
        private final Path notch = new Path();
        private final Path ptr = new Path();
        private boolean highlight = false;
        private boolean controllerFocused = false;

        AngledRowLayout(Context context) {
            super(context);
            setWillNotDraw(false);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            int hPad = P5Theme.dpx(context, 14);
            int vPad = P5Theme.dpx(context, 9);
            setPadding(hPad, vPad, hPad, vPad);
        }

        void setHighlight(boolean h) {
            this.highlight = h;
            invalidate();
        }

        void setControllerFocused(boolean f) {
            if (this.controllerFocused == f) return;
            this.controllerFocused = f;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float skew = 7f * d;

            // Hard dark slab
            P5Theme.setSlantedQuad(poly, 0, 0, w - 3 * d, h - 2 * d, skew * 0.5f, skew);
            paint.setStyle(Paint.Style.FILL);
            if (controllerFocused) {
                paint.setColor(Color.argb(252, 38, 22, 28));
            } else {
                paint.setColor(highlight ? Color.argb(245, 28, 12, 14) : P5Theme.INK_SURFACE);
            }
            c.drawPath(poly, paint);

            // Crisp border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((controllerFocused ? 2.4f : 1.5f) * d);
            paint.setColor(controllerFocused ? P5Theme.ACID_YELLOW : (highlight ? P5Theme.P5_RED : P5Theme.BORDER_SUBTLE));
            c.drawPath(poly, paint);

            // Left notch bar + P5 focus pointer
            paint.setStyle(Paint.Style.FILL);
            paint.setColor((controllerFocused || highlight) ? P5Theme.ACID_YELLOW : P5Theme.P5_RED);
            P5Theme.setSlantedQuad(notch, 0, 0, (controllerFocused ? 7f : 5f) * d, h - 2 * d, skew * 0.5f, skew * 0.5f);
            c.drawPath(notch, paint);

            if (controllerFocused) {
                float cy = (h - 2 * d) * 0.5f;
                ptr.reset();
                ptr.moveTo(6.5f * d, cy - 5.5f * d);
                ptr.lineTo(13.5f * d, cy);
                ptr.lineTo(6.5f * d, cy + 5.5f * d);
                ptr.close();
                paint.setColor(P5Theme.ACID_YELLOW);
                c.drawPath(ptr, paint);
            }
        }
    }

    // ---- 7b. Adaptive P5 Controller Hint Bar (Auto-shown on gamepad input, hidden on touch) ----

    static final class ControllerHintBar extends View {
        private String[] leftKeys = new String[0];
        private String[] leftLabels = new String[0];
        private String[] rightKeys = new String[0];
        private String[] rightLabels = new String[0];
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lblPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path bgPoly = new Path();
        private final Path capPoly = new Path();

        ControllerHintBar(Context context) {
            super(context);
            keyPaint.setTypeface(P5Theme.condensedBold());
            keyPaint.setTextAlign(Paint.Align.CENTER);
            lblPaint.setTypeface(P5Theme.condensedBold());
            lblPaint.setTextAlign(Paint.Align.LEFT);
            setClickable(false);
            setFocusable(false);
        }

        void setSplitHints(String[] lk, String[] ll, String[] rk, String[] rl) {
            this.leftKeys = lk != null ? lk : new String[0];
            this.leftLabels = ll != null ? ll : new String[0];
            this.rightKeys = rk != null ? rk : new String[0];
            this.rightLabels = rl != null ? rl : new String[0];
            invalidate();
        }

        private float measureGroup(String[] ks, String[] ls, float d, float capPad) {
            float total = 0f;
            int n = Math.min(ks.length, ls.length);
            for (int i = 0; i < n; i++) {
                float kw = Math.max(18 * d, keyPaint.measureText(ks[i]) + capPad * 2f);
                float lw = lblPaint.measureText(ls[i]);
                total += kw + 5 * d + lw + (i + 1 < n ? 14 * d : 0f);
            }
            return total;
        }

        private void drawGroup(Canvas c, String[] ks, String[] ls, float startX, float maxX,
                               float d, float h, float capPad, float capV, float kBase, float lBase) {
            float x = startX;
            int n = Math.min(ks.length, ls.length);
            for (int i = 0; i < n; i++) {
                float kw = Math.max(18 * d, keyPaint.measureText(ks[i]) + capPad * 2f);
                float lw = lblPaint.measureText(ls[i]);
                if (x + kw + 5 * d + lw > maxX) break;

                P5Theme.setSlantedQuad(capPoly, x, capV, x + kw, h - capV, 3.5f * d, 3.5f * d);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(P5Theme.ACID_YELLOW);
                c.drawPath(capPoly, paint);

                keyPaint.setColor(P5Theme.INK_BLACK);
                c.drawText(ks[i], x + kw * 0.5f, kBase, keyPaint);

                lblPaint.setColor(P5Theme.PAPER_WHITE);
                c.drawText(ls[i], x + kw + 5 * d, lBase, lblPaint);

                x += kw + 5 * d + lw + 14 * d;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0 || (leftKeys.length == 0 && rightKeys.length == 0)) return;

            // Slanted ink strip
            P5Theme.setSlantedQuad(bgPoly, 0, 1 * d, w - 2 * d, h - 1 * d, 8 * d, 8 * d);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(230, 14, 14, 18));
            c.drawPath(bgPoly, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f * d);
            paint.setColor(Color.argb(140, 230, 0, 18));
            c.drawPath(bgPoly, paint);

            keyPaint.setTextSize(10.5f * d);
            lblPaint.setTextSize(10.5f * d);
            Paint.FontMetrics kfm = keyPaint.getFontMetrics();
            Paint.FontMetrics lfm = lblPaint.getFontMetrics();
            float kBase = h * 0.5f - (kfm.ascent + kfm.descent) * 0.5f;
            float lBase = h * 0.5f - (lfm.ascent + lfm.descent) * 0.5f;

            float capPad = 5.5f * d;
            float capV = 4.5f * d;

            // Right-aligned fixed global hints (never shift when moving Up/Down)
            float rightW = measureGroup(rightKeys, rightLabels, d, capPad);
            float rightStart = Math.max(14 * d, w - 16 * d - rightW);
            drawGroup(c, rightKeys, rightLabels, rightStart, w - 8 * d, d, h, capPad, capV, kBase, lBase);

            // Left-aligned contextual row action (confined safely left of rightStart)
            drawGroup(c, leftKeys, leftLabels, 14 * d, rightStart - 10 * d, d, h, capPad, capV, kBase, lBase);
        }
    }

    // ---- 8. Slanted Cut-Paper Progress Bar (for ImportActivity) ----

    static final class SlashProgressBar extends View {
        private int max = 1000;
        private int progress = 0;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path trackPath = new Path();
        private final Path fillPath = new Path();
        private final Path edgePath = new Path();
        private final Path badgePath = new Path();

        SlashProgressBar(Context context) {
            super(context);
            textPaint.setTypeface(P5Theme.monoBold());
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setMax(int max) {
            this.max = Math.max(1, max);
            invalidate();
        }

        void setProgress(int progress) {
            this.progress = Math.max(0, Math.min(max, progress));
            invalidate();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            int h = P5Theme.dpx(getContext(), 34);
            setMeasuredDimension(w, h);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float skew = 9f * d;
            float sh = 3f * d;
            float badgeW = 64f * d;
            float gap = 8f * d;
            float trackL = 2f * d;
            float trackT = 2f * d;
            float trackR = Math.max(trackL + 40f * d, w - badgeW - gap - sh);
            float trackB = h - sh - 2f * d;

            // Track hard shadow
            P5Theme.setSlantedQuad(trackPath, trackL + sh, trackT + sh, trackR + sh, trackB + sh, skew, skew);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(trackPath, paint);

            // Track base fill
            P5Theme.setSlantedQuad(trackPath, trackL, trackT, trackR, trackB, skew, skew);
            paint.setColor(P5Theme.INK_ELEVATED);
            c.drawPath(trackPath, paint);

            // Crimson progress fill clipped cleanly inside the slanted track
            float frac = Math.max(0f, Math.min(1f, (float) progress / max));
            if (frac > 0.001f) {
                float usableW = (trackR - trackL) - skew;
                float fillRight = trackL + skew + usableW * frac;
                c.save();
                c.clipPath(trackPath);
                P5Theme.setSlantedQuad(fillPath, trackL - skew, trackT, fillRight, trackB, skew, skew);
                paint.setColor(P5Theme.P5_RED);
                c.drawPath(fillPath, paint);

                // Sharp white diagonal leading slash edge while in progress
                if (frac < 0.995f) {
                    edgePath.reset();
                    edgePath.moveTo(fillRight - 4f * d, trackT);
                    edgePath.lineTo(fillRight, trackT);
                    edgePath.lineTo(fillRight - skew, trackB);
                    edgePath.lineTo(fillRight - skew - 4f * d, trackB);
                    edgePath.close();
                    paint.setColor(P5Theme.PAPER_WHITE);
                    c.drawPath(edgePath, paint);
                }
                c.restore();
            }

            // Crisp track outline
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f * d);
            paint.setColor(P5Theme.PAPER_WHITE);
            c.drawPath(trackPath, paint);

            // Right-side slanted percentage readout badge
            float bL = trackR + gap - skew * 0.35f;
            float bR = w - sh - 2f * d;
            P5Theme.setSlantedQuad(badgePath, bL + sh, trackT + sh, bR + sh, trackB + sh, skew * 0.7f, skew * 0.7f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(badgePath, paint);

            P5Theme.setSlantedQuad(badgePath, bL, trackT, bR, trackB, skew * 0.7f, skew * 0.7f);
            paint.setColor(frac >= 0.999f ? P5Theme.PAPER_WHITE : P5Theme.INK_BLACK);
            c.drawPath(badgePath, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f * d);
            paint.setColor(frac >= 0.999f ? P5Theme.P5_RED : P5Theme.PAPER_WHITE);
            c.drawPath(badgePath, paint);

            int pct = Math.round(frac * 100f);
            textPaint.setTextSize(13f * d);
            textPaint.setColor(frac >= 0.999f ? P5Theme.INK_BLACK : P5Theme.ACID_YELLOW);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float base = (trackT + trackB) * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            c.drawText(pct + "%", (bL + bR) * 0.5f, base, textPaint);
        }
    }

    // ---- 9. Calling-Card Skewed Dialog Frame (Hard offset crimson shadow + cut paper polygon) ----

    static final class DialogFrameLayout extends LinearLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shadowPoly = new Path();
        private final Path bodyPoly = new Path();
        private final Path accentBar = new Path();

        DialogFrameLayout(Context context) {
            super(context);
            setWillNotDraw(false);
            setOrientation(VERTICAL);
            int padL = P5Theme.dpx(context, 22);
            int padT = P5Theme.dpx(context, 20);
            int padR = P5Theme.dpx(context, 26);
            int padB = P5Theme.dpx(context, 24);
            setPadding(padL, padT, padR, padB);
        }

        void playPopIn() {
            setScaleX(0.92f);
            setScaleY(0.92f);
            setAlpha(0f);
            animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(150)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = P5Theme.dp(getContext(), 1f);
            float w = getWidth(), h = getHeight();
            float sh = 6f * d;
            float l = 3f * d, t = 3f * d, r = w - sh - 2f * d, b = h - sh - 2f * d;

            // Hard 0px-blur offset crimson shadow
            P5Theme.setCallingCardQuad(shadowPoly, l + sh, t + sh, r + sh, b + sh,
                    5f * d, 1.5f * d, -2f * d, -1f * d, -5f * d, 0f, 1.5f * d, -1.5f * d);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.P5_RED);
            c.drawPath(shadowPoly, paint);

            // Cut-paper INK_BLACK body
            P5Theme.setCallingCardQuad(bodyPoly, l, t, r, b,
                    5f * d, 1.5f * d, -2f * d, -1f * d, -5f * d, 0f, 1.5f * d, -1.5f * d);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(bodyPoly, paint);

            // Top crimson accent strip clipped to the card body
            c.save();
            c.clipPath(bodyPoly);
            accentBar.reset();
            accentBar.moveTo(l, t);
            accentBar.lineTo(r, t - 2f * d);
            accentBar.lineTo(r, t + 5f * d);
            accentBar.lineTo(l, t + 7f * d);
            accentBar.close();
            paint.setColor(P5Theme.P5_RED);
            c.drawPath(accentBar, paint);
            c.restore();

            // Crisp paper-white outline
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.4f * d);
            paint.setColor(P5Theme.PAPER_WHITE);
            c.drawPath(bodyPoly, paint);
        }
    }

    // ---- 10. Custom P5 Modal Picker Dialog (Replaces stock Android AlertDialog for lists) ----

    interface OnModalPick {
        void onPick(int index);
    }

    private static void styleDialogWindow(Dialog dlg) {
        Window win = dlg.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = win.getAttributes();
            lp.dimAmount = 0.72f;
            win.setAttributes(lp);
        }
    }

    interface OnGamepadKeyCapture { void onCapture(int keyCode); }

    static void showGamepadCaptureDialog(final Context ctx, String title, String message,
                                         final OnGamepadKeyCapture callback) {
        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        styleDialogWindow(dlg);
        DialogFrameLayout frame = new DialogFrameLayout(ctx);
        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
        int contentW = Math.min(P5Theme.dpx(ctx, 430), screenW - P5Theme.dpx(ctx, 96));
        frame.addView(new SectionHeaderView(ctx, title, ""),
                new LinearLayout.LayoutParams(contentW, P5Theme.dpx(ctx, 42)));

        TextView prompt = new TextView(ctx);
        prompt.setText(message);
        prompt.setTextColor(P5Theme.PAPER_WHITE);
        prompt.setTypeface(P5Theme.bodyRegular());
        prompt.setTextSize(14);
        prompt.setLineSpacing(P5Theme.dp(ctx, 3), 1f);
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(P5Theme.dpx(ctx, 8), P5Theme.dpx(ctx, 24), P5Theme.dpx(ctx, 8), P5Theme.dpx(ctx, 24));
        prompt.setFocusableInTouchMode(true);
        frame.addView(prompt, new LinearLayout.LayoutParams(contentW, LinearLayout.LayoutParams.WRAP_CONTENT));

        SlashButton cancel = new SlashButton(ctx, "CANCELAR / BACK", false, 40);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { P5Feedback.cancel(ctx); dlg.dismiss(); }
        });
        LinearLayout actions = new LinearLayout(ctx);
        actions.setGravity(Gravity.END);
        actions.addView(cancel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, P5Theme.dpx(ctx, 40)));
        frame.addView(actions, new LinearLayout.LayoutParams(contentW, LinearLayout.LayoutParams.WRAP_CONTENT));

        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() != 0) return true;
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    P5Feedback.cancel(ctx); dlg.dismiss(); return true;
                }
                InputDevice device = event.getDevice();
                int sources = event.getSource() | (device == null ? 0 : device.getSources());
                boolean gamepad = KeyEvent.isGamepadButton(keyCode)
                        || (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                        || (sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD;
                if (!gamepad) return false;
                dlg.dismiss();
                if (callback != null) callback.onCapture(keyCode);
                return true;
            }
        });
        prompt.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override public boolean onGenericMotion(View view, MotionEvent event) {
                if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) return false;
                int code = 0;
                if (event.getAxisValue(MotionEvent.AXIS_HAT_X) < -0.5f) code = KeyEvent.KEYCODE_DPAD_LEFT;
                else if (event.getAxisValue(MotionEvent.AXIS_HAT_X) > 0.5f) code = KeyEvent.KEYCODE_DPAD_RIGHT;
                else if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) < -0.5f) code = KeyEvent.KEYCODE_DPAD_UP;
                else if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) > 0.5f) code = KeyEvent.KEYCODE_DPAD_DOWN;
                else if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0.5f || event.getAxisValue(MotionEvent.AXIS_BRAKE) > 0.5f)
                    code = KeyEvent.KEYCODE_BUTTON_L2;
                else if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0.5f || event.getAxisValue(MotionEvent.AXIS_GAS) > 0.5f)
                    code = KeyEvent.KEYCODE_BUTTON_R2;
                if (code == 0) return false;
                dlg.dismiss();
                if (callback != null) callback.onCapture(code);
                return true;
            }
        });
        dlg.setContentView(frame);
        dlg.show();
        frame.playPopIn();
        prompt.requestFocus();
    }

    static void showPickerDialog(final Context ctx, String title, String[] labels, final int current, final OnModalPick cb) {
        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        styleDialogWindow(dlg);
        DialogFrameLayout frame = new DialogFrameLayout(ctx);

        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
        int screenH = ctx.getResources().getDisplayMetrics().heightPixels;
        int contentW = Math.min(P5Theme.dpx(ctx, 390), screenW - P5Theme.dpx(ctx, 96));
        int maxListH = Math.min(Math.round(screenH * 0.54f), P5Theme.dpx(ctx, 240));

        SectionHeaderView hdr = new SectionHeaderView(ctx, title, "");
        frame.addView(hdr, new LinearLayout.LayoutParams(contentW, P5Theme.dpx(ctx, 42)));

        final ScrollView sc = new ScrollView(ctx);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sc.setVerticalScrollBarEnabled(false);
        sc.setVerticalFadingEdgeEnabled(true);
        sc.setFadingEdgeLength(P5Theme.dpx(ctx, 14));
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, P5Theme.dpx(ctx, 8), 0, P5Theme.dpx(ctx, 8));

        final List<SlashButton> buttons = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            SlashButton btn = new SlashButton(ctx, labels[i], i == current, 42);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, P5Theme.dpx(ctx, 42));
            lp.bottomMargin = P5Theme.dpx(ctx, 6);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dlg.dismiss();
                    cb.onPick(idx);
                }
            });
            buttons.add(btn);
            list.addView(btn, lp);
        }
        sc.addView(list);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                contentW, Math.min(maxListH, labels.length * P5Theme.dpx(ctx, 48) + P5Theme.dpx(ctx, 14)));
        frame.addView(sc, scLp);

        final int[] focused = {Math.max(0, Math.min(labels.length - 1, current))};
        final boolean[] focusVisible = {sControllerMode};
        if (sControllerMode && !buttons.isEmpty()) {
            buttons.get(focused[0]).setControllerFocused(true);
        }
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (buttons.isEmpty()) return true;
                    if (!focusVisible[0]) {
                        focusVisible[0] = true;
                    } else {
                        buttons.get(focused[0]).setControllerFocused(false);
                        int dir = (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) ? 1 : -1;
                        focused[0] = (focused[0] + dir + buttons.size()) % buttons.size();
                    }
                    buttons.get(focused[0]).setControllerFocused(true);
                    P5Feedback.tick(ctx);
                    int rowH = P5Theme.dpx(ctx, 48);
                    int targetY = Math.max(0, focused[0] * rowH - sc.getHeight() / 2 + rowH / 2);
                    sc.smoothScrollTo(0, targetY);
                    return true;
                }
                if (isConfirmKey(keyCode)) {
                    if (!buttons.isEmpty()) {
                        P5Feedback.confirm(ctx);
                        dlg.dismiss();
                        cb.onPick(focused[0]);
                    }
                    return true;
                }
                if (isBackKey(keyCode)) {
                    P5Feedback.cancel(ctx);
                    dlg.dismiss();
                    return true;
                }
                return false;
            }
        });

        dlg.setContentView(frame);
        dlg.show();
        frame.playPopIn();

        if (current > 0) {
            sc.post(new Runnable() {
                @Override
                public void run() {
                    int rowH = P5Theme.dpx(ctx, 48);
                    int targetY = Math.max(0, current * rowH - sc.getHeight() / 2 + rowH / 2);
                    sc.scrollTo(0, targetY);
                }
            });
        }
    }

    // ---- 11. Custom P5 Styled Info / Confirm Dialog ----

    static void showMessageDialog(Context ctx, String title, String message,
                                  String posLabel, final Runnable onPos,
                                  String neutralLabel, final Runnable onNeutral,
                                  String negLabel) {
        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        styleDialogWindow(dlg);
        DialogFrameLayout frame = new DialogFrameLayout(ctx);

        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
        int contentW = Math.min(P5Theme.dpx(ctx, 430), screenW - P5Theme.dpx(ctx, 96));

        SectionHeaderView hdr = new SectionHeaderView(ctx, title, "");
        frame.addView(hdr, new LinearLayout.LayoutParams(contentW, P5Theme.dpx(ctx, 42)));

        TextView msg = new TextView(ctx);
        msg.setText(message);
        msg.setTextColor(P5Theme.PAPER_WHITE);
        msg.setTypeface(P5Theme.bodyRegular());
        msg.setTextSize(14);
        msg.setLineSpacing(P5Theme.dp(ctx, 3), 1f);
        msg.setPadding(P5Theme.dpx(ctx, 4), P5Theme.dpx(ctx, 12), P5Theme.dpx(ctx, 4), P5Theme.dpx(ctx, 16));
        frame.addView(msg, new LinearLayout.LayoutParams(contentW, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = new LinearLayout(ctx);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        final List<SlashButton> actionBtns = new ArrayList<>();
        if (negLabel != null) {
            SlashButton neg = new SlashButton(ctx, negLabel, false, 40);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, P5Theme.dpx(ctx, 40));
            lp.rightMargin = P5Theme.dpx(ctx, 8);
            neg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    P5Feedback.cancel(ctx);
                    dlg.dismiss();
                }
            });
            actionBtns.add(neg);
            actions.addView(neg, lp);
        }
        if (neutralLabel != null) {
            SlashButton neu = new SlashButton(ctx, neutralLabel, false, 40);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, P5Theme.dpx(ctx, 40));
            lp.rightMargin = P5Theme.dpx(ctx, 8);
            neu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dlg.dismiss();
                    if (onNeutral != null) onNeutral.run();
                }
            });
            actionBtns.add(neu);
            actions.addView(neu, lp);
        }
        SlashButton pos = new SlashButton(ctx, posLabel != null ? posLabel : "OK", true, 40);
        pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
                if (onPos != null) onPos.run();
            }
        });
        actionBtns.add(pos);
        actions.addView(pos, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, P5Theme.dpx(ctx, 40)));
        frame.addView(actions, new LinearLayout.LayoutParams(contentW, LinearLayout.LayoutParams.WRAP_CONTENT));

        final int[] focused = {actionBtns.size() - 1};
        final boolean[] focusVisible = {sControllerMode};
        if (sControllerMode && !actionBtns.isEmpty()) {
            actionBtns.get(focused[0]).setControllerFocused(true);
        }
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (actionBtns.size() <= 1) return true;
                    if (!focusVisible[0]) {
                        focusVisible[0] = true;
                    } else {
                        actionBtns.get(focused[0]).setControllerFocused(false);
                        int dir = (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ? 1 : -1;
                        focused[0] = (focused[0] + dir + actionBtns.size()) % actionBtns.size();
                    }
                    actionBtns.get(focused[0]).setControllerFocused(true);
                    P5Feedback.tick(ctx);
                    return true;
                }
                if (isConfirmKey(keyCode)) {
                    actionBtns.get(focused[0]).performClick();
                    return true;
                }
                if (isBackKey(keyCode)) {
                    P5Feedback.cancel(ctx);
                    dlg.dismiss();
                    return true;
                }
                return false;
            }
        });

        dlg.setContentView(frame);
        dlg.show();
        frame.playPopIn();
    }

    // ---- 12. Cold-Start Twin-Blade Diagonal Cut Intro Overlay ----

    static final class IntroOverlayView extends View {
        private float progress = 0f; // 0..1 across ~920ms
        private boolean started = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path bladePath = new Path();
        private final Path edgePath = new Path();
        private final Path starPath = new Path();
        private ValueAnimator anim;

        IntroOverlayView(Context context) {
            super(context);
        }

        void play() {
            if (started) return;
            started = true;
            if (anim != null) anim.cancel();
            progress = 0f;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(920);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    progress = (Float) a.getAnimatedValue();
                    invalidate();
                }
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                    if (getParent() instanceof ViewGroup) {
                        ((ViewGroup) getParent()).removeView(IntroOverlayView.this);
                    }
                }
            });
            anim.start();
        }

        private void buildStar(Path p, float cx, float cy, float outerR, float innerR) {
            p.reset();
            for (int i = 0; i < 10; i++) {
                double a = Math.toRadians(-90.0 + i * 36.0);
                float r = (i % 2 == 0) ? outerR : innerR;
                float x = cx + (float) Math.cos(a) * r;
                float y = cy + (float) Math.sin(a) * r;
                if (i == 0) p.moveTo(x, y);
                else p.lineTo(x, y);
            }
            p.close();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            float d = P5Theme.dp(getContext(), 1f);

            // Diagonal cut seam from top-right-of-center to bottom-left-of-center
            float xTop = w * 0.59f;
            float xBot = w * 0.41f;
            float dx = xBot - xTop;
            float dy = h;
            float len = (float) Math.hypot(dx, dy);
            // Unit perpendicular pointing down-right
            float nx = dy / len;
            float ny = -dx / len;

            // Phase 1 split envelope (t = 0.48 -> 1.0)
            float splitRaw = Math.max(0f, Math.min(1f, (progress - 0.48f) / 0.52f));
            // Smooth cubic accelerate-out feel so the blades snap sharply open
            float splitEase = splitRaw * splitRaw * (1.8f - 0.8f * splitRaw);
            if (splitEase > 1f) splitEase = 1f;
            float maxSlide = (float) Math.hypot(w, h) * 0.68f;
            float slide = maxSlide * splitEase;
            float redW = 11f * d;
            float whiteW = 3f * d;

            // Left / Top diagonal blade (slides up-left)
            c.save();
            c.translate(-nx * slide, -ny * slide);
            bladePath.reset();
            bladePath.moveTo(-w, -h);
            bladePath.lineTo(xTop, -h);
            bladePath.lineTo(xTop, 0);
            bladePath.lineTo(xBot, h);
            bladePath.lineTo(xBot, h * 2f);
            bladePath.lineTo(-w, h * 2f);
            bladePath.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(bladePath, paint);

            if (splitRaw > 0.001f) {
                // Bold P5 crimson + crisp white inner blade edge
                edgePath.reset();
                edgePath.moveTo(xTop - nx * redW, 0 - ny * redW);
                edgePath.lineTo(xTop, 0);
                edgePath.lineTo(xBot, h);
                edgePath.lineTo(xBot - nx * redW, h - ny * redW);
                edgePath.close();
                paint.setColor(P5Theme.P5_RED);
                c.drawPath(edgePath, paint);

                edgePath.reset();
                edgePath.moveTo(xTop - nx * whiteW, 0 - ny * whiteW);
                edgePath.lineTo(xTop, 0);
                edgePath.lineTo(xBot, h);
                edgePath.lineTo(xBot - nx * whiteW, h - ny * whiteW);
                edgePath.close();
                paint.setColor(P5Theme.PAPER_WHITE);
                c.drawPath(edgePath, paint);
            }
            c.restore();

            // Right / Bottom diagonal blade (slides down-right)
            c.save();
            c.translate(nx * slide, ny * slide);
            bladePath.reset();
            bladePath.moveTo(xTop, -h);
            bladePath.lineTo(w * 2f, -h);
            bladePath.lineTo(w * 2f, h * 2f);
            bladePath.lineTo(xBot, h * 2f);
            bladePath.lineTo(xBot, h);
            bladePath.lineTo(xTop, 0);
            bladePath.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(bladePath, paint);

            if (splitRaw > 0.001f) {
                edgePath.reset();
                edgePath.moveTo(xTop, 0);
                edgePath.lineTo(xTop + nx * redW, 0 + ny * redW);
                edgePath.lineTo(xBot + nx * redW, h + ny * redW);
                edgePath.lineTo(xBot, h);
                edgePath.close();
                paint.setColor(P5Theme.P5_RED);
                c.drawPath(edgePath, paint);

                edgePath.reset();
                edgePath.moveTo(xTop, 0);
                edgePath.lineTo(xTop + nx * whiteW, 0 + ny * whiteW);
                edgePath.lineTo(xBot + nx * whiteW, h + ny * whiteW);
                edgePath.lineTo(xBot, h);
                edgePath.close();
                paint.setColor(P5Theme.PAPER_WHITE);
                c.drawPath(edgePath, paint);
            }
            c.restore();

            // Razor-thin seam flash right before the blades part (t = 0.41 -> 0.53)
            if (progress >= 0.41f && progress <= 0.53f) {
                float f = 1f - Math.abs((progress - 0.47f) / 0.06f);
                int flashA = Math.round(255 * Math.max(0f, Math.min(1f, f)));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.5f * d);
                paint.setColor(Color.argb(flashA, 246, 245, 240));
                c.drawLine(xTop, 0, xBot, h, paint);
            }

            // Center P5 Star emblem + subtle "CHAKEYY'S VERSION" stamp (t = 0.0 -> 0.62)
            if (progress < 0.62f) {
                float emblemAlpha;
                float starScale;
                if (progress < 0.18f) {
                    float u = progress / 0.18f;
                    float ease = 1f - (1f - u) * (1f - u);
                    starScale = 0.72f + 0.32f * ease; // 0.72 -> 1.04
                    emblemAlpha = Math.min(1f, u * 1.6f);
                } else if (progress < 0.45f) {
                    float u = Math.min(1f, (progress - 0.18f) / 0.12f);
                    starScale = 1.04f - 0.04f * u; // settle to 1.0 and hold cleanly
                    emblemAlpha = 1f;
                } else {
                    float u = (progress - 0.45f) / 0.17f;
                    starScale = 1.0f + 0.14f * u;
                    emblemAlpha = Math.max(0f, 1f - u);
                }

                float cx = w * 0.5f;
                float cy = h * 0.50f;
                float outerR = Math.min(w, h) * 0.185f;
                float innerR = outerR * 0.42f;
                int a = Math.round(255 * emblemAlpha);

                c.save();
                c.translate(cx, cy);
                c.scale(starScale, starScale);
                c.rotate(-18f);

                // Sharp paper-white offset drop layer
                buildStar(starPath, 4.5f * d, 4.5f * d, outerR, innerR);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(a, 246, 245, 240));
                c.drawPath(starPath, paint);

                // Bold P5 crimson star
                buildStar(starPath, 0, 0, outerR, innerR);
                paint.setColor(Color.argb(a, 228, 0, 18));
                c.drawPath(starPath, paint);

                // Crisp white outline
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.6f * d);
                paint.setColor(Color.argb(a, 246, 245, 240));
                c.drawPath(starPath, paint);

                // Inner ink core cutout
                buildStar(starPath, 0, 0, outerR * 0.46f, innerR * 0.46f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(a, 10, 10, 12));
                c.drawPath(starPath, paint);
                c.restore();
            }
        }
    }

    // ---- 13. Snappy Diagonal P5 Launch Wipe Overlay (on PLAY) ----

    static final class LaunchSlashOverlayView extends View {
        private float progress = 0f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path fillPath = new Path();
        private final Path redPath = new Path();
        private final Path whitePath = new Path();
        private ValueAnimator anim;

        LaunchSlashOverlayView(Context context) {
            super(context);
            setClickable(true);
        }

        void play(final Runnable onComplete) {
            if (anim != null) anim.cancel();
            progress = 0f;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(185);
            anim.setInterpolator(new DecelerateInterpolator(1.6f));
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    progress = (Float) a.getAnimatedValue();
                    invalidate();
                }
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) onComplete.run();
                }
            });
            anim.start();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            float d = P5Theme.dp(getContext(), 1f);
            float tilt = w * 0.28f;
            float redW = 28f * d;
            float whiteW = 5f * d;
            // Lead edge sweeps from left-of-screen all the way past right+tilt
            float leadTopX = -redW + (w + tilt + redW * 2f) * progress;
            float leadBotX = leadTopX - tilt;

            paint.setStyle(Paint.Style.FILL);

            // Solid INK_BLACK curtain behind the slash
            fillPath.reset();
            fillPath.moveTo(-tilt, 0);
            fillPath.lineTo(leadTopX, 0);
            fillPath.lineTo(leadBotX, h);
            fillPath.lineTo(-tilt, h);
            fillPath.close();
            paint.setColor(P5Theme.INK_BLACK);
            c.drawPath(fillPath, paint);

            // Leading crimson blade
            redPath.reset();
            redPath.moveTo(leadTopX, 0);
            redPath.lineTo(leadTopX + redW, 0);
            redPath.lineTo(leadBotX + redW, h);
            redPath.lineTo(leadBotX, h);
            redPath.close();
            paint.setColor(P5Theme.P5_RED);
            c.drawPath(redPath, paint);

            // Razor-sharp paper-white edge
            whitePath.reset();
            whitePath.moveTo(leadTopX + redW, 0);
            whitePath.lineTo(leadTopX + redW + whiteW, 0);
            whitePath.lineTo(leadBotX + redW + whiteW, h);
            whitePath.lineTo(leadBotX + redW, h);
            whitePath.close();
            paint.setColor(P5Theme.PAPER_WHITE);
            c.drawPath(whitePath, paint);
        }
    }
}
