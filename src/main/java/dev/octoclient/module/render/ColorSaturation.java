package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.render.LiquidGlass;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Full-screen color saturation adjustment for the rendered scene. */
public class ColorSaturation extends Module {
    private final NumberSetting hue =
            new NumberSetting("Hue", "Rotate all scene colors", 0, 0, 360, 1);
    private final NumberSetting saturation =
            new NumberSetting("Saturation", "0 is grayscale, 1 is normal, and 2 is highly saturated", 1.0, 0.0, 2.0, 0.05);
    private final NumberSetting brightness =
            new NumberSetting("Brightness", "Adjust scene brightness", 1.0, 0.0, 2.0, 0.05);
    private final NumberSetting contrast =
            new NumberSetting("Contrast", "Adjust scene contrast", 1.0, 0.0, 2.0, 0.05);

    public ColorSaturation() {
        super("Color Saturation", "Adjust the color saturation of the game", 'C');
        addSetting(hue);
        addSetting(saturation);
        addSetting(brightness);
        addSetting(contrast);
    }

    public void renderOverlay(GuiGraphicsExtractor graphics) {
        if (isEnabled() && (Math.abs(hue.getFloat()) > 0.001f
                || Math.abs(saturation.getFloat() - 1.0f) > 0.001f
                || Math.abs(brightness.getFloat() - 1.0f) > 0.001f
                || Math.abs(contrast.getFloat() - 1.0f) > 0.001f)) {
            LiquidGlass.drawSaturation(graphics, hue.getFloat() / 360f, saturation.getFloat(),
                    brightness.getFloat(), contrast.getFloat());
        }
    }
}
