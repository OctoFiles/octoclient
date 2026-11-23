package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Selects the best matching hotbar tool while the player is mining a block. */
public class AutoTool extends Module implements Module.Tickable {
    private final BooleanSetting restoreSlot =
            new BooleanSetting("Restore Slot", "Return to the previous slot when mining stops", true);
    private int originalSlot = -1;
    private boolean wasMining;

    public AutoTool() {
        super("Auto Tool", "Choose the fastest hotbar tool for the block you're mining", 'T');
        addSetting(restoreSlot);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || !client.options.keyAttack.isDown() || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            restore(client);
            return;
        }

        var state = client.level.getBlockState(hit.getBlockPos());
        var inventory = client.player.getInventory();
        int bestSlot = inventory.getSelectedSlot();
        float bestSpeed = inventory.getItem(bestSlot).getDestroySpeed(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = inventory.getItem(slot).getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        if (!wasMining) {
            originalSlot = inventory.getSelectedSlot();
        }
        wasMining = true;
        inventory.setSelectedSlot(bestSlot);
    }

    private void restore(Minecraft client) {
        if (wasMining && restoreSlot.isEnabled() && originalSlot >= 0 && client.player != null) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        wasMining = false;
        originalSlot = -1;
    }

    @Override
    protected void onDisable() {
        restore(Minecraft.getInstance());
    }
}
