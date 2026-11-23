package dev.octoclient.module.hud;

import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

/** Distance of the last hit, ported from NoRisk's reach display. */
public class ReachDisplay extends HudModule {
    private final NumberSetting showFor =
            new NumberSetting("Show For", "Seconds the distance stays visible", 3.0, 1.0, 10.0, 0.5);

    private double reach;
    private long lastHit;

    public ReachDisplay() {
        super("Reach", "Distance of your last hit", 'R');
        addSetting(showFor);
        this.x = 4;
        this.y = 210;
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (isEnabled() && player != null && entity instanceof LivingEntity && entity != player) {
                reach = player.distanceTo(entity);
                lastHit = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    protected void onDisable() {
        lastHit = 0;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (lastHit == 0 || System.currentTimeMillis() - lastHit > (long) (showFor.getFloat() * 1000f)) {
            return;
        }
        chip(ctx, String.format(Locale.ROOT, "%.2f m", reach), Theme.TEXT_SECONDARY);
    }
}
