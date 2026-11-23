package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.List;

/** Places obsidian around the target so it cannot run away from the crystals. */
public class CrystalSurround extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to surround.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum placement distance.", 4.5, 0, 6, 0.1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards each block.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to obsidian automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between blocks.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never surrounds friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public CrystalSurround() {
        super("CrystalSurround", "Surrounds the target with obsidian for crystals.", 'C');
        addSetting(targets);
        addSetting(range);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(delay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
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
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        }
        if (target == null) {
            return;
        }
        for (AnchorUtil.Spot spot : AnchorUtil.surroundSpots(client, target, range.getValue())) {
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, spot.pos());
            }
            if (AnchorUtil.placeWith(client, swap, Items.OBSIDIAN, spot)) {
                timer = Math.max(1, delay.getInt());
                return;
            }
        }
    }
}
