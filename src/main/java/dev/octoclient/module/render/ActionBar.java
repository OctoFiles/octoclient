package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;

/** Custom positioning and styling for vanilla action-bar messages. */
public class ActionBar extends Module {
    private static ActionBar instance;
    private final NumberSetting xOffset = new NumberSetting("X Offset", "Horizontal position offset", 0, -300, 300, 1);
    private final NumberSetting yOffset = new NumberSetting("Y Offset", "Vertical position offset", 0, -150, 150, 1);
    private final NumberSetting scale = new NumberSetting("HUD Scale", "Action-bar message scale", 1, 0.5, 2, 0.05);
    private final BooleanSetting dynamicPadding =
            new BooleanSetting("Dynamic Padding", "Fit the action-bar text to its natural size", true);
    private final NumberSetting width = new NumberSetting("Width", "Target width in GUI pixels; zero uses text width", 0, 0, 500, 1);
    private final NumberSetting height = new NumberSetting("Height", "Target height in GUI pixels; zero uses text height", 0, 0, 100, 1);
    private final ColorSetting textColor = new ColorSetting("Text Color", "Action-bar text color", 0xFFFFFFFF);

    public ActionBar() {
        super("Action Bar", "Move and style item and status messages", 'B');
        addSetting(xOffset);
        addSetting(yOffset);
        addSetting(scale);
        addSetting(dynamicPadding);
        addSetting(width);
        addSetting(height);
        addSetting(textColor);
        instance = this;
    }

    public static ActionBar active() {
        return instance != null && instance.isEnabled() ? instance : null;
    }

    public int xOffset() {
        return xOffset.getInt();
    }

    public int yOffset() {
        return yOffset.getInt();
    }

    public float scale() {
        return scale.getFloat();
    }

    public float scaleX(int textWidth) {
        return scale.getFloat() * targetScale(width.getInt(), textWidth, dynamicPadding.isEnabled());
    }

    public float scaleY(int textHeight) {
        return scale.getFloat() * targetScale(height.getInt(), textHeight, dynamicPadding.isEnabled());
    }

    private float targetScale(int target, int naturalSize, boolean useNaturalSize) {
        if (useNaturalSize || target <= 0 || naturalSize <= 0) {
            return 1f;
        }
        return Math.max(0.25f, Math.min(4f, target / (float) naturalSize));
    }

    public int textColor() {
        return textColor.getValue();
    }
}
