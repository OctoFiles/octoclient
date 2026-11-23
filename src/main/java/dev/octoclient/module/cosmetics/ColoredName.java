package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Client-side solid or two-color gradient for the local player's name. */
public class ColoredName extends Module implements CosmeticPart {
    private static ColoredName instance;

    private final ModeSetting mode =
            new ModeSetting("Color Mode", "Choose a solid color, gradient, or rainbow", "Solid",
                    List.of("Solid", "Gradient", "Rainbow"));
    private final BooleanSetting animate =
            new BooleanSetting("Animate Rainbow", "Cycle rainbow colors over time", true);
    private final ColorSetting startColor =
            new ColorSetting("Name Color", "Solid color or gradient start color", 0xFFFFFFFF);
    private final ColorSetting endColor =
            new ColorSetting("Gradient End", "Gradient end color", 0xFF8E97C4);

    public ColoredName() {
        super("Colored Name", "Set a solid color or gradient on your name", 'C');
        addSetting(mode);
        addSetting(animate);
        addSetting(startColor);
        addSetting(endColor);
        instance = this;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    public static boolean appliesTo(Player player) {
        return isActive() && player == net.minecraft.client.Minecraft.getInstance().player;
    }

    public static Component apply(Player player, Component name) {
        ColoredName module = instance;
        if (module == null || !appliesTo(player) || name == null) {
            return name;
        }

        String text = name.getString();
        MutableComponent coloredName = Component.empty();
        String colorMode = module.mode.getValue();
        if ("Rainbow".equals(colorMode)) {
            float timeOffset = module.animate.isEnabled()
                    ? (System.currentTimeMillis() % 3_000L) / 3_000f
                    : 0f;
            int length = Math.max(1, text.length());
            for (int index = 0; index < text.length(); index++) {
                float hue = (index / (float) length + timeOffset) % 1f;
                coloredName.append(Component.literal(text.substring(index, index + 1))
                        .withStyle(name.getStyle().withColor(TextColor.fromRgb(rainbowColor(hue)))));
            }
            return coloredName;
        }
        if (!"Gradient".equals(colorMode) || text.length() < 2) {
            return coloredName.append(name.copy().withStyle(
                    name.getStyle().withColor(TextColor.fromRgb(module.startColor.getRgb()))));
        }

        int from = module.startColor.getRgb();
        int to = module.endColor.getRgb();
        for (int index = 0; index < text.length(); index++) {
            float amount = index / (float) (text.length() - 1);
            int red = channel(from >> 16, to >> 16, amount);
            int green = channel(from >> 8, to >> 8, amount);
            int blue = channel(from, to, amount);
            int color = red << 16 | green << 8 | blue;
            coloredName.append(Component.literal(text.substring(index, index + 1))
                    .withStyle(name.getStyle().withColor(TextColor.fromRgb(color))));
        }
        return coloredName;
    }

    private static int rainbowColor(float hue) {
        float sector = hue * 6f;
        int index = (int) sector;
        float fraction = sector - index;
        float rising = fraction;
        return switch (index % 6) {
            case 0 -> 0xFF0000 | Math.round(rising * 255f) << 8;
            case 1 -> 0xFFFF00 - (Math.round(rising * 255f) << 16);
            case 2 -> 0x00FF00 | Math.round(rising * 255f);
            case 3 -> 0x00FFFF - (Math.round(rising * 255f) << 8);
            case 4 -> 0x0000FF | Math.round(rising * 255f) << 16;
            default -> 0xFF00FF - Math.round(rising * 255f);
        };
    }

    private static int channel(int from, int to, float amount) {
        return Math.round(((from & 0xFF) * (1f - amount)) + ((to & 0xFF) * amount));
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.NAMETAG;
    }
}
