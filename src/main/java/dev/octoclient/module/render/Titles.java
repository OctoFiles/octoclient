package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Settings for on-screen title placement, scale and transition style. */
public class Titles extends Module {
    private static Titles instance;
    private final NumberSetting yOffset = new NumberSetting("Y Offset", "Vertical title position offset", 0, -100, 100, 1);
    private final NumberSetting scale = new NumberSetting("Scale", "Title and subtitle scale", 1, 0.5, 2, 0.05);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", "Overall title overlay scale", 1, 0.5, 2, 0.05);
    private final BooleanSetting showInHudEditor =
            new BooleanSetting("Show in HUD Editor", "Preview the title overlay in the HUD editor", true);
    private final ModeSetting animation = new ModeSetting("Animation", "Title transition", "Vanilla",
            List.of("Vanilla", "Fade", "Slide", "None"));
    private final BooleanSetting overrideTitle =
            new BooleanSetting("Custom Title", "Replace incoming title text", false);
    private final TextSetting titleText =
            new TextSetting("Title Text", "Text shown instead of the server title", "Title", 64);
    private final BooleanSetting overrideSubtitle =
            new BooleanSetting("Custom Subtitle", "Replace incoming subtitle text", false);
    private final TextSetting subtitleText =
            new TextSetting("Subtitle Text", "Text shown instead of the server subtitle", "", 96);

    public Titles() {
        super("Titles", "Customize title messages on screen", 'T');
        addSetting(yOffset);
        addSetting(scale);
        addSetting(hudScale);
        addSetting(showInHudEditor);
        addSetting(animation);
        addSetting(overrideTitle);
        addSetting(titleText);
        addSetting(overrideSubtitle);
        addSetting(subtitleText);
        instance = this;
    }

    public static Titles active() {
        return instance != null && instance.isEnabled() ? instance : null;
    }

    public int yOffset() {
        return yOffset.getInt();
    }

    public float scale() {
        return scale.getFloat() * hudScale.getFloat();
    }

    public static void renderEditorPreview(GuiGraphicsExtractor graphics, int width, int height) {
        Titles module = active();
        if (module == null || !module.showInHudEditor.isEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Component title = Component.literal(module.displayedTitle("Title Preview"));
        String subtitleText = module.displayedSubtitle("Subtitle Preview");
        Component subtitle = subtitleText.isEmpty() ? null : Component.literal(subtitleText);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(width / 2f, height / 2f - 20 + module.yOffset());
        graphics.pose().scale(module.scale(), module.scale());
        graphics.text(client.font, title, -client.font.width(title) / 2, 0, 0xFFFFFFFF, true);
        if (subtitle != null) {
            graphics.text(client.font, subtitle, -client.font.width(subtitle) / 2, 10, 0xFFD8D8D8, true);
        }
        graphics.pose().popMatrix();
    }

    public String animation() {
        return animation.getValue();
    }

    public String displayedTitle(String incoming) {
        return overrideTitle.isEnabled() ? titleText.getValue() : incoming;
    }

    public boolean customTitleEnabled() {
        return overrideTitle.isEnabled();
    }

    public String displayedSubtitle(String incoming) {
        return overrideSubtitle.isEnabled() ? subtitleText.getValue() : incoming;
    }

    public boolean customSubtitleEnabled() {
        return overrideSubtitle.isEnabled();
    }
}
