package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class ItemCounterHud extends HudModule {
    private final TextSetting searchItem = new TextSetting("Search Item", "Search by item name or registry ID",
            "minecraft:ender_pearl", 128);
    private final BooleanSetting includeEquipment =
            new BooleanSetting("Include Equipment", "Count armor and offhand slots", true);
    private final ModeSetting display =
            new ModeSetting("Display", "Show an item icon or name", "Icon and Count",
                    List.of("Icon and Count", "Name and Count"));
    private final ColorSetting color = new ColorSetting("Color", "Counter text color", 0xFFFFFFFF);
    private String lastSearch;
    private Item cachedItem;

    public ItemCounterHud() {
        super("Item Counter", "Count a selected item across your inventory", 'I');
        addSetting(searchItem);
        addSetting(includeEquipment);
        addSetting(display);
        addSetting(color);
        x = 10;
        y = 320;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        String query = searchItem.getValue().trim();
        if (!query.equals(lastSearch)) {
            cachedItem = findItem(query);
            lastSearch = query;
        }
        Item item = cachedItem;
        if (item == null) {
            chip(ctx, query.isEmpty() ? "Search for an item" : "No matching item", 0xFFFF5F6D);
            return;
        }

        ItemStack target = item.getDefaultInstance();
        int count = 0;
        if (ctx.player() != null) {
            for (ItemStack stack : ctx.player().getInventory().getNonEquipmentItems()) {
                if (!stack.isEmpty() && stack.is(item)) {
                    count += stack.getCount();
                }
            }
            if (includeEquipment.isEnabled()) {
                for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET, EquipmentSlot.OFFHAND)) {
                    ItemStack stack = ctx.player().getItemBySlot(slot);
                    if (!stack.isEmpty() && stack.is(item)) {
                        count += stack.getCount();
                    }
                }
            }
        }

        boolean showName = display.getValue().equals("Name and Count");
        String label = showName ? target.getHoverName().getString() + ": " + count : "Count: " + count;
        int panelWidth = ctx.textWidth(label) + (showName ? 12 : 34);
        int panelHeight = ctx.textHeight() + 10;
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, Math.min(cornerRadiusPx(), panelHeight / 2),
                0x8E97C4, 0.94f, 0, 70);
        if (showName) {
            ctx.drawText(label, x + 6, y + 5, color.getValue(), true);
        } else {
            ctx.drawItem(target, x + 5, y + 1);
            ctx.drawText(label, x + 26, y + 5, color.getValue(), true);
        }
    }

    private static Item findItem(String query) {
        if (query.isEmpty()) {
            return null;
        }
        Identifier identifier = Identifier.tryParse(query);
        if (identifier != null && BuiltInRegistries.ITEM.containsKey(identifier)) {
            return BuiltInRegistries.ITEM.getValue(identifier);
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        for (Item candidate : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(candidate);
            if (id != null && (id.getPath().equalsIgnoreCase(normalized)
                    || id.toString().equalsIgnoreCase(normalized)
                    || candidate.getDefaultInstance().getHoverName().getString()
                    .toLowerCase(Locale.ROOT).contains(normalized))) {
                return candidate;
            }
        }
        return null;
    }
}
