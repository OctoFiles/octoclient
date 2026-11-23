package dev.octoclient.module.secret;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Small hotbar helper for modules that need a specific item. It remembers the
 * slot the player had selected so the module can hand it back, and notices
 * when the player picks a slot himself so it never fights a manual choice.
 */
final class HotbarSwap {
    private int previous = -1;
    private int taken = -1;

    /** Makes sure the main hand holds a matching item. */
    boolean ensure(Minecraft client, Predicate<ItemStack> predicate) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        if (predicate.test(player.getMainHandItem())) {
            return true;
        }
        int slot = AuraUtil.slotOf(client, predicate);
        if (slot < 0) {
            return false;
        }
        if (previous < 0) {
            previous = player.getInventory().getSelectedSlot();
        }
        taken = slot;
        if (player.getInventory().getSelectedSlot() != slot) {
            player.getInventory().setSelectedSlot(slot);
        }
        return true;
    }

    boolean ensure(Minecraft client, Item item) {
        return ensure(client, stack -> !stack.isEmpty() && stack.is(item));
    }

    /** Switches to an explicit slot, remembering the slot to restore. */
    boolean select(Minecraft client, int slot) {
        LocalPlayer player = client.player;
        if (player == null || slot < 0 || slot > 8) {
            return false;
        }
        if (previous < 0) {
            previous = player.getInventory().getSelectedSlot();
        }
        taken = slot;
        if (player.getInventory().getSelectedSlot() != slot) {
            player.getInventory().setSelectedSlot(slot);
        }
        return true;
    }

    /** True while the module owns the selected slot. */
    boolean owns(Minecraft client) {
        return previous >= 0 && client.player != null
                && client.player.getInventory().getSelectedSlot() == taken;
    }

    /** The player switched slots manually; drop the restore target. */
    void respectManualChoice(Minecraft client) {
        if (previous >= 0 && client.player != null
                && client.player.getInventory().getSelectedSlot() != taken) {
            previous = -1;
            taken = -1;
        }
    }

    void release(Minecraft client, boolean restore) {
        if (restore && previous >= 0 && client.player != null) {
            client.player.getInventory().setSelectedSlot(previous);
        }
        previous = -1;
        taken = -1;
    }

    boolean swapped() {
        return previous >= 0;
    }
}
