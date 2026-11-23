package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;

/** Frames-per-second HUD with configurable format and bounds. */
public class FpsHud extends HudModule {
    private final TextSetting textFormat =
            new TextSetting("Text Format", "Use {fps} for the current frame rate", "{fps} FPS", 48);
    private final ColorSetting color = new ColorSetting("Color", "FPS text color", 0xFFE2E8F0);
    private final BooleanSetting dynamicPadding =
            new BooleanSetting("Dynamic Padding", "Size padding to the rendered text", true);
    private final NumberSetting fixedWidth = new NumberSetting("Width", "Fixed HUD width; zero uses text width", 0, 0, 400, 1);
    private final NumberSetting fixedHeight = new NumberSetting("Height", "Fixed HUD height; zero uses text height", 0, 0, 100, 1);

    public FpsHud() {
        super("FPS", "Frames per second", 'F');
        x = 4;
        y = 4;
        addSetting(textFormat);
        addSetting(color);
        addSetting(dynamicPadding);
        addSetting(fixedWidth);
        addSetting(fixedHeight);
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        String text = textFormat.getValue().replace("{fps}", Integer.toString(ctx.mc.getFps()));
        int textWidth = ctx.textWidth(text);
        int textHeight = ctx.textHeight();
        int padding = dynamicPadding.isEnabled() ? 6 : 0;
        int panelWidth = fixedWidth.getInt() == 0 ? textWidth + padding * 2
                : Math.max(fixedWidth.getInt(), textWidth);
        int panelHeight = fixedHeight.getInt() == 0 ? textHeight + padding * 2
                : Math.max(fixedHeight.getInt(), textHeight);
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        ctx.drawText(text, x + (panelWidth - textWidth) / 2f, y + (panelHeight - textHeight) / 2f,
                color.getValue(), false);
    }
}
