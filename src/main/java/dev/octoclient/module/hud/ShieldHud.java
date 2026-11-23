package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shield durability while blocking, ported from NoRisk's shield status. */
public class ShieldHud extends HudModule {
    private final BooleanSetting percent = new BooleanSetting("Percent", "Show the remaining percentage", true);

    public ShieldHud() {
        super("Shield Status", "Shield durability while blocking", 'S');
        addSetting(percent);
        this.x = 4;
        this.y = 195;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (ctx.mc.player == null || !ctx.mc.player.isBlocking()) {
            return;
        }
        ItemStack use = ctx.mc.player.getUseItem();
        if (use.isEmpty() || !use.is(Items.SHIELD) || use.getMaxDamage() <= 0) {
            return;
        }
        int max = use.getMaxDamage();
        int left = Math.max(0, max - use.getDamageValue());
        int remaining = Math.round(100f * left / max);
        String text = percent.isEnabled() ? "Shield " + remaining + "%" : "Shield";
        chip(ctx, text, remaining < 30 ? Theme.RED : Theme.EMERALD);
    }
}
