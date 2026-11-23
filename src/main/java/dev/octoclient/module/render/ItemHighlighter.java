package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class ItemHighlighter extends Module {
    private static ItemHighlighter instance;

    private final TextSetting query = new TextSetting("Search", "Highlight items matching this text", "", 64);
    private final ModeSetting searchIn =
            new ModeSetting("Search In", "Match item name, identifier, or both", "Both", List.of("Name", "ID", "Both"));
    private final ColorSetting color = new ColorSetting("Highlight Color", "Inventory slot outline color", 0xFFFFC857);

    public ItemHighlighter() {
        super("Item Highlighter", "Highlight matching items in inventory screens", 'H');
        addSetting(query);
        addSetting(searchIn);
        addSetting(color);
        instance = this;
    }

    public static boolean shouldHighlight(ItemStack stack) {
        if (instance == null || !instance.isEnabled() || stack.isEmpty()) {
            return false;
        }
        String text = instance.query.getValue().trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return false;
        }
        boolean nameMatch = stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(text);
        boolean idMatch = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT).contains(text);
        return switch (instance.searchIn.getValue()) {
            case "Name" -> nameMatch;
            case "ID" -> idMatch;
            default -> nameMatch || idMatch;
        };
    }

    public static int highlightColor() {
        return instance == null ? 0xFFFFC857 : instance.color.getValue();
    }
}
