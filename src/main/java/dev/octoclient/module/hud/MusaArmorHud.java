package dev.octoclient.module.hud;

import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Compact alternative armor and held-item durability widget. */
public class MusaArmorHud extends HudModule {
    private final BooleanSetting showWidget = new BooleanSetting("Show Widget", "Render the armor HUD", true);
    private final BooleanSetting durability = new BooleanSetting("Durability", "Show item durability", true);
    private final BooleanSetting showIcons = new BooleanSetting("Show Icons", "Show equipment item icons", true);
    private final BooleanSetting stickyHud = new BooleanSetting("Sticky HUD", "Keep all equipment slots visible when empty", false);
    private final ModeSetting style =
            new ModeSetting("Style", "Show durability as text or a bar", "Percent", List.of("Percent", "Bar", "Icon"));
    private final ModeSetting orientation =
            new ModeSetting("Orientation", "Arrange item cards", "Horizontal", List.of("Horizontal", "Vertical"));
    private final ColorSetting barColor = new ColorSetting("Bar Color", "Durability bar color", 0xFF42D392);

    public MusaArmorHud() {
        super("Musa's Armor HUD", "Alternative armor and held-item status display", 'M');
        addSetting(showWidget);
        addSetting(durability);
        addSetting(showIcons);
        addSetting(stickyHud);
        addSetting(style);
        addSetting(orientation);
        addSetting(barColor);
        x = 70;
        y = 175;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (!showWidget.isEnabled()) {
            return;
        }
        if (ctx.player() == null) {
            return;
        }
        ItemStack[] slots = {
                ctx.getArmor(EquipmentSlot.HEAD),
                ctx.getArmor(EquipmentSlot.CHEST),
                ctx.getArmor(EquipmentSlot.LEGS),
                ctx.getArmor(EquipmentSlot.FEET),
                ctx.getMainHand()
        };
        List<ItemStack> shown = new ArrayList<>();
        if (stickyHud.isEnabled()) {
            shown.addAll(List.of(slots));
        } else {
            for (ItemStack stack : slots) {
                if (!stack.isEmpty()) {
                    shown.add(stack);
                }
            }
        }
        if (shown.isEmpty()) {
            width = 64;
            height = 28;
            return;
        }

        int icon = 16;
        int cardWidth = 28;
        int cardHeight = 34;
        int gap = 3;
        boolean horizontal = orientation.getValue().equals("Horizontal");
        width = horizontal ? shown.size() * (cardWidth + gap) - gap : cardWidth;
        height = horizontal ? cardHeight : shown.size() * (cardHeight + gap) - gap;
        drawPanel(ctx, (int) x, (int) y, (int) width, (int) height, Math.min(cornerRadiusPx(), 8),
                0x8E97C4, 0.94f, 0, 70);

        for (int i = 0; i < shown.size(); i++) {
            ItemStack stack = shown.get(i);
            int cardX = (int) x + (horizontal ? i * (cardWidth + gap) : 0);
            int cardY = (int) y + (horizontal ? 0 : i * (cardHeight + gap));
            int itemX = cardX + (cardWidth - icon) / 2;
            if (showIcons.isEnabled()) {
                ctx.drawItem(stack, itemX, cardY + 1);
            }
            if (durability.isEnabled() && style.getValue().equals("Percent") && stack.getMaxDamage() > 0) {
                int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
                int percent = Math.round(remaining * 100f / stack.getMaxDamage());
                String label = percent + "%";
                ctx.drawText(label, cardX + (cardWidth - ctx.textWidth(label)) / 2f, cardY + icon + 2,
                        barColor.getValue(), false);
            } else if (durability.isEnabled() && style.getValue().equals("Bar") && stack.getMaxDamage() > 0) {
                int barX = cardX + 4;
                int barY = cardY + cardHeight - 6;
                int barWidth = cardWidth - 8;
                float fraction = (float) Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                ctx.fill(barX, barY, barX + barWidth, barY + 3, 0xAA242631);
                ctx.fill(barX, barY, barX + Math.round(barWidth * fraction), barY + 3, barColor.getValue());
            }
        }
    }
}
