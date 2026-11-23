package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Armor + offhand as icon chips with durability percentages. */
public class ArmorStatusHud extends HudModule {
    private final NumberSetting scale = new NumberSetting("Scale", "Icon size", 1.0, 0.7, 2.0, 0.1);
    private final ModeSetting orientation = new ModeSetting("Orientation", "Chip layout",
            "Horizontal", List.of("Horizontal", "Vertical"));
    private final BooleanSetting showPercent = new BooleanSetting("Percent", "Show durability percentage", true);
    private final BooleanSetting showOffhand = new BooleanSetting("Offhand", "Include the offhand slot", true);

    private float appearAnim;

    public ArmorStatusHud() {
        super("Armor Status", "Armor durability at a glance", 'A');
        addSetting(scale);
        addSetting(orientation);
        addSetting(showPercent);
        addSetting(showOffhand);
        this.x = 70;
        this.y = 130;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (ctx.player() == null) {
            return;
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);
        int alpha = (int) (Anim.clamp01(appearAnim) * 255);

        float s = scale.getFloat();
        int iconSize = (int) (16 * s);
        int chipH = iconSize + 8;
        int chipPad = 6;
        int gap = 3;
        int pad = 4;

        ItemStack[] items = {
                ctx.getArmor(EquipmentSlot.HEAD),
                ctx.getArmor(EquipmentSlot.CHEST),
                ctx.getArmor(EquipmentSlot.LEGS),
                ctx.getArmor(EquipmentSlot.FEET),
                showOffhand.isEnabled() ? ctx.getOffhand() : ItemStack.EMPTY,
        };

        List<ItemStack> shown = new java.util.ArrayList<>();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                shown.add(item);
            }
        }
        if (shown.isEmpty()) {
            appearAnim = Anim.approach(appearAnim, 0, 0.4f, tickDelta);
            return;
        }

        // Measure chips.
        int[] chipWs = new int[shown.size()];
        int totalMain = 0;
        for (int i = 0; i < shown.size(); i++) {
            String pct = percentText(shown.get(i));
            int w = pad * 2 + iconSize + (pct.isEmpty() ? 0 : chipPad + ctx.textWidth(pct));
            chipWs[i] = w;
            totalMain += w + gap;
        }
        totalMain -= gap;

        boolean horizontal = !"Vertical".equals(orientation.getValue());
        int width;
        int height;
        if (horizontal) {
            width = totalMain + pad * 2;
            height = chipH + pad * 2;
        } else {
            width = chipWs[0] + pad * 2;
            height = shown.size() * (chipH + gap) - gap + pad * 2;
        }
        this.width = width;
        this.height = height;

        drawPanel(ctx, (int) x, (int) y, width, height, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);

        float cx = x + pad;
        float cy = y + pad;
        for (int i = 0; i < shown.size(); i++) {
            ItemStack stack = shown.get(i);
            Draw.roundedRect(ctx.gfx, (int) cx, (int) cy, chipWs[i], chipH, 6,
                    Theme.withAlpha(Theme.DARK_900, (int) (0.8f * alpha)));
            ctx.outline(cx, cy, cx + chipWs[i], cy + chipH, Theme.withAlpha(Theme.DARK_700, alpha));
            ctx.drawItem(stack, cx + pad, cy + 4);

            String pct = percentText(stack);
            if (!pct.isEmpty()) {
                int pctX = (int) (cx + pad + iconSize + chipPad);
                ctx.drawText(pct, pctX, cy + (chipH - ctx.textHeight()) / 2f + 1,
                        Theme.withAlpha(durabilityColor(stack), alpha), false);
            }

            if (horizontal) {
                cx += chipWs[i] + gap;
            } else {
                cy += chipH + gap;
            }
        }
    }

    private String percentText(ItemStack stack) {
        if (!showPercent.isEnabled() || stack.getMaxDamage() <= 0) {
            return "";
        }
        int max = stack.getMaxDamage();
        int pct = Math.round(100f * (max - stack.getDamageValue()) / max);
        return pct + "%";
    }

    private static int durabilityColor(ItemStack stack) {
        if (stack.getMaxDamage() <= 0) {
            return Theme.TEXT_SECONDARY;
        }
        int max = stack.getMaxDamage();
        int pct = Math.round(100f * (max - stack.getDamageValue()) / max);
        if (pct > 60) {
            return Theme.TEXT_SECONDARY;
        }
        if (pct > 25) {
            return Theme.YELLOW;
        }
        return Theme.RED;
    }
}
