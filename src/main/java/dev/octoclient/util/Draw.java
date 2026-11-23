package dev.octoclient.util;

import dev.octoclient.render.LiquidGlass;
import dev.octoclient.render.UiShapes;
import dev.octoclient.module.render.ClearBackground;
import dev.octoclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import net.minecraft.world.item.ItemStack;

/**
 * Enhanced shape helpers with modern effects - glow, shadows, gradients.
 * Rounded corners are approximated with per-row insets.
 * All coordinates are in the caller's (virtual) space.
 */
public final class Draw {
    private Draw() {
    }

    /** Filled rounded rectangle with enhanced smoothness. */
    public static void roundedRect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            gfx.fill(x, y, x + w, y + h, argb);
            return;
        }
        gfx.fill(x, y + r, x + w, y + h - r, argb);
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.sqrt((double) r * r - (double) (r - i - 1) * (r - i - 1));
            gfx.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);
            gfx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, argb);
        }
    }

    /** Rounded rectangle with glow effect. */
    public static void roundedRectGlow(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r, int glowColor, int fillColor) {
        if (w <= 0 || h <= 0) {
            return;
        }
        // Outer glow
        roundedRect(gfx, x - 2, y - 2, w + 4, h + 4, r + 2, Theme.withAlpha(glowColor, 40));
        roundedRect(gfx, x - 1, y - 1, w + 2, h + 2, r + 1, Theme.withAlpha(glowColor, 60));
        // Main body
        roundedRectBorder(gfx, x, y, w, h, r, Theme.withAlpha(glowColor, 120), fillColor);
    }

    /** Soft shadow effect. */
    public static void shadowRect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r) {
        roundedRect(gfx, x + 3, y + 4, w, h, r, Theme.withAlpha(0x000000, 60));
        roundedRect(gfx, x + 2, y + 3, w, h, r, Theme.withAlpha(0x000000, 80));
    }

    /** Rounded rectangle with a 1px border ring. */
    public static void roundedRectBorder(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r,
                                         int borderArgb, int fillArgb) {
        roundedRect(gfx, x, y, w, h, r, borderArgb);
        roundedRect(gfx, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fillArgb);
    }

    /** Vertical gradient panel (top color -> bottom color), rounded, with subtle border. */
    public static void gradientPanel(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r,
                                     int topArgb, int bottomArgb, int borderArgb) {
        // Border pass.
        roundedRect(gfx, x, y, w, h, r, borderArgb);
        // Gradient fill row by row inside the border.
        for (int i = 1; i < h - 1; i++) {
            float t = (i - 1) / (float) Math.max(1, h - 3);
            int color = Theme.mix(topArgb, bottomArgb, t);
            roundedRow(gfx, x + 1, y + i, w - 2, 1, r - 1, color);
        }
    }

    /** One rounded row (used to fake vertical gradients). */
    private static void roundedRow(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r, int argb) {
        r = Math.min(r, w / 2);
        if (r <= 0) {
            gfx.fill(x, y, x + w, y + h, argb);
            return;
        }
        for (int i = 0; i < h; i++) {
            int inset = r - (int) Math.sqrt(Math.max(0, (double) r * r - (double) (r - i - 1) * (r - i - 1)));
            gfx.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);
        }
    }

    /**
     * Liquid-glass panel.
     *
     * <p>Built from additive layers only — there is no blur pass, so the world
     * behind stays perfectly sharp while the sheet still reads as glass:</p>
     * <ol>
     *   <li>a soft contact shadow (three stacked passes instead of a blur),</li>
     *   <li>a tinted body with a smooth vertical grade, brighter at the top,</li>
     *   <li>a specular highlight hugging the top edge,</li>
     *   <li>a dark floor inside the bottom edge, and</li>
     *   <li>a hairline rim that is bright up top and fades toward the bottom.</li>
     * </ol>
     */
    public static void glassPanel(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius) {
        glassPanel(gfx, x, y, w, h, radius, 0xFFFFFF, 1f);
    }

    /** Same as {@link #glassPanel} but lets the caller tint and fade the sheet. */
    public static void glassPanel(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                  int tintRgb, float opacity) {
        if (w <= 0 || h <= 0 || opacity <= 0.01f) {
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        float o = Math.min(1f, opacity);

        // 1. Contact shadow — a short stack reads softer than one hard offset.
        roundedRect(gfx, x + 1, y + 3, w, h, r, withAlphaF(0x000000, 0.16f * o));
        roundedRect(gfx, x + 1, y + 2, w, h, r, withAlphaF(0x000000, 0.13f * o));
        roundedRect(gfx, x, y + 1, w, h, r, withAlphaF(0x000000, 0.10f * o));

        // 2. Body: top-lit grade. The tint is pulled toward white at the top so
        //    the sheet picks up a cool sheen instead of looking like flat paint.
        int top = Theme.mix(0x1C1C22, tintRgb, 0.10f);
        int bottom = Theme.mix(0x0B0B0D, tintRgb, 0.03f);
        fillGlassBody(gfx, x, y, w, h, r, top, bottom, (int) (232 * o));

        // 3. Specular band along the top edge, following the corner curve.
        for (int i = 1; i <= 2; i++) {
            if (i >= h - 1) {
                break;
            }
            int ins = rowInset(r, i, h);
            gfx.fill(x + ins + 1, y + i, x + w - ins - 1, y + i + 1,
                    withAlphaF(0xFFFFFF, (i == 1 ? 0.26f : 0.10f) * o));
        }

        // 4. Floor: a whisper of shadow inside the bottom edge for thickness.
        if (h > 4) {
            int insF = rowInset(r, h - 2, h);
            gfx.fill(x + insF + 1, y + h - 2, x + w - insF - 1, y + h - 1,
                    withAlphaF(0x000000, 0.22f * o));
        }

        // 5. Rim: a faint full outline so the sides never disappear, plus a
        //    bright specular line on the top edge for direction.
        roundedOutline(gfx, x, y, w, h, r, withAlphaF(0xFFFFFF, 0.07f * o));
        gfx.fill(x + rowInset(r, 0, h), y, x + w - rowInset(r, 0, h), y + 1,
                withAlphaF(0xFFFFFF, 0.17f * o));
    }

    /**
     * True 1px rounded outline — unlike {@link #roundedRectBorder} this never
     * paints the interior, so it can sit on top of translucent glass.
     */
    public static void roundedOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int rr = Math.min(r, Math.min(w, h) / 2);
        if (rr <= 0) {
            gfx.fill(x, y, x + w, y + 1, argb);
            gfx.fill(x, y + h - 1, x + w, y + h, argb);
            gfx.fill(x, y, x + 1, y + h, argb);
            gfx.fill(x + w - 1, y, x + w, y + h, argb);
            return;
        }
        // Straight runs.
        gfx.fill(x + rr, y, x + w - rr, y + 1, argb);
        gfx.fill(x + rr, y + h - 1, x + w - rr, y + h, argb);
        gfx.fill(x, y + rr, x + 1, y + h - rr, argb);
        gfx.fill(x + w - 1, y + rr, x + w, y + h - rr, argb);
        // Corner arcs.
        for (int i = 0; i < rr; i++) {
            int c = cornerInset(rr, i);
            gfx.fill(x + c, y + i, x + c + 1, y + i + 1, argb);
            gfx.fill(x + w - c - 1, y + i, x + w - c, y + i + 1, argb);
            gfx.fill(x + c, y + h - i - 1, x + c + 1, y + h - i, argb);
            gfx.fill(x + w - c - 1, y + h - i - 1, x + w - c, y + h - i, argb);
        }
    }

    /** Vertical grade clipped to a rounded rect; smoothstep keeps the highlight band tight. */
    private static void fillGlassBody(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r,
                                      int topArgb, int bottomArgb, int alpha) {
        for (int i = 0; i < h; i++) {
            int ins = rowInset(r, i, h);
            float t = h <= 1 ? 0f : i / (float) (h - 1);
            t = t * t * (3f - 2f * t);
            int c = Theme.mix(topArgb, bottomArgb, t);
            gfx.fill(x + ins, y + i, x + w - ins, y + i + 1, Theme.withAlpha(c, alpha));
        }
    }

    /**
     * Horizontal inset of a rounded rect at row {@code i} of {@code h}.
     * Handles shallow panels by taking the larger of the top and bottom curves.
     */
    private static int rowInset(int r, int i, int h) {
        if (r <= 0) {
            return 0;
        }
        int top = i < r ? cornerInset(r, i) : 0;
        int bottom = (h - 1 - i) < r ? cornerInset(r, h - 1 - i) : 0;
        return Math.max(top, bottom);
    }

    private static int cornerInset(int r, int row) {
        return r - (int) Math.sqrt(Math.max(0.0,
                (double) r * r - (double) (r - row - 1) * (double) (r - row - 1)));
    }

    /** Pack an RGB + 0..1 alpha into ARGB. */
    public static int withAlphaF(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * Draws a panel with the real refractive glass shader, falling back to the
     * layered look when the render pipeline is unavailable.
     *
     * @return true when the refractive path was used
     */
    public static boolean liquidGlass(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                      int tintRgb, float opacity) {
        return liquidGlass(gfx, x, y, w, h, radius, tintRgb, opacity, 0f, 0);
    }

    /**
     * Same as {@link #liquidGlass(GuiGraphicsExtractor, int, int, int, int, int, int, float)}
     * but with a pointer hover amount that brightens the rim.
     */
    public static boolean liquidGlass(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                      int tintRgb, float opacity, float hover) {
        return liquidGlass(gfx, x, y, w, h, radius, tintRgb, opacity, hover, 0);
    }

    /**
     * Same as above, with the border drawn by the shader. A rectangle fill in
     * virtual coordinates snaps to whole design pixels and turns into visible
     * stair steps once the dashboard is scaled up; the SDF rim never does.
     *
     * @param borderAlpha 0..255 strength of the 1px rim
     */
    public static boolean liquidGlass(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                      int tintRgb, float opacity, float hover, int borderAlpha) {
        opacity *= ClearBackground.opacityMultiplier();
        if (LiquidGlass.draw(gfx, x, y, w, h, radius, opacity, hover, tintRgb, borderAlpha)) {
            return true;
        }
        glassPanel(gfx, x, y, w, h, radius, tintRgb, opacity);
        return false;
    }

    /**
     * Whole-window frost behind a screen, square-cornered by construction.
     * Returns false while the frame copy is not ready yet.
     */
    public static boolean screenBlur(GuiGraphicsExtractor gfx, float radiusPx, float dim) {
        return LiquidGlass.drawBlur(gfx, radiusPx, dim);
    }

    /**
     * Clear-liquid sheet: same refraction and rim as {@link #liquidGlass}, but
     * without the smoked body, so a module card shows the scene through it
     * instead of reading as a grey plate.
     */
    public static boolean liquidGlassClear(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                           int tintRgb, float opacity, float hover, int borderAlpha) {
        opacity *= ClearBackground.opacityMultiplier();
        if (LiquidGlass.draw(gfx, x, y, w, h, radius, opacity, hover, tintRgb, borderAlpha, true)) {
            return true;
        }
        glassPanel(gfx, x, y, w, h, radius, tintRgb, opacity);
        return false;
    }

    /**
     * Dark frosted glass, the "Dark Frosted Glass" / "Dark Glassmorphism" card
     * style: a matte, semi-transparent graphite pane with a heavy backdrop
     * frost, rounded corners and a single hairline light rim. Deliberately
     * calmer than {@link #liquidGlass} — no smoked lift, no sheen, no
     * refraction rings.
     *
     * @param borderAlpha 0..255 strength of the hairline rim
     */
    public static boolean frostedGlass(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                       int tintRgb, float opacity, float hover, int borderAlpha) {
        opacity *= ClearBackground.opacityMultiplier();
        if (LiquidGlass.drawFrosted(gfx, x, y, w, h, radius, opacity, hover, tintRgb, borderAlpha)) {
            return true;
        }
        frostedPanel(gfx, x, y, w, h, radius, tintRgb, opacity, borderAlpha);
        return false;
    }

    /**
     * Layered fallback for {@link #frostedGlass} when the render pipeline is
     * unavailable: the same matte graphite body and hairline rim, just without
     * the real backdrop blur.
     */
    public static void frostedPanel(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius,
                                    int tintRgb, float opacity, int borderAlpha) {
        if (w <= 0 || h <= 0 || opacity <= 0.01f) {
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        float o = Math.min(1f, opacity);

        // Soft contact shadow, then a flat graphite body with only a whisper of
        // a vertical grade: the frosted style must not pick up a glossy sheen.
        roundedRect(gfx, x + 1, y + 3, w, h, r, withAlphaF(0x000000, 0.16f * o));
        roundedRect(gfx, x, y + 1, w, h, r, withAlphaF(0x000000, 0.10f * o));
        int top = Theme.mix(Theme.FROST_BODY_TOP, tintRgb, 0.06f);
        int bottom = Theme.mix(Theme.FROST_BODY_BOTTOM, tintRgb, 0.04f);
        fillGlassBody(gfx, x, y, w, h, r, top, bottom, Math.round(238 * o));

        // Very fine, very faint light rim; this is the only highlight the style
        // gets, so it stays a hairline instead of a bevel.
        float rim = Math.min(0.34f, Math.max(0.06f, borderAlpha / 255f * 0.55f));
        roundedOutline(gfx, x, y, w, h, r, withAlphaF(0xFFFFFF, rim * o));
    }

    /** Antialiased rounded fill; falls back to the pixel-snapped rectangle. */
    public static void shapeF(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float r, int argb) {
        if (UiShapes.draw(gfx, x, y, w, h, r, argb)) {
            return;
        }
        pushSnap(gfx, x, y);
        roundedRect(gfx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(r), argb);
        popSnap(gfx);
    }

    /**
     * Animated specular sweep across a glass sheet — the "liquid" part.
     *
     * @param progress 0..1 travel of the band across the panel
     * @param strength peak opacity of the band (0 disables it)
     */
    public static void glassSheen(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int r,
                                  float progress, float strength) {
        if (strength <= 0.005f || w <= 0 || h <= 0) {
            return;
        }
        int rr = Math.min(r, Math.min(w, h) / 2);
        float p = Math.max(0f, Math.min(1f, progress));
        float bandW = Math.max(28f, w * 0.42f);
        float cx = x - bandW + (w + bandW * 2f) * p;
        int steps = 9;
        int stepW = Math.max(1, (int) (bandW / steps));
        for (int s = 0; s < steps; s++) {
            float f = (s + 0.5f) / steps;
            // Sine bell: no hard edges, so the band never looks like a stripe.
            float a = (float) Math.sin(f * Math.PI) * strength;
            int argb = withAlphaF(0xFFFFFF, a);
            int bx = (int) (cx - bandW / 2f) + s * stepW;
            for (int i = 0; i < h; i++) {
                int ins = rowInset(rr, i, h);
                int x0 = Math.max(x + ins, bx);
                int x1 = Math.min(x + w - ins, bx + stepW);
                if (x1 > x0) {
                    gfx.fill(x0, y + i, x1, y + i + 1, argb);
                }
            }
        }
    }

    /** Horizontal progress bar with rounded caps. */
    public static void progressBar(GuiGraphicsExtractor gfx, int x, int y, int w, int h, float fraction,
                                   int trackArgb, int fillArgb) {
        roundedRect(gfx, x, y, w, h, h / 2, trackArgb);
        int fillW = (int) (w * clamp01(fraction));
        if (fillW > h / 2) {
            roundedRect(gfx, x, y, fillW, h, h / 2, fillArgb);
        } else if (fillW > 0) {
            gfx.fill(x, y + 1, x + fillW, y + h - 1, fillArgb);
        }
    }

    /** Slider with a small round knob (dashboard settings). */
    public static void slider(GuiGraphicsExtractor gfx, int x, int y, int w, float fraction, int accentArgb) {
        int trackH = 5;
        roundedRect(gfx, x, y, w, trackH, trackH / 2, Theme.withAlpha(Theme.DARK_950, 205));
        int fillW = (int) (w * clamp01(fraction));
        if (fillW > trackH / 2) {
            roundedRect(gfx, x, y, fillW, trackH, trackH / 2, accentArgb);
        }
        int knobX = x + (int) ((w - 8) * clamp01(fraction));
        // Drop shadow first so the knob reads as sitting on the track.
        roundedRect(gfx, knobX, y, 8, 7, 3, Theme.withAlpha(0x000000, 90));
        roundedRect(gfx, knobX, y - 1, 8, 7, 3, Theme.NEUTRAL_100);
    }

    /** Enhanced pill toggle switch with smooth animations and glow. */
    public static void toggleSwitch(GuiGraphicsExtractor gfx, int x, int y, float on) {
        int w = 36;
        int h = 20;
        int track = Theme.mix(Theme.TOGGLE_OFF_BG, Theme.TOGGLE_ON_BG, on);
        int border = Theme.mix(Theme.TOGGLE_OFF_BORDER, Theme.ACCENT_MID, on);
        if (on > 0.05f) {
            int glowAlpha = (int) (70 * on);
            roundedRect(gfx, x - 2, y - 2, w + 4, h + 4, (h + 4) / 2, Theme.withAlpha(Theme.ACCENT_MID, glowAlpha));
        }
        roundedRectBorder(gfx, x, y, w, h, h / 2, border, track);
        int knobSize = h - 6;
        float travel = Math.min(1.03f, Anim.easeOutBack(on));
        int knobX = x + 3 + (int) ((w - knobSize - 6) * travel);
        if (on > 0.05f) {
            roundedRect(gfx, knobX, y + 2, knobSize, knobSize, knobSize / 2, Theme.withAlpha(0x000000, 50));
        }
        int knobColor = Theme.mix(Theme.TOGGLE_KNOB_OFF, Theme.DARK_950, on);
        roundedRect(gfx, knobX, y + 3, knobSize, knobSize, knobSize / 2, knobColor);
    }

    /**
     * Sub-pixel snap: push a pose offset carrying only the fractional part of
     * (x, y), then draw at the rounded coordinates. The quad lands exactly on
     * the requested float position instead of snapping to whole design pixels,
     * which is what keeps animated knobs and lifts smooth at every GUI scale.
     */
    public static void pushSnap(GuiGraphicsExtractor gfx, float x, float y) {
        gfx.pose().pushMatrix();
        gfx.pose().translate(x - Math.round(x), y - Math.round(y));
    }

    public static void popSnap(GuiGraphicsExtractor gfx) {
        gfx.pose().popMatrix();
    }

    /** Filled circle (used for slider knobs). */
    public static void circle(GuiGraphicsExtractor gfx, float cx, float cy, float r, int argb) {
        pushSnap(gfx, cx - r, cy - r);
        roundedRect(gfx, Math.round(cx - r), Math.round(cy - r),
                Math.round(r * 2f), Math.round(r * 2f), Math.round(r), argb);
        popSnap(gfx);
    }

    /**
     * Pill switch of the dashboard design: dark track, white body when on, and
     * a knob that may overshoot its travel (spring). {@code on} is deliberately
     * not clamped for the knob position so the spring reads.
     */
    public static void switchF(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float on) {
        float a = clamp01(on);

        // Smooth path: the pill, its rim and the knob are SDF shapes, so their
        // edges stay antialiased instead of becoming stair steps at the
        // fractional GUI scales this dashboard renders at.
        if (UiShapes.isAvailable()) {
            int border = Theme.mix(0xFF4A4A52, 0xFF4F8BF7, a);
            int trackColor = Theme.mix(0xE6050506, 0xEAF8F8F8, a);
            if (a > 0.02f) {
                UiShapes.draw(gfx, x - 2f, y - 2f, w + 4f, h + 4f, (h + 4f) / 2f,
                        withAlphaF(Theme.ACCENT_MID, 0.16f * a));
            }
            UiShapes.draw(gfx, x, y, w, h, h / 2f, border);
            UiShapes.draw(gfx, x + 1f, y + 1f, w - 2f, h - 2f, (h - 2f) / 2f, trackColor);

            float knob = h - 6f;
            float kx = x + 3f + on * (w - knob - 6f);
            float ky = y + 3f;
            UiShapes.draw(gfx, kx, ky + 1f, knob, knob, knob / 2f, 0x3C000000);
            UiShapes.draw(gfx, kx, ky, knob, knob, knob / 2f,
                    Theme.mix(Theme.TOGGLE_KNOB_OFF, Theme.DARK_950, a));
            return;
        }

        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(w);
        int ih = Math.round(h);
        pushSnap(gfx, x, y);
        roundedRect(gfx, ix, iy, iw, ih, ih / 2, Theme.withAlpha(0x000000, 230));
        if (a > 0.01f) {
            roundedRect(gfx, ix, iy, iw, ih, ih / 2, withAlphaF(0xF8F8F8, 0.918f * a));
        }
        roundedOutline(gfx, ix, iy, iw, ih, ih / 2, Theme.mix(0xFF4A4A52, 0xFF4F8BF7, a));
        popSnap(gfx);

        int knob = ih - 6;
        float kx = x + 3f + on * (w - knob - 6f);
        float ky = y + 3f;
        pushSnap(gfx, kx, ky);
        roundedRect(gfx, Math.round(kx), Math.round(ky), knob, knob, knob / 2,
                Theme.mix(0xFF8A8A8A, 0xFF050506, a));
        popSnap(gfx);
    }

    /** 4px slider with a circular knob that grows while hovered or dragged. */
    public static void sliderF(GuiGraphicsExtractor gfx, float x, float y, float w, float frac,
                               int argb, boolean hot) {
        float f = clamp01(frac);

        if (UiShapes.isAvailable()) {
            UiShapes.draw(gfx, x, y, w, 4f, 2f, Theme.withAlpha(0xFFFFFF, 26));
            UiShapes.draw(gfx, x, y, Math.max(4f, w * f), 4f, 2f, argb);
            float kx = x + w * f;
            float ky = y + 2f;
            float r = hot ? 6.2f : 5f;
            UiShapes.draw(gfx, kx - r, ky - r, r * 2f, r * 2f, r, 0x73000000);
            UiShapes.draw(gfx, kx - r + 1f, ky - r + 1f, r * 2f - 2f, r * 2f - 2f, r - 0.5f,
                    Theme.NEUTRAL_100);
            return;
        }

        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(w);
        pushSnap(gfx, x, y);
        roundedRect(gfx, ix, iy, iw, 4, 2, Theme.withAlpha(0xFFFFFF, 26));
        int fw = Math.max(4, Math.round(w * f));
        roundedRect(gfx, ix, iy, fw, 4, 2, argb);
        popSnap(gfx);

        float kx = x + w * f;
        float ky = y + 2f;
        float r = hot ? 6.2f : 5f;
        circle(gfx, kx, ky, r, 0xFFF8F8F8);
        pushSnap(gfx, kx - r, ky - r);
        roundedOutline(gfx, Math.round(kx - r), Math.round(ky - r),
                Math.round(r * 2f), Math.round(r * 2f), Math.round(r), Theme.withAlpha(0x000000, 115));
        popSnap(gfx);
    }

    /** Icon glyph rotated around its own centre (gear opening animation). */
    public static void uiIconRot(GuiGraphicsExtractor gfx, int codepoint, float cx, float cy,
                                 int argb, float angle) {
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate(angle);
        uiIcon(gfx, codepoint, 0f, 0f, argb);
        pose.popMatrix();
    }

    /** Checkbox square, checked = white fill with dark tick. */
    public static void checkbox(GuiGraphicsExtractor gfx, int x, int y, boolean checked) {
        int s = 12;
        if (checked) {
            roundedRect(gfx, x, y, s, s, 3, Theme.NEUTRAL_100);
            // Tick drawn from two strokes.
            gfx.fill(x + 3, y + 6, x + 5, y + 8, Theme.DARK_950);
            gfx.fill(x + 5, y + 7, x + 7, y + 9, Theme.DARK_950);
            gfx.fill(x + 7, y + 4, x + 9, y + 6, Theme.DARK_950);
        } else {
            roundedRectBorder(gfx, x, y, s, s, 3, Theme.DARK_600, Theme.DARK_950);
        }
    }

    /** Small square icon tile with a border (logo, gear buttons...). */
    public static void tile(GuiGraphicsExtractor gfx, int x, int y, int size, int radius, int fill, int border) {
        roundedRectBorder(gfx, x, y, size, size, radius, border, fill);
    }

    /**
     * Thin scrollbar: track + draggable thumb.
     *
     * @param vis 0..1 visibility — the caller decays this so the bar dims when
     *            the list has not been scrolled for a while, then relights on
     *            the next wheel tick or hover.
     */
    public static void scrollbar(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                 double scroll, double maxScroll, int contentH, float vis) {
        float v = Math.max(0f, Math.min(1f, vis));
        roundedRect(gfx, x, y, w, h, w / 2, Theme.withAlpha(Theme.DARK_950, (int) (200 * v)));
        int thumbH = Math.max(24, (int) (h * (h / (double) Math.max(h, contentH))));
        int travel = h - thumbH;
        int thumbY = y + (maxScroll <= 0 ? 0 : (int) (travel * (scroll / maxScroll)));
        // Thumb also lightens as it fades in, so reactivity reads at a glance.
        int thumb = Theme.withAlpha(Theme.mix(Theme.DARK_600, Theme.NEUTRAL_200, v * 0.5f), (int) (255 * v));
        roundedRect(gfx, x, thumbY, w, thumbH, w / 2, thumb);
    }

    /** Dashed rectangle outline (HUD editor). */
    public static void dashedRect(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int argb) {
        dashH(gfx, x, y, w, argb);
        dashH(gfx, x, y + h - 1, w, argb);
        dashV(gfx, x, y, h, argb);
        dashV(gfx, x + w - 1, y, h, argb);
    }

    private static void dashH(GuiGraphicsExtractor gfx, int x, int y, int w, int argb) {
        for (int i = 0; i < w; i += 4) {
            gfx.fill(x + i, y, x + Math.min(i + 2, w), y + 1, argb);
        }
    }

    private static void dashV(GuiGraphicsExtractor gfx, int x, int y, int h, int argb) {
        for (int i = 0; i < h; i += 4) {
            gfx.fill(x, y + i, x + 1, y + Math.min(i + 2, h), argb);
        }
    }

    // ------------------------------------------------------------- text / items

    /** Text with optional scale (nested matrix), so titles can be bigger without GUI-scale chunkiness. */
    public static void text(GuiGraphicsExtractor gfx, Font font, String text, float x, float y,
                            int argb, float scale, boolean shadow) {
        if (scale == 1f) {
            gfx.text(font, text, (int) x, (int) y, argb, shadow);
            return;
        }
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        gfx.text(font, text, (int) (x / scale), (int) (y / scale), argb, shadow);
        pose.popMatrix();
    }

    public static float textWidth(Font font, String text, float scale) {
        return font.width(text) * scale;
    }

    // ------------------------------------------------------------- UI font

    /** Text in Minecraft's own font (regular weight). */
    public static void ui(GuiGraphicsExtractor gfx, String text, float x, float y, int argb, boolean shadow) {
        gfx.text(Minecraft.getInstance().font, UiFonts.seq(text, false),
                (int) x, (int) y, argb, shadow);
    }

    /** UI text with scale + optional bold pass. */
    public static void ui(GuiGraphicsExtractor gfx, String text, float x, float y, int argb,
                          float scale, boolean bold, boolean shadow) {
        if (scale == 1f) {
            gfx.text(Minecraft.getInstance().font, UiFonts.seq(text, bold),
                    (int) x, (int) y, argb, shadow);
            return;
        }
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        gfx.text(Minecraft.getInstance().font, UiFonts.seq(text, bold),
                (int) (x / scale), (int) (y / scale), argb, shadow);
        pose.popMatrix();
    }

    /** Horizontally centered UI text. */
    public static void uiCentered(GuiGraphicsExtractor gfx, String text, float cx, float y, int argb,
                                  float scale, boolean bold, boolean shadow) {
        float w = uiWidth(text, bold) * scale;
        ui(gfx, text, cx - w / 2f, y, argb, scale, bold, shadow);
    }

    /** Width of a UI-font string in GUI pixels. */
    public static float uiWidth(String text, boolean bold) {
        return UiFonts.width(text, bold);
    }

    /**
     * Font Awesome icon glyph centered at (cx, cy).
     *
     * <p>FA glyphs fill exactly one em vertically (ascender 0.875em above the
     * baseline, 0.125em below), and MC places the em-box top on the draw Y, so
     * the icon's visual center sits half the em height below Y — the icon font
     * is sized 10, hence the constant offset of 5.</p>
     */
    public static void uiIcon(GuiGraphicsExtractor gfx, int codepoint, float cx, float cy, int argb) {
        uiIcon(gfx, codepoint, cx, cy, argb, 1f);
    }

    /** Scaled Font Awesome icon glyph centered at (cx, cy). */
    public static void uiIcon(GuiGraphicsExtractor gfx, int codepoint, float cx, float cy, int argb, float scale) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(UiFonts.iconComponent(codepoint));
        if (scale == 1f) {
            gfx.text(font, UiFonts.iconSeq(codepoint), (int) (cx - w / 2f), (int) (cy - 5f), argb, false);
            return;
        }
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        gfx.text(font, UiFonts.iconSeq(codepoint), (int) (cx / scale - w / 2f), (int) (cy / scale - 5f), argb, false);
        pose.popMatrix();
    }

    /** Item icon drawn in the current transform (scales with the UI). */
    public static void item(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            gfx.item(stack, x, y);
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
