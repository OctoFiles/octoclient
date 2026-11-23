package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.ClickCounter;
import dev.octoclient.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/** WASD keys plus LMB/RMB chips with live CPS, styled like the reference design. */
public class KeystrokesHud extends HudModule {
    private static volatile KeystrokesHud instance;
    private static volatile long leftClickUntilNanos;
    private static volatile long rightClickUntilNanos;

    private final NumberSetting scale = new NumberSetting("HUD Scale", "Size of the widget", 1.0, 0.7, 2.5, 0.1);
    private final ModeSetting layout =
            new ModeSetting("Layout", "Choose key cap density", "Grid", java.util.List.of("Grid", "Compact"));
    private final NumberSetting corners = new NumberSetting("Corners", "Key cap corner radius", 6, 0, 18, 1);
    private final NumberSetting fontSize = new NumberSetting("Font Size", "Key label size", 1, 0.7, 2, 0.1);
    private final NumberSetting keyGap = new NumberSetting("Key Gap", "Space between key caps", 3, 0, 10, 1);
    private final BooleanSetting showMouse = new BooleanSetting("Mouse", "Show LMB / RMB with CPS", true);
    private final BooleanSetting showSpace = new BooleanSetting("Space", "Show the space bar row", false);
    private final BooleanSetting showCps = new BooleanSetting("Show CPS", "Show clicks per second below mouse buttons", true);
    private final NumberSetting fadeDelay = new NumberSetting("Key Fade Delay", "Pressed-key animation duration in milliseconds", 100, 0, 500, 10);
    private final ColorSetting idleColor = new ColorSetting("Idle Color", "Unpressed key color", 0xA50E0F14);
    private final ColorSetting pressBackground = new ColorSetting("Press Background", "Pressed key fill color", 0xC4CAD4);
    private final ColorSetting pressTextColor = new ColorSetting("Press Text Color", "Pressed key label color", 0xFFE8ECF2);

    private float wAnim;
    private float aAnim;
    private float sAnim;
    private float dAnim;
    private float spaceAnim;
    private float lmbAnim;
    private float rmbAnim;

    public KeystrokesHud() {
        super("Keystrokes", "WASD and mouse buttons with CPS", 'K');
        instance = this;
        addSetting(scale);
        addSetting(layout);
        addSetting(corners);
        addSetting(fontSize);
        addSetting(keyGap);
        addSetting(showMouse);
        addSetting(showSpace);
        addSetting(showCps);
        addSetting(fadeDelay);
        addSetting(idleColor);
        addSetting(pressBackground);
        addSetting(pressTextColor);
        addSetting(new ActionSetting("Customize", "Open the HUD layout editor with a live Keystrokes preview",
                "Open Layout Editor", () -> Minecraft.getInstance().setScreenAndShow(new dev.octoclient.ui.EditHudScreen())));
        this.x = 160;
        this.y = 92;
    }

    public static void recordMousePress(int button, int action) {
        if (action != GLFW.GLFW_PRESS || Minecraft.getInstance().player == null
                || Minecraft.getInstance().gui.screen() != null || instance == null || !instance.isEnabled()) {
            return;
        }
        long pulseNanos = (long) (Math.max(180f, instance.fadeDelay.getFloat()) * 1_000_000L);
        long until = System.nanoTime() + pulseNanos;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            leftClickUntilNanos = until;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightClickUntilNanos = until;
        }
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Options options = mc.options;
        float s = scale.getFloat();
        boolean compact = layout.getValue().equals("Compact");
        int key = (int) ((compact ? 14 : 18) * s);
        int gap = Math.max(0, Math.round(keyGap.getFloat() * s));
        int pad = Math.max(2, (int) ((compact ? 3 : 5) * s));
        int radius = Math.max(0, Math.round(corners.getFloat() * s));
        float animationStep = fadeDelay.getFloat() <= 0 ? 1f
                : Math.max(0.35f, Math.min(1f, 50f / fadeDelay.getFloat()));
        float dt = Math.max(tickDelta, 0.05f);

        wAnim = Anim.approach(wAnim, options.keyUp.isDown() ? 1 : 0, animationStep, dt);
        aAnim = Anim.approach(aAnim, options.keyLeft.isDown() ? 1 : 0, animationStep, dt);
        sAnim = Anim.approach(sAnim, options.keyDown.isDown() ? 1 : 0, animationStep, dt);
        dAnim = Anim.approach(dAnim, options.keyRight.isDown() ? 1 : 0, animationStep, dt);
        spaceAnim = Anim.approach(spaceAnim, options.keyJump.isDown() ? 1 : 0, animationStep, dt);
        long now = System.nanoTime();
        boolean leftPressed = mc.mouseHandler.isLeftPressed() || now < leftClickUntilNanos;
        boolean rightPressed = mc.mouseHandler.isRightPressed() || now < rightClickUntilNanos;
        lmbAnim = Anim.approach(lmbAnim, leftPressed ? 1 : 0, animationStep, dt);
        rmbAnim = Anim.approach(rmbAnim, rightPressed ? 1 : 0, animationStep, dt);

        // WASD is two rows (W + ASD), so the key block is 2 keys plus one gap.
        // The old 3-key height reserved a whole extra row and left a key-sized
        // hole under the mouse buttons.
        int width = key * 3 + gap * 2 + pad * 2;
        int height = key * 2 + gap + pad * 2;
        if (showSpace.isEnabled()) {
            height += gap + (int) ((compact ? 5 : 7) * s);
        }
        if (showMouse.isEnabled()) {
            height += gap + (int) ((compact ? 17 : 20) * s);
        }
        this.width = width;
        this.height = height;

        // Frosted sheet behind the keys — same liquid-glass pipeline as the
        // dashboard, so the widget reads as glass instead of a flat grey plate.
        int panelR = Math.min(Math.min(width, height) / 2, Math.max(2, radius));
        drawPanel(ctx, (int) x, (int) y, width, height, panelR,
                0x8E97C4, 0.94f, 0f, 70);

        float kx = x + pad;
        float ky = y + pad;
        // W centered above ASD.
        drawKey(ctx, kx + key + gap, ky, key, key, "W", wAnim, radius, s, false, null);
        drawKey(ctx, kx, ky + key + gap, key, key, "A", aAnim, radius, s, false, null);
        drawKey(ctx, kx + key + gap, ky + key + gap, key, key, "S", sAnim, radius, s, false, null);
        drawKey(ctx, kx + (key + gap) * 2, ky + key + gap, key, key, "D", dAnim, radius, s, false, null);
        float rowY = ky + (key + gap) * 2;

        if (showSpace.isEnabled()) {
            int spaceH = (int) ((compact ? 5 : 7) * s);
            drawKey(ctx, kx, rowY, width - pad * 2, spaceH, "", spaceAnim, radius, s, false, null);
            rowY += spaceH + gap;
        }
        if (showMouse.isEnabled()) {
            int mouseH = (int) ((compact ? 17 : 20) * s);
            int halfW = (width - pad * 2 - gap) / 2;
            ClickCounter clicks = ClickCounter.get();
            drawKey(ctx, kx, rowY, halfW, mouseH, "LMB", lmbAnim, radius, s, showCps.isEnabled(),
                    showCps.isEnabled() ? clicks.getLeftCps() + " CPS" : null);
            drawKey(ctx, kx + halfW + gap, rowY, halfW, mouseH, "RMB", rmbAnim, radius, s, showCps.isEnabled(),
                    showCps.isEnabled() ? clicks.getRightCps() + " CPS" : null);
        }
    }

    private void drawKey(HudDrawContext ctx, float kx, float ky, int kw, int kh, String label,
                         float pressed, int radius, float s, boolean withCps, String cpsText) {
        int ix = Math.round(kx);
        int iy = Math.round(ky + pressed * 1.5f);
        // Caps glow neutral grey when held — the accent blue made every screen
        // read like a notification, grey keeps the widget quiet.
        int glow = pressBackground.getRgb();
        int r = Math.max(0, Math.min(radius, Math.min(kw, kh) / 2));

        // Rest: a soft translucent cap with a hairline rim. Pressed: the cap
        // fills solid grey, sinks a pixel and the rim lights up, so a key press
        // is unmistakable.
        int fill = Theme.mix(Theme.withAlpha(idleColor.getRgb(), 165), Theme.withAlpha(glow, 235), pressed);
        int rim = Theme.mix(Theme.withAlpha(0x9AA3B0, 40), Theme.withAlpha(0xE8ECF2, 235), pressed);

        if (pressed > 0.02f) {
            // Grey halo that breathes out while the key is held.
            Draw.shapeF(ctx.gfx, ix - 2, iy - 2, kw + 4, kh + 4, r + 2,
                    Theme.withAlpha(glow, Math.round(80 * pressed)));
        }
        Draw.shapeF(ctx.gfx, ix - 1, iy - 1, kw + 2, kh + 2, r + 1, rim);
        Draw.shapeF(ctx.gfx, ix, iy, kw, kh, r, fill);
        // Top-lit sheen along the upper edge.
        Draw.shapeF(ctx.gfx, ix + 1, iy + 1, kw - 2, Math.max(3, Math.round(kh * 0.35f)),
                Math.max(0, r - 1), Theme.withAlpha(0xFFFFFF, Math.round(14 + 16 * pressed)));

        if (label.isEmpty()) {
            return;
        }
        // Labels scale with the widget so large keystroke pads don't end up
        // with tiny letters; both lines are centred on the key itself.
        float labelScale = Math.max(0.6f, Math.min(2f, s * fontSize.getFloat()));
        if (withCps) {
            float blockH = ctx.textHeight() * labelScale + 1 + ctx.textHeight() * Math.max(1f, labelScale * 0.85f);
            float labelY = ky + (kh - blockH) / 2f;
            float labelW = ctx.textWidth(label) * labelScale;
            ctx.drawTextScaled(label, kx + (kw - labelW) / 2f, labelY,
                    Theme.mix(Theme.TEXT_SECONDARY, pressTextColor.getRgb(), pressed), labelScale, false);
            if (cpsText != null) {
                float cpsScale = Math.max(1f, labelScale * 0.85f);
                float cpsW = ctx.textWidth(cpsText) * cpsScale;
                ctx.drawTextScaled(cpsText, kx + (kw - cpsW) / 2f,
                        labelY + ctx.textHeight() * labelScale + 1,
                        Theme.mix(Theme.TEXT_FAINT, pressTextColor.getRgb(), pressed * 0.7f), cpsScale, false);
            }
        } else {
            float labelW = ctx.textWidth(label) * labelScale;
            float labelY = ky + (kh - ctx.textHeight() * labelScale) / 2f;
            ctx.drawTextScaled(label, kx + (kw - labelW) / 2f, labelY,
                    Theme.mix(Theme.TEXT_SECONDARY, pressTextColor.getRgb(), pressed), labelScale, false);
        }
    }
}
