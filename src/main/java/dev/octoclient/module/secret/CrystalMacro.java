package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Crystal macro: on the obsidian you are looking at, places a crystal and
 * immediately breaks it again.
 */
public class CrystalMacro extends Module implements Module.Tickable {
    private final ModeSetting trigger = new ModeSetting("trigger",
            "Which key starts the macro.", "Use", List.of("Use", "Attack", "Always"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the base block.", 4.5, 0, 6, 0.1);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the base block.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to end crystals automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between macro steps.", 1, 0, 20, 1);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public CrystalMacro() {
        super("CrystalMacro", "Places and breaks a crystal on the block you look at.", 'C');
        addSetting(trigger);
        addSetting(range);
        addSetting(maxSelfDamage);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(delay);
    }

    @Override
    protected void onDisable() {
        swap.release(Minecraft.getInstance(), swapBack.isEnabled());
        timer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
            return;
        }
        swap.respectManualChoice(client);
        if (!triggered(client)) {
            timer = 0;
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockPos base = hit.getBlockPos();
        if (!CrystalUtil.isBase(client.level.getBlockState(base))) {
            return;
        }
        if (client.player.distanceToSqr(
                base.getX() + 0.5, base.getY() + 0.5, base.getZ() + 0.5) > range.getValue() * range.getValue()) {
            return;
        }
        if (rotate.isEnabled()) {
            AuraUtil.lookAt(client.player, base.above());
        }
        AABB area = new AABB(base.getX(), base.getY() + 1, base.getZ(),
                base.getX() + 1, base.getY() + 2, base.getZ() + 1);
        List<EndCrystal> crystals = client.level.getEntitiesOfClass(EndCrystal.class, area);
        if (!crystals.isEmpty()) {
            EndCrystal crystal = crystals.getFirst();
            if (CrystalUtil.selfDamage(client, crystal.position()) > maxSelfDamage.getValue()) {
                return;
            }
            if (CrystalUtil.breakCrystal(client, crystal)) {
                timer = Math.max(1, delay.getInt());
            }
            return;
        }
        if (CrystalUtil.place(client, swap, base)) {
            timer = Math.max(1, delay.getInt());
        }
    }

    private boolean triggered(Minecraft client) {
        return switch (trigger.getValue()) {
            case "Attack" -> client.options.keyAttack.isDown();
            case "Always" -> true;
            default -> client.options.keyUse.isDown();
        };
    }
}
