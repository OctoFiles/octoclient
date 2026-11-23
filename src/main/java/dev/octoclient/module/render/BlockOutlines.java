package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;

/** Controls the selected-block outline's color and stroke thickness. */
public class BlockOutlines extends Module {
    private static BlockOutlines instance;
    private final BooleanSetting outlineEnabled = new BooleanSetting("Outline Enabled", "Render the selected block outline", true);
    private final ColorSetting color = new ColorSetting("Color", "Selected block outline color", 0xFFFFFFFF);
    private final BooleanSetting rgb = new BooleanSetting("RGB", "Cycle the outline through colors", false);
    private final NumberSetting lineWidth = new NumberSetting("Width", "Outline thickness", 1.5, 0.5, 5, 0.25);
    private final BooleanSetting overlayEnabled = new BooleanSetting("Overlay Enabled", "Fill the selected block shape", false);
    private final ColorSetting overlayColor = new ColorSetting("Overlay Color", "Selected block fill color", 0x66FFFFFF);
    private final BooleanSetting overlayRgb = new BooleanSetting("Overlay RGB", "Cycle the overlay through colors", false);
    private final NumberSetting overlayAlpha = new NumberSetting("Overlay Alpha", "Selected block fill opacity", 40, 0, 100, 1);

    public BlockOutlines() {
        super("Block Outlines", "Customize the outline of the targeted block", 'O');
        addSetting(outlineEnabled);
        addSetting(color);
        addSetting(rgb);
        addSetting(lineWidth);
        addSetting(overlayEnabled);
        addSetting(overlayColor);
        addSetting(overlayRgb);
        addSetting(overlayAlpha);
        instance = this;
    }

    public static boolean active() {
        return instance != null && instance.isEnabled();
    }

    public static int color() {
        return instance == null ? 0xFFFFFFFF : instance.rgb.isEnabled()
                ? 0xFF000000 | Mth.hsvToRgb((System.currentTimeMillis() % 6000L) / 6000f, 1f, 1f)
                : instance.color.getValue();
    }

    public static float lineWidth() {
        return instance == null ? 1f : instance.lineWidth.getFloat();
    }

    public static boolean outlineEnabled() {
        return instance == null || !instance.isEnabled() || instance.outlineEnabled.isEnabled();
    }

    public static void submitOverlay(BlockOutlineRenderState state) {
        BlockOutlines module = instance;
        if (module == null || !module.isEnabled() || !module.overlayEnabled.isEnabled()) {
            return;
        }
        int base = module.overlayRgb.isEnabled()
                ? 0xFF000000 | Mth.hsvToRgb((System.currentTimeMillis() % 6000L) / 6000f, 1f, 1f)
                : module.overlayColor.getValue();
        int alpha = Math.round(module.overlayAlpha.getFloat() * 2.55f);
        int tint = (alpha << 24) | (base & 0xFFFFFF);
        if (state.shape() != null && !state.shape().isEmpty()) {
            var bounds = state.shape().bounds().move(state.pos());
            Gizmos.cuboid(bounds, GizmoStyle.fill(tint));
        }
    }
}
