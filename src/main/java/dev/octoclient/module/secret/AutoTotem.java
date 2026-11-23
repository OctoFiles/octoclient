package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Port of Meteor's AutoTotem (Smart/Strict, delay in ticks). */
public class AutoTotem extends Module implements Module.Tickable {
    private final ModeSetting mode = new ModeSetting("mode",
            "When to equip a totem.", "Smart", List.of("Smart", "Strict"));
    private final NumberSetting delay = new NumberSetting("delay",
            "The delay between actions, in ticks.", 0, 0, 20, 1);
    private final NumberSetting health = new NumberSetting("health",
            "The health to equip a totem at.", 10, 0, 20, 1);
    private final BooleanSetting elytra = new BooleanSetting("elytra",
            "Equips a totem while flying with an elytra.", true);
    private final BooleanSetting fall = new BooleanSetting("fall",
            "Counts fall damage when checking health.", true);
    private final BooleanSetting explosion = new BooleanSetting("explosion",
            "Counts explosion damage when checking health.", true);

    private int ticks;

    public AutoTotem() {
        super("AutoTotem", "Automatically equips a totem in your offhand.", 'T');
        addSetting(mode);
        addSetting(delay);
        addSetting(health);
        addSetting(elytra);
        addSetting(fall);
        addSetting(explosion);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int slot = findTotem(client);
        if (slot < 0) {
            ticks = 0;
            return;
        }
        if (ticks >= delay.getInt()) {
            float reductions = 0;
            if (fall.isEnabled()) {
                reductions += Math.max(0, client.player.fallDistance - 3.0f);
            }
            boolean low = client.player.getHealth() + client.player.getAbsorptionAmount() - reductions
                    <= health.getValue();
            boolean flying = elytra.isEnabled()
                    && client.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
                    && client.player.isFallFlying();
            boolean locked = mode.getValue().equals("Strict") || low || flying;
            if (locked && client.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
                client.gameMode.handleContainerInput(0, menuSlot(slot), 40,
                        ContainerInput.SWAP, client.player);
            }
            ticks = 0;
            return;
        }
        ticks++;
    }

    private static int findTotem(Minecraft client) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return slot;
            }
        }
        return -1;
    }

    /** Maps a player inventory index to the slot id used by the player's container menu. */
    static int menuSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }
}
