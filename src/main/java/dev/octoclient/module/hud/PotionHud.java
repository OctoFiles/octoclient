package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Active potion effects with icon, name and remaining time. */
public class PotionHud extends HudModule {
    private final ModeSetting sort = new ModeSetting("Sort By", "Effect ordering", "Default",
            List.of("Default", "Name", "Duration"));
    private final ModeSetting view = new ModeSetting("View", "HUD layout", "Cards",
            List.of("Cards", "Compact"));
    private final BooleanSetting showIcon = new BooleanSetting("Icons", "Show potion icons", true);
    private final NumberSetting maxEffects =
            new NumberSetting("Max Effects", "Maximum visible effects", 5, 1, 20, 1);
    private final BooleanSetting dynamicPadding =
            new BooleanSetting("Dynamic Padding", "Use compact spacing around the effect list", true);
    private final NumberSetting fixedWidth = new NumberSetting("Width", "Minimum panel width", 0, 0, 500, 1);
    private final NumberSetting fixedHeight = new NumberSetting("Height", "Minimum panel height", 0, 0, 500, 1);
    private final BooleanSetting showTime = new BooleanSetting("Show Time", "Show remaining effect durations", true);
    private final BooleanSetting hideVanillaPotionStatus =
            new BooleanSetting("Hide Vanilla Potion Status", "Hide Minecraft's duplicate effect list", false);
    private final BooleanSetting showBackground =
            new BooleanSetting("Show Background", "Show the effect panel and row backgrounds", true);
    private final BooleanSetting dontShiftInventory =
            new BooleanSetting("Don't Shift Inventory", "Hide vanilla effect icons while the inventory is open", false);
    private final BooleanSetting dontShowInInventory =
            new BooleanSetting("Don't Show in Inventory", "Hide this widget while the inventory is open", false);

    private float appearAnim;

    public PotionHud() {
        super("Potion Status", "Active effects with timers", 'P');
        addSetting(sort);
        addSetting(view);
        addSetting(showIcon);
        addSetting(maxEffects);
        addSetting(dynamicPadding);
        addSetting(fixedWidth);
        addSetting(fixedHeight);
        addSetting(showTime);
        addSetting(hideVanillaPotionStatus);
        addSetting(showBackground);
        addSetting(dontShiftInventory);
        addSetting(dontShowInInventory);
        this.x = 180;
        this.y = 30;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        if (mc.player == null) {
            return;
        }
        if (dontShowInInventory.isEnabled() && mc.gui.screen() instanceof InventoryScreen) {
            width = 0;
            height = 0;
            return;
        }
        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        if (effects.isEmpty()) {
            appearAnim = Anim.approach(appearAnim, 0, 0.4f, tickDelta);
            return;
        }
        if (sort.getValue().equals("Name")) {
            effects.sort(Comparator.comparing(e -> e.getEffect().value().getDisplayName().getString()));
        } else if (sort.getValue().equals("Duration")) {
            effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        }
        if (effects.size() > maxEffects.getInt()) {
            effects = effects.subList(0, maxEffects.getInt());
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);

        if (view.getValue().equals("Compact")) {
            renderCompact(ctx, effects);
            return;
        }

        int iconSize = 16;
        int chipH = showTime.isEnabled() ? 22 : 14;
        int chipPad = 6;
        int gap = 3;
        int pad = dynamicPadding.isEnabled() ? 4 : 0;

        int maxTextW = 0;
        for (MobEffectInstance effect : effects) {
            String label = name(effect);
            if (showTime.isEnabled()) {
                label += " " + duration(effect);
            }
            maxTextW = Math.max(maxTextW, ctx.textWidth(label));
        }

        int panelW = pad * 2 + (showIcon.isEnabled() ? iconSize + chipPad : 0) + maxTextW + chipPad * 2;
        int panelH = pad * 2 + effects.size() * (chipH + gap) - gap;
        panelW = Math.max(panelW, fixedWidth.getInt());
        panelH = Math.max(panelH, fixedHeight.getInt());
        this.width = panelW;
        this.height = panelH;

        int alpha = (int) (Anim.clamp01(appearAnim) * 255);
        if (showBackground.isEnabled()) {
            drawPanel(ctx, (int) x, (int) y, panelW, panelH, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        }

        float cy = y + pad;
        for (MobEffectInstance effect : effects) {
            int chipX = (int) x + pad;
            if (showBackground.isEnabled()) {
                Draw.roundedRect(ctx.gfx, chipX, (int) cy, panelW - pad * 2, chipH, 6,
                        Theme.withAlpha(Theme.DARK_900, (int) (0.8f * alpha)));
                ctx.outline(chipX, cy, chipX + panelW - pad * 2, cy + chipH,
                        Theme.withAlpha(Theme.DARK_700, alpha));
            }

            float tx = chipX + chipPad;
            if (showIcon.isEnabled()) {
                // Category-colored glow behind the vanilla effect sprite.
                int glow = switch (effect.getEffect().value().getCategory()) {
                    case BENEFICIAL -> 0x553B82F6;
                    case HARMFUL -> 0x55EF4444;
                    default -> 0x55A3A3A3;
                };
                int iconX = chipX + 3;
                int iconY = (int) cy + 3;
                Draw.roundedRect(ctx.gfx, iconX, iconY, iconSize, iconSize, 4,
                        Theme.withAlpha(glow, alpha));
                ctx.drawEffectIcon(effect.getEffect(), iconX, iconY, iconSize);
                tx = iconX + iconSize + chipPad;
            }

            int nameColor = switch (effect.getEffect().value().getCategory()) {
                case BENEFICIAL -> Theme.TEXT_PRIMARY;
                case HARMFUL -> Theme.RED;
                default -> Theme.TEXT_SECONDARY;
            };
            ctx.drawText(name(effect), tx, cy + 3, Theme.withAlpha(nameColor, alpha), false);
            if (showTime.isEnabled()) {
                ctx.drawText(duration(effect), tx, cy + 12,
                        Theme.withAlpha(Theme.TEXT_FAINT, alpha), false);
            }
            cy += chipH + gap;
        }
    }

    private void renderCompact(HudDrawContext ctx, List<MobEffectInstance> effects) {
        int iconSize = showIcon.isEnabled() ? 12 : 0;
        int rowHeight = 14;
        int pad = dynamicPadding.isEnabled() ? 5 : 0;
        int maxTextW = effects.stream()
                .mapToInt(effect -> ctx.textWidth(showTime.isEnabled()
                        ? name(effect) + " " + duration(effect) : name(effect)))
                .max().orElse(0);
        int panelW = pad * 2 + iconSize + (iconSize > 0 ? 4 : 0) + maxTextW;
        int panelH = pad * 2 + effects.size() * rowHeight;
        panelW = Math.max(panelW, fixedWidth.getInt());
        panelH = Math.max(panelH, fixedHeight.getInt());
        width = panelW;
        height = panelH;
        if (showBackground.isEnabled()) {
            drawPanel(ctx, (int) x, (int) y, panelW, panelH, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        }
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            int rowY = (int) y + pad + i * rowHeight;
            int textX = (int) x + pad;
            if (showIcon.isEnabled()) {
                ctx.drawEffectIcon(effect.getEffect(), textX, rowY, iconSize);
                textX += iconSize + 4;
            }
            int color = switch (effect.getEffect().value().getCategory()) {
                case BENEFICIAL -> Theme.TEXT_PRIMARY;
                case HARMFUL -> Theme.RED;
                default -> Theme.TEXT_SECONDARY;
            };
            String text = showTime.isEnabled()
                    ? name(effect) + " " + duration(effect) : name(effect);
            ctx.drawText(text, textX, rowY, color, false);
        }
    }

    public static boolean shouldHideVanillaStatus() {
        PotionHud module = ModuleManagerLookup.get();
        return module != null && module.isEnabled()
                && (module.hideVanillaPotionStatus.isEnabled()
                || (module.dontShiftInventory.isEnabled()
                && Minecraft.getInstance().gui.screen() instanceof InventoryScreen));
    }

    private static final class ModuleManagerLookup {
        private static PotionHud get() {
            return dev.octoclient.module.ModuleManager.get()
                    .byName("Potion Status", PotionHud.class).orElse(null);
        }
    }

    private static String name(MobEffectInstance effect) {
        Component base = effect.getEffect().value().getDisplayName();
        int amp = effect.getAmplifier();
        if (amp >= 1 && amp <= 9) {
            return base.getString() + " "
                    + Component.translatable("enchantment.level." + (amp + 1)).getString();
        }
        return base.getString();
    }

    private static String duration(MobEffectInstance effect) {
        Minecraft mc = Minecraft.getInstance();
        float tickrate = mc.level != null ? mc.level.tickRateManager().tickrate() : 20f;
        return MobEffectUtil.formatDuration(effect, 1.0f, tickrate).getString();
    }
}
