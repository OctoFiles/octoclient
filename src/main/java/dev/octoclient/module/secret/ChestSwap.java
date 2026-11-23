package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Port of Meteor's ChestSwap: swaps between chestplate and elytra. */
public class ChestSwap extends Module {
    private final ModeSetting chestplate = new ModeSetting("chestplate",
            "Which type of chestplate to swap to.", "PreferNetherite",
            List.of("Diamond", "Netherite", "PreferDiamond", "PreferNetherite"));
    private final BooleanSetting stayOn = new BooleanSetting("stay-on",
            "Stays on and activates when you turn it off.", false);
    private final BooleanSetting closeInventory = new BooleanSetting("close-inventory",
            "Sends inventory close after swap.", true);

    /** Player inventory menu: 5 = helmet, 6 = chestplate, 7 = leggings, 8 = boots. */
    private static final int CHEST_MENU_SLOT = 6;

    public ChestSwap() {
        super("ChestSwap", "Automatically swaps between a chestplate and an elytra.", 'E');
        addSetting(chestplate);
        addSetting(stayOn);
        addSetting(closeInventory);
    }

    @Override
    protected void onEnable() {
        swap();
        if (!stayOn.isEnabled()) {
            setEnabled(false);
        }
    }

    @Override
    protected void onDisable() {
        if (stayOn.isEnabled()) {
            swap();
        }
    }

    public void swap() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        ItemStack current = client.player.getItemBySlot(EquipmentSlot.CHEST);
        if (current.has(DataComponents.GLIDER)) {
            equipChestplate(client);
        } else if (current.has(DataComponents.EQUIPPABLE)
                && current.get(DataComponents.EQUIPPABLE) != null
                && current.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.CHEST) {
            equipElytra(client);
        } else if (!equipChestplate(client)) {
            equipElytra(client);
        }
    }

    private boolean equipChestplate(Minecraft client) {
        int bestSlot = -1;
        boolean breakLoop = false;
        var items = client.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i).getItem();
            switch (chestplate.getValue()) {
                case "Diamond" -> {
                    if (item == Items.DIAMOND_CHESTPLATE) {
                        bestSlot = i;
                        breakLoop = true;
                    }
                }
                case "Netherite" -> {
                    if (item == Items.NETHERITE_CHESTPLATE) {
                        bestSlot = i;
                        breakLoop = true;
                    }
                }
                case "PreferDiamond" -> {
                    if (item == Items.DIAMOND_CHESTPLATE) {
                        bestSlot = i;
                        breakLoop = true;
                    } else if (item == Items.NETHERITE_CHESTPLATE) {
                        bestSlot = i;
                    }
                }
                case "PreferNetherite" -> {
                    if (item == Items.DIAMOND_CHESTPLATE) {
                        bestSlot = i;
                    } else if (item == Items.NETHERITE_CHESTPLATE) {
                        bestSlot = i;
                        breakLoop = true;
                    }
                }
                default -> {
                }
            }
            if (breakLoop) {
                break;
            }
        }
        if (bestSlot == -1) {
            return false;
        }
        equip(client, bestSlot);
        return true;
    }

    private void equipElytra(Minecraft client) {
        var items = client.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).has(DataComponents.GLIDER)) {
                equip(client, i);
                return;
            }
        }
    }

    /** Pick up the stack, place it into the armor slot, put the old one back. */
    private void equip(Minecraft client, int inventorySlot) {
        if (client.gameMode == null || client.player == null) {
            return;
        }
        int menuSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
        client.gameMode.handleContainerInput(0, menuSlot, 0, ContainerInput.PICKUP, client.player);
        client.gameMode.handleContainerInput(0, CHEST_MENU_SLOT, 0, ContainerInput.PICKUP, client.player);
        client.gameMode.handleContainerInput(0, menuSlot, 0, ContainerInput.PICKUP, client.player);
        if (closeInventory.isEnabled()) {
            client.player.closeContainer();
        }
    }
}
