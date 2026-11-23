package dev.octoclient.ui;

import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.hud.HudDrawContext;
import dev.octoclient.module.hud.HudModule;
import dev.octoclient.module.render.Titles;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import dev.octoclient.util.UiFonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD drag & drop editor with snapping guides and an edit-mode banner.
 * ESC or the Save button writes positions to config/client_hud.json.
 */
public class EditHudScreen extends Screen {
    private static final int SNAP_DISTANCE = 6;
    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int DELETE_SIZE = 14;

    private HudModule dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private HudModule resizing;
    private ResizeCorner resizeCorner;
    private double resizeAnchorX;
    private double resizeAnchorY;
    private float resizeBaseWidth;
    private float resizeBaseHeight;
    private float resizeMaxScale;
    private HudModule hovered;
    private int mouseX;
    private int mouseY;
    private int snapGuideX = Integer.MIN_VALUE;
    private int snapGuideY = Integer.MIN_VALUE;

    private float appearAnim;
    /** Banner entrance spring (0..1). */
    private float bannerIn;
    /** Seconds since the editor opened, used for staggered intros. */
    private float intro;
    /** Running time in seconds for subtle pulses. */
    private float time;
    /** Save button hover / press amounts. */
    private float saveHover;
    private float savePress;
    /** Snap-guide fade. */
    private float guideA;

    /** Per-module hover/selection springs, so frames never pop. */
    private final Map<HudModule, FrameAnim> frameAnims = new HashMap<>();

    public EditHudScreen() {
        super(Component.literal("Edit HUD"));
    }

    @Override
    public void init() {
        super.init();
        // No vanilla tutorial toasts flashing over the editor.
        if (minecraft != null && minecraft.gui != null) {
            minecraft.gui.toastManager().clear();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ----------------------------------------------------------------- banner

    /** Same text step the dashboard uses, so the editor reads as one UI. */
    private static final float S_UI = 1.15f;
    private static final int BANNER_H = 30;
    private static final int BANNER_PAD = 10;
    private static final int BANNER_TILE = 20;
    private static final float SAVE_W = 54f;
    private static final float SAVE_H = 18f;

    private static final String BANNER_TITLE = "HUD EDITOR";
    private static final String BANNER_HINT = "Drag to move  |  Scroll or pull a corner to resize  |";
    private static final String BANNER_SAVE_HINT = "saves & closes";

    private static float kbdWidth() {
        return Draw.uiWidth("ESC", true) * S_UI + 14f;
    }

    private int bannerWidth() {
        float w = BANNER_PAD * 2f
                + BANNER_TILE + 8f
                + Draw.uiWidth(BANNER_TITLE, true) * S_UI + 14f
                + 1f + 14f
                + Draw.uiWidth(BANNER_HINT, false) * S_UI + 14f
                + kbdWidth() + 6f
                + Draw.uiWidth(BANNER_SAVE_HINT, false) * S_UI + 12f
                + SAVE_W;
        return Math.round(w);
    }

    private int bannerX() {
        return (width - bannerWidth()) / 2;
    }

    private int bannerY() {
        return 10;
    }

    private int saveButtonX() {
        return Math.round(bannerX() + bannerWidth() - BANNER_PAD - SAVE_W);
    }

    private int saveButtonY() {
        return Math.round(bannerY() + (BANNER_H - SAVE_H) / 2f);
    }

    // ----------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        float pt = partialTick;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        appearAnim = Anim.approach(appearAnim, 1f, 0.38f, pt);
        bannerIn = Anim.approach(bannerIn, 1f, 0.45f, pt);
        intro += pt / 20f;
        time += pt / 20f;
        guideA = Anim.approach(guideA,
                dragging != null && (snapGuideX != Integer.MIN_VALUE || snapGuideY != Integer.MIN_VALUE)
                        ? 1f : 0f, 0.55f, pt);
        float saveHot = inRect(mouseX, mouseY, saveButtonX(), saveButtonY(), SAVE_W, SAVE_H) ? 1f : 0f;
        saveHover = Anim.approach(saveHover, saveHot, 0.5f, pt);
        savePress = Anim.approach(savePress, 0f, 0.22f, pt);

        // No blur here either: the world behind stays sharp, only dimmed.
        gfx.fill(0, 0, width, height, 0x9A08090C);

        HudDrawContext ctx = new HudDrawContext(gfx, minecraft, pt, width, height);

        // Snap guides (accent glow) while dragging.
        if (dragging != null) {
            drawGuides(gfx);
        }

        List<HudModule> hudModules = ModuleManager.get().getHudModules();
        hovered = findAt(mouseX, mouseY, hudModules);
        for (int i = 0; i < hudModules.size(); i++) {
            HudModule module = hudModules.get(i);
            if (!module.isEnabled()) {
                continue;
            }
            boolean selected = module == dragging || module == resizing;
            boolean isHovered = module == hovered;
            module.renderScaled(ctx, pt, true, selected);

            FrameAnim fa = frameAnim(module);
            fa.hover = Anim.approach(fa.hover, isHovered ? 1f : 0f, 0.5f, pt);
            fa.selected = Anim.approach(fa.selected, selected ? 1f : 0f, 0.55f, pt);
            float focus = Math.max(fa.hover, fa.selected);

            int frameX = (int) module.x - 3;
            int frameY = (int) module.y - 3;
            int frameW = (int) module.width + 6;
            int frameH = (int) module.height + 6;
            if (frameW <= 6 || frameH <= 6) {
                continue;
            }

            // Staggered intro: every frame eases in a beat after the previous.
            float frameIn = Anim.easeOut(Anim.clamp01((intro - i * 0.03f) / 0.32f));
            float appear = appearAnim * frameIn;

            // Accent halo + dark backdrop fade in with the hover spring; the
            // selected halo breathes so the dragged widget never looks frozen.
            if (focus > 0.01f) {
                float pulse = 0.92f + 0.08f * (float) Math.sin(time * 5f + i * 0.7f);
                float halo = (0.28f * fa.hover + 0.52f * fa.selected) * pulse;
                float grow = 2f + 3f * focus;
                Draw.shapeF(gfx, frameX - grow, frameY - grow, frameW + grow * 2f, frameH + grow * 2f,
                        9f + grow, fade(Theme.withAlpha(Theme.ACCENT_MID, Math.round(60f * halo)), appear));
                Draw.shapeF(gfx, frameX, frameY, frameW, frameH, 9f,
                        fade(Theme.withAlpha(0x000000, Math.round(110f * focus)), appear));
            }

            int faint = Theme.withAlpha(0xFFFFFF, 70);
            int bright = Theme.withAlpha(0xFFFFFF, 210);
            int color = Theme.mix(faint, bright, focus);
            Draw.roundedOutline(gfx, frameX, frameY, frameW, frameH, 9, fade(color, appear));
            if (fa.selected > 0.01f) {
                Draw.roundedOutline(gfx, frameX + 1, frameY + 1, frameW - 2, frameH - 2, 8,
                        fade(Theme.withAlpha(Theme.ACCENT_MID, Math.round(130f * fa.selected)), appear));
            }

            // Every enabled module keeps its remove button; it just scales in
            // with the hover state instead of appearing abruptly.
            drawDeleteButton(gfx, module, focus, appear);

            // "SIZE" pill slides up out of the frame while hovered/selected.
            if (focus > 0.02f) {
                float chipA = focus * appear;
                String label = "SIZE";
                String value = Math.round(module.sizeSetting().getFloat() * 100f) + "%";
                float lw = Draw.uiWidth(label, true) * S_UI;
                float vw = Draw.uiWidth(value, false) * S_UI;
                float chipW = 21f + lw + 8f + vw + 12f;
                float chipH = 20f;
                float chipX = frameX;
                float chipBottom = frameY - 6f;
                boolean above = chipBottom - chipH >= 4f;
                float chipY = above ? chipBottom - chipH : frameY + frameH + 6f;
                float ease = focus >= 1f ? 1f : Anim.easeOutBack(focus);
                float slide = (1f - Math.min(1f, ease)) * 6f * (above ? 1f : -1f);
                float cy = chipY + slide;
                float scale = 0.92f + 0.08f * Anim.clamp01(ease);
                pushScale(gfx, chipX + chipW / 2f, cy + chipH / 2f, scale);
                Draw.shapeF(gfx, chipX, cy, chipW, chipH, chipH / 2f, fade(0xE6141519, chipA));
                Draw.shapeF(gfx, chipX, cy, chipW, chipH, chipH / 2f,
                        fade(Theme.withAlpha(0xFFFFFF, 46), chipA));
                Draw.shapeF(gfx, chipX + 1f, cy + 1f, chipW - 2f, chipH - 2f, chipH / 2f - 1f,
                        fade(0xE6141519, chipA));
                Draw.uiIcon(gfx, UiFonts.ICON_SLIDERS, chipX + 11f, cy + chipH / 2f,
                        fade(Theme.ACCENT_MID, chipA), 0.7f);
                Draw.ui(gfx, label, chipX + 20f, cy + 5.5f, fade(Theme.TEXT_FAINT, chipA),
                        S_UI, true, false);
                Draw.ui(gfx, value, chipX + 20f + lw + 8f, cy + 5.5f,
                        fade(Theme.TEXT_PRIMARY, chipA), S_UI, false, false);
                popScale(gfx);
            }

            if (fa.selected > 0.02f) {
                drawResizeHandles(gfx, frameX, frameY, frameW, frameH, fa.selected);
            }
        }

        Titles.renderEditorPreview(gfx, width, height);
        drawBanner(gfx, mouseX, mouseY);
    }

    private FrameAnim frameAnim(HudModule module) {
        return frameAnims.computeIfAbsent(module, k -> new FrameAnim());
    }

    private static void pushScale(GuiGraphicsExtractor gfx, float cx, float cy, float scale) {
        gfx.pose().pushMatrix();
        gfx.pose().translate(cx, cy);
        gfx.pose().scale(scale, scale);
        gfx.pose().translate(-cx, -cy);
    }

    private static void popScale(GuiGraphicsExtractor gfx) {
        gfx.pose().popMatrix();
    }

    private void drawBanner(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        float a = Anim.clamp01(bannerIn);
        if (a < 0.01f) {
            return;
        }
        float rise = Anim.clamp01(Anim.easeOutBack(bannerIn));
        int bx = bannerX();
        int by = Math.round(bannerY() - (1f - rise) * 28f);
        int bw = bannerWidth();

        // Floating liquid-glass pill, same material as the dashboard chrome.
        Draw.shapeF(gfx, bx, by + 2f, bw, BANNER_H, BANNER_H / 2f, fade(0x2E000000, a));
        Draw.liquidGlass(gfx, bx, by, bw, BANNER_H, BANNER_H / 2, 0x8E97C4, 0.92f * a, -1f, 28);

        float x = bx + BANNER_PAD;
        float tileY = by + (BANNER_H - BANNER_TILE) / 2f;
        float textY = by + (BANNER_H - 8f * S_UI) / 2f;

        // Icon tile with the editors' sliders icon.
        Draw.shapeF(gfx, x, tileY, BANNER_TILE, BANNER_TILE, 7f, fade(Theme.withAlpha(Theme.ACCENT_MID, 150), a));
        Draw.shapeF(gfx, x + 1f, tileY + 1f, BANNER_TILE - 2f, BANNER_TILE - 2f, 6f,
                fade(Theme.withAlpha(Theme.ACCENT_MID, 46), a));
        Draw.uiIcon(gfx, UiFonts.ICON_SLIDERS, x + BANNER_TILE / 2f, tileY + BANNER_TILE / 2f,
                fade(Theme.TEXT_PRIMARY, a), 0.72f);
        x += BANNER_TILE + 8f;

        // Title.
        Draw.ui(gfx, BANNER_TITLE, x, textY, fade(Theme.TEXT_PRIMARY, a), S_UI, true, false);
        x += Draw.uiWidth(BANNER_TITLE, true) * S_UI + 14f;

        // Divider.
        gfx.fill(Math.round(x), Math.round(by + 10f), Math.round(x) + 1, Math.round(by + BANNER_H - 10f),
                fade(Theme.withAlpha(0xFFFFFF, 30), a));
        x += 15f;

        // Hint.
        Draw.ui(gfx, BANNER_HINT, x, textY, fade(Theme.TEXT_MUTED, a), S_UI, false, false);
        x += Draw.uiWidth(BANNER_HINT, false) * S_UI + 14f;

        // ESC keycap.
        float kw = kbdWidth();
        float kh = 15f;
        float ky = by + (BANNER_H - kh) / 2f;
        Draw.shapeF(gfx, x, ky, kw, kh, kh / 2f, fade(Theme.withAlpha(0xFFFFFF, 62), a));
        Draw.shapeF(gfx, x + 1f, ky + 1f, kw - 2f, kh - 2f, kh / 2f - 1f, fade(0xCC191A20, a));
        Draw.uiCentered(gfx, "ESC", x + kw / 2f, ky + 3.5f, fade(Theme.TEXT_PRIMARY, a), S_UI, true, false);
        x += kw + 6f;

        Draw.ui(gfx, BANNER_SAVE_HINT, x, textY, fade(Theme.TEXT_MUTED, a), S_UI, false, false);

        // Save button: bright pill, dark label. Hover and press are springs, so
        // the pill grows and settles instead of toggling between two states.
        float sx = saveButtonX();
        float sy = saveButtonY();
        float saveScale = 1f + 0.05f * saveHover - 0.05f * savePress;
        pushScale(gfx, sx + SAVE_W / 2f, sy + SAVE_H / 2f, saveScale);
        if (saveHover > 0.01f) {
            Draw.shapeF(gfx, sx - 2f, sy - 2f, SAVE_W + 4f, SAVE_H + 4f, (SAVE_H + 4f) / 2f,
                    fade(Theme.withAlpha(Theme.ACCENT_MID, Math.round(80f * saveHover)), a));
        }
        Draw.shapeF(gfx, sx, sy, SAVE_W, SAVE_H, SAVE_H / 2f,
                fade(Theme.withAlpha(0xFFFFFF, Math.round(228f + 27f * saveHover)), a));
        Draw.uiIcon(gfx, UiFonts.ICON_CHECK, sx + 13f, sy + SAVE_H / 2f,
                fade(0xFF101216, a * (0.75f + 0.25f * saveHover)), 0.72f);
        Draw.ui(gfx, "Save", sx + 21f, sy + 4.5f, fade(0xFF101216, a), S_UI, true, false);
        popScale(gfx);
    }

    private static int fade(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Anim.clamp01(a));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private void drawGuides(GuiGraphicsExtractor gfx) {
        if (snapGuideX != Integer.MIN_VALUE) {
            glowLineV(gfx, snapGuideX);
        }
        if (snapGuideY != Integer.MIN_VALUE) {
            glowLineH(gfx, snapGuideY);
        }
    }

    private SnapResult snapAxis(double position, double extent, double screenExtent,
                                List<HudModule> modules, boolean horizontal) {
        double bestPosition = position;
        double bestDistance = SNAP_DISTANCE + 1;
        int guide = Integer.MIN_VALUE;

        double[] movingAnchors = {0, extent / 2, extent};
        double[] screenAnchors = {0, screenExtent / 2, screenExtent};
        for (int i = 0; i < movingAnchors.length; i++) {
            double target = screenAnchors[i];
            double candidate = target - movingAnchors[i];
            double distance = Math.abs(candidate - position);
            if (distance <= SNAP_DISTANCE && distance < bestDistance) {
                bestPosition = candidate;
                bestDistance = distance;
                guide = (int) Math.round(target);
            }
        }

        for (HudModule other : modules) {
            if (other == dragging || !other.isEnabled()) {
                continue;
            }
            double otherPosition = horizontal ? other.x : other.y;
            double otherExtent = horizontal ? other.width : other.height;
            double[] targetAnchors = {otherPosition, otherPosition + otherExtent / 2, otherPosition + otherExtent};
            for (double movingAnchor : movingAnchors) {
                for (double target : targetAnchors) {
                    double candidate = target - movingAnchor;
                    double distance = Math.abs(candidate - position);
                    if (distance <= SNAP_DISTANCE && distance < bestDistance) {
                        bestPosition = candidate;
                        bestDistance = distance;
                        guide = (int) Math.round(target);
                    }
                }
            }
        }
        return new SnapResult(bestPosition, guide);
    }

    private void glowLineV(GuiGraphicsExtractor gfx, int x) {
        float pulse = guideA * (0.82f + 0.18f * (float) Math.sin(time * 8f));
        gfx.fill(x - 2, 0, x + 3, height, fade(Theme.SNAP_GUIDE_GLOW, pulse));
        gfx.fill(x, 0, x + 1, height, fade(Theme.SNAP_GUIDE, pulse));
    }

    private void glowLineH(GuiGraphicsExtractor gfx, int y) {
        float pulse = guideA * (0.82f + 0.18f * (float) Math.sin(time * 8f));
        gfx.fill(0, y - 2, width, y + 3, fade(Theme.SNAP_GUIDE_GLOW, pulse));
        gfx.fill(0, y, width, y + 1, fade(Theme.SNAP_GUIDE, pulse));
    }

    /** Screen rect of a module's "remove from HUD" button. */
    private int[] deleteButtonRect(HudModule module) {
        int frameX = (int) module.x - 3;
        int frameY = (int) module.y - 3;
        int frameW = (int) module.width + 6;
        // Inside the frame's top-right corner: always on screen and always
        // clickable, even for modules hugging the window edge.
        int bx = frameX + frameW - DELETE_SIZE - 2;
        int by = frameY + 2;
        bx = Math.max(0, Math.min(width - DELETE_SIZE, bx));
        by = Math.max(0, Math.min(height - DELETE_SIZE, by));
        return new int[]{bx, by, DELETE_SIZE, DELETE_SIZE};
    }

    private void drawDeleteButton(GuiGraphicsExtractor gfx, HudModule module, float focus, float appear) {
        int[] r = deleteButtonRect(module);
        boolean hot = focus > 0.35f && inRect(mouseX, mouseY, r[0], r[1], r[2], r[3]);
        float a = Anim.clamp01(0.45f + 0.55f * focus) * Anim.clamp01(appear);
        float ease = Anim.clamp01(Anim.easeOutBack(Anim.clamp01(focus)));
        float scale = 0.82f + 0.18f * ease + (hot ? 0.08f : 0f);
        float cx = r[0] + r[2] / 2f;
        float cy = r[1] + r[3] / 2f;
        float rad = (r[2] / 2f) * scale;
        // Glass circle that turns red on hover, matching the dashboard chips.
        Draw.circle(gfx, cx, cy, rad + 1f, fade(hot ? 0xFFFFA0A4 : Theme.withAlpha(0xFFFFFF, 74), a));
        Draw.circle(gfx, cx, cy, rad, fade(hot ? 0xFFE5484D : 0xD914161B, a));
        Draw.uiIcon(gfx, UiFonts.ICON_XMARK, cx, cy, fade(hot ? 0xFFFFFFFF : 0xFFC6CCD6, a),
                0.62f * Math.max(0.85f, scale));
    }

    private void drawResizeHandles(GuiGraphicsExtractor gfx, int x, int y, int w, int h, float sel) {
        int handle = RESIZE_HANDLE_SIZE;
        float ease = Anim.clamp01(Anim.easeOutBack(Anim.clamp01(sel)));
        float scale = 0.55f + 0.45f * ease;
        int[][] positions = {
                {x - handle / 2, y - handle / 2},
                {x + w - handle / 2, y - handle / 2},
                {x - handle / 2, y + h - handle / 2},
                {x + w - handle / 2, y + h - handle / 2}
        };
        for (int[] position : positions) {
            float cx = position[0] + handle / 2f;
            float cy = position[1] + handle / 2f;
            float rad = (handle / 2f) * scale;
            Draw.circle(gfx, cx, cy, rad + 1.2f, fade(Theme.ACCENT_MID, sel));
            Draw.circle(gfx, cx, cy, rad - 0.8f, fade(0xFF14161B, sel));
        }
    }

    private ResizeCorner findResizeCorner(double mouseX, double mouseY, HudModule module) {
        double left = module.x;
        double top = module.y;
        double right = left + module.width;
        double bottom = top + module.height;
        double radius = RESIZE_HANDLE_SIZE;
        if (inRect(mouseX, mouseY, left - radius / 2, top - radius / 2, radius, radius)) {
            return ResizeCorner.TOP_LEFT;
        }
        if (inRect(mouseX, mouseY, right - radius / 2, top - radius / 2, radius, radius)) {
            return ResizeCorner.TOP_RIGHT;
        }
        if (inRect(mouseX, mouseY, left - radius / 2, bottom - radius / 2, radius, radius)) {
            return ResizeCorner.BOTTOM_LEFT;
        }
        if (inRect(mouseX, mouseY, right - radius / 2, bottom - radius / 2, radius, radius)) {
            return ResizeCorner.BOTTOM_RIGHT;
        }
        return null;
    }

    private void beginResize(HudModule module, ResizeCorner corner) {
        resizing = module;
        resizeCorner = corner;
        resizeBaseWidth = module.width / module.sizeSetting().getFloat();
        resizeBaseHeight = module.height / module.sizeSetting().getFloat();
        resizeAnchorX = corner.isLeft() ? module.x + module.width : module.x;
        resizeAnchorY = corner.isTop() ? module.y + module.height : module.y;
        double availableWidth = corner.isLeft() ? resizeAnchorX : width - resizeAnchorX;
        double availableHeight = corner.isTop() ? resizeAnchorY : height - resizeAnchorY;
        resizeMaxScale = (float) Math.max(module.sizeSetting().getMin(),
                Math.min(module.sizeSetting().getMax(),
                        Math.min(availableWidth / resizeBaseWidth, availableHeight / resizeBaseHeight)));
    }

    private void updateResize(double mouseX, double mouseY) {
        double desiredWidth = resizeCorner.isLeft() ? resizeAnchorX - mouseX : mouseX - resizeAnchorX;
        double desiredHeight = resizeCorner.isTop() ? resizeAnchorY - mouseY : mouseY - resizeAnchorY;
        double scaleX = desiredWidth / resizeBaseWidth;
        double scaleY = desiredHeight / resizeBaseHeight;
        float scale = (float) ((scaleX + scaleY) / 2.0);
        var size = resizing.sizeSetting();
        scale = Anim.clamp(scale, (float) size.getMin(), resizeMaxScale);
        size.setRaw(scale);

        if (resizeCorner.isLeft()) {
            resizing.x = (float) (resizeAnchorX - resizeBaseWidth * scale);
        } else {
            resizing.x = (float) resizeAnchorX;
        }
        if (resizeCorner.isTop()) {
            resizing.y = (float) (resizeAnchorY - resizeBaseHeight * scale);
        } else {
            resizing.y = (float) resizeAnchorY;
        }
    }

    private HudModule findAt(double mouseX, double mouseY, List<HudModule> modules) {
        for (int i = modules.size() - 1; i >= 0; i--) {
            HudModule module = modules.get(i);
            if (!module.isEnabled()) {
                continue;
            }
            if (mouseX >= module.x - 4 && mouseX <= module.x + module.width + 4
                    && mouseY >= module.y - 4 && mouseY <= module.y + module.height + 4) {
                return module;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Remove-from-HUD button. Check the module under the cursor first so
        // overlapping frames cannot steal the click from the visible X.
        List<HudModule> enabled = ModuleManager.get().getHudModules().stream()
                .filter(HudModule::isEnabled).toList();
        HudModule topMost = findAt(event.x(), event.y(), enabled);
        if (topMost != null) {
            int[] r = deleteButtonRect(topMost);
            if (inRect(event.x(), event.y(), r[0], r[1], r[2], r[3])) {
                topMost.setEnabled(false);
                ConfigManager.save();
                return true;
            }
        }
        for (HudModule module : enabled) {
            if (module == topMost) {
                continue;
            }
            int[] r = deleteButtonRect(module);
            if (inRect(event.x(), event.y(), r[0], r[1], r[2], r[3])) {
                module.setEnabled(false);
                ConfigManager.save();
                return true;
            }
        }

        // Save button.
        if (inRect(event.x(), event.y(), saveButtonX(), saveButtonY(), SAVE_W, SAVE_H)) {
            savePress = 1f;
            onClose();
            return true;
        }

        HudModule hit = findAt(event.x(), event.y(), ModuleManager.get().getHudModules());
        if (hit != null) {
            ResizeCorner corner = findResizeCorner(event.x(), event.y(), hit);
            if (corner != null) {
                beginResize(hit, corner);
                return true;
            }
            dragging = hit;
            dragOffsetX = event.x() - hit.x;
            dragOffsetY = event.y() - hit.y;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (resizing != null) {
            updateResize(event.x(), event.y());
            return true;
        }
        if (dragging != null) {
            double newX = event.x() - dragOffsetX;
            double newY = event.y() - dragOffsetY;
            List<HudModule> modules = ModuleManager.get().getHudModules();
            SnapResult horizontalSnap = snapAxis(newX, dragging.width, width, modules, true);
            SnapResult verticalSnap = snapAxis(newY, dragging.height, height, modules, false);
            newX = horizontalSnap.position();
            newY = verticalSnap.position();
            snapGuideX = horizontalSnap.guide();
            snapGuideY = verticalSnap.guide();

            newX = Anim.clamp((float) newX, 0, Math.max(0, width - dragging.width));
            newY = Anim.clamp((float) newY, 0, Math.max(0, height - dragging.height));

            dragging.x = (float) newX;
            dragging.y = (float) newY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (resizing != null) {
            resizing = null;
            resizeCorner = null;
            ConfigManager.save();
            return true;
        }
        if (dragging != null) {
            dragging = null;
            snapGuideX = Integer.MIN_VALUE;
            snapGuideY = Integer.MIN_VALUE;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudModule module = findAt(mouseX, mouseY, ModuleManager.get().getHudModules());
        if (module == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        var size = module.sizeSetting();
        double next = size.getSnapped() + Math.signum(scrollY) * size.getStep() * 2;
        size.setRaw(next);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ConfigManager.saveHud();
        ConfigManager.save();
        super.onClose();
    }

    private static boolean inRect(double px, double py, double x, double y, double w, double h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private record SnapResult(double position, int guide) {
    }

    /** Per-module spring state for hover and selection. */
    private static final class FrameAnim {
        float hover;
        float selected;
    }

    private enum ResizeCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        private boolean isLeft() {
            return this == TOP_LEFT || this == BOTTOM_LEFT;
        }

        private boolean isTop() {
            return this == TOP_LEFT || this == TOP_RIGHT;
        }
    }
}
