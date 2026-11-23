package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.List;

/** Keeps end crystals or obsidian in the hand, depending on what is needed. */
public class CrystalSwitch extends Module implements Module.Tickable {
    private final ModeSetting mode = new ModeSetting("mode",
            "Which item to keep in the hand.", "Auto", List.of("Auto", "Crystal", "Obsidian"));
    private final NumberSetting range = new NumberSetting("range",
            "How far to look for bases and crystals.", 5.0, 0, 8, 0.1);
    private final BooleanSetting targetOnly = new BooleanSetting("target-only",
            "Only switches when a target is nearby.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between checks.", 2, 0, 20, 1);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public CrystalSwitch() {
        super("CrystalSwitch", "Automatically holds end crystals or obsidian.", 'C');
        addSetting(mode);
        addSetting(range);
        addSetting(targetOnly);
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
        if (timer > 0) {
            timer--;
            return;
        }
        timer = Math.max(1, delay.getInt());
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, 8, "Players", true, true);
        }
        if (target == null && targetOnly.isEnabled()) {
            return;
        }
        switch (mode.getValue()) {
            case "Crystal" -> swap.ensure(client, Items.END_CRYSTAL);
            case "Obsidian" -> swap.ensure(client, Items.OBSIDIAN);
            default -> {
                BlockPos base = target != null
                        ? CrystalUtil.bestBase(client, target, range.getValue(), 0, 20, 0)
                        : null;
                if (base != null) {
                    swap.ensure(client, Items.END_CRYSTAL);
                } else {
                    swap.ensure(client, Items.OBSIDIAN);
                }
            }
        }
    }
}
