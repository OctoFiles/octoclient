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

/** Builds an obsidian roof above the target so it cannot jump out. */
public class CrystalTrap extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to trap.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum placement distance.", 4.5, 0, 6, 0.1);
    private final NumberSetting blocks = new NumberSetting("blocks",
            "How many obsidian layers to place above the target.", 1, 1, 3, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards each block.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to obsidian automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between blocks.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never traps friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;
    private int layer;

    public CrystalTrap() {
        super("CrystalTrap", "Places an obsidian roof above your target.", 'C');
        addSetting(targets);
        addSetting(range);
        addSetting(blocks);
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
        layer = 0;
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
            layer = 0;
            return;
        }
        BlockPos head = target.blockPosition().above(2 + layer);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos pos = head.above(dy);
            if (!AuraUtil.replaceable(client.level, pos)) {
                continue;
            }
            AnchorUtil.Spot spot = new AnchorUtil.Spot(pos.immutable(),
                    AnchorUtil.supportFace(client.level, pos));
            if (spot.face() == null) {
                continue;
            }
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, spot.pos());
            }
            if (AnchorUtil.placeWith(client, swap, Items.OBSIDIAN, spot)) {
                layer = (layer + 1) % Math.max(1, blocks.getInt());
                timer = Math.max(1, delay.getInt());
                return;
            }
        }
    }
}
