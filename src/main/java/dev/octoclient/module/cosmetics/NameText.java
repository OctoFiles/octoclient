package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Optional client-side text shown above or below the local player's name tag. */
public class NameText extends Module implements CosmeticPart {
    private static NameText instance;

    private final TextSetting text = new TextSetting("Text", "Text shown with your name", "", 48);
    private final ModeSetting colorMode =
            new ModeSetting("Color Mode", "Choose a solid color or gradient for the text", "Solid",
                    List.of("Solid", "Gradient"));
    private final ColorSetting textColor =
            new ColorSetting("Text Color", "Text color or gradient start color", 0xFFFFFFFF);
    private final ColorSetting gradientEnd =
            new ColorSetting("Gradient End", "Gradient end color", 0xFF8E97C4);
    private final ModeSetting position =
            new ModeSetting("Position", "Show the text above or below your name", "Above", List.of("Above", "Below"));

    public NameText() {
        super("Name Text", "Show custom text above or below your name", 'T');
        addSetting(text);
        addSetting(position);
        addSetting(colorMode);
        addSetting(textColor);
        addSetting(gradientEnd);
        instance = this;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled() && !instance.text.getValue().isBlank();
    }

    public static Component renderTextFor(Player player) {
        NameText module = instance;
        if (module == null || !module.isActive() || player != net.minecraft.client.Minecraft.getInstance().player) {
            return null;
        }
        String label = module.text.getValue().trim();
        if (!"Gradient".equals(module.colorMode.getValue()) || label.length() < 2) {
            return Component.literal(label).withStyle(style ->
                    style.withColor(TextColor.fromRgb(module.textColor.getRgb())));
        }

        int from = module.textColor.getRgb();
        int to = module.gradientEnd.getRgb();
        MutableComponent gradient = Component.empty();
        for (int index = 0; index < label.length(); index++) {
            float amount = index / (float) (label.length() - 1);
            int red = interpolate(from >> 16, to >> 16, amount);
            int green = interpolate(from >> 8, to >> 8, amount);
            int blue = interpolate(from, to, amount);
            gradient.append(Component.literal(label.substring(index, index + 1))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(red << 16 | green << 8 | blue))));
        }
        return gradient;
    }

    public static boolean isAbove() {
        return instance == null || "Above".equals(instance.position.getValue());
    }

    private static int interpolate(int from, int to, float amount) {
        return Math.round((from & 0xFF) * (1f - amount) + (to & 0xFF) * amount);
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.NAMETAG;
    }
}
