package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/** Port of Meteor's AutoArmor (equips the best protection in the inventory). */
public class AutoArmor extends Module implements Module.Tickable {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final ModeSetting preferredProtection = new ModeSetting("preferred-protection",
            "The type of protection to prioritize.", "Protection",
            List.of("Protection", "Blast", "Fire", "Projectile", "Feather"));
    private final NumberSetting swapDelay = new NumberSetting("swap-delay",
            "The delay between swapping items, in ticks.", 1, 0, 20, 1);
    private final BooleanSetting blastProtLeggings = new BooleanSetting("blast-prot-leggings",
            "Prefers blast protection on leggings.", true);
    private final BooleanSetting antiBreak = new BooleanSetting("anti-break",
            "Swaps out armor that is about to break.", false);
    private final BooleanSetting ignoreElytra = new BooleanSetting("ignore-elytra",
            "Does not replace an equipped elytra.", true);

    private int ticks;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the best armor.", 'A');
        addSetting(preferredProtection);
        addSetting(swapDelay);
        addSetting(blastProtLeggings);
        addSetting(antiBreak);
        addSetting(ignoreElytra);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (ticks < swapDelay.getInt()) {
            ticks++;
            return;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack equipped = client.player.getItemBySlot(slot);
            if (ignoreElytra.isEnabled() && slot == EquipmentSlot.CHEST
                    && equipped.getItem() == Items.ELYTRA) {
                continue;
            }
            double current = antiBreak.isEnabled() && !equipped.isEmpty() && equipped.getMaxDamage() > 0
                    && equipped.getMaxDamage() - equipped.getDamageValue() <= 5 ? -1 : score(equipped, slot);
            int bestSlot = -1;
            double best = current;
            for (int inv = 0; inv < 36; inv++) {
                ItemStack stack = client.player.getInventory().getItem(inv);
                if (stack.isEmpty() || armorSlotOf(stack) != slot) {
                    continue;
                }
                double value = score(stack, slot);
                if (value > best) {
                    best = value;
                    bestSlot = inv;
                }
            }
            if (bestSlot >= 0) {
                client.gameMode.handleContainerInput(0, AutoTotem.menuSlot(bestSlot), 0,
                        ContainerInput.QUICK_MOVE, client.player);
                ticks = 0;
                return;
            }
        }
    }

    private double score(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return 0;
        }
        double armor = 0;
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            armor = modifiers.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0, slot);
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY);
        double protection = 0;
        var preferred = switch (preferredProtection.getValue()) {
            case "Blast" -> Enchantments.BLAST_PROTECTION;
            case "Fire" -> Enchantments.FIRE_PROTECTION;
            case "Projectile" -> Enchantments.PROJECTILE_PROTECTION;
            case "Feather" -> Enchantments.FEATHER_FALLING;
            default -> Enchantments.PROTECTION;
        };
        for (var entry : enchantments.entrySet()) {
            var key = entry.getKey().unwrapKey().orElse(null);
            if (key == null) {
                continue;
            }
            if (key.equals(preferred)) {
                protection += entry.getIntValue() * 2.0;
            } else if (key.equals(Enchantments.PROTECTION)) {
                protection += entry.getIntValue();
            }
        }
        double value = armor + protection;
        if (blastProtLeggings.isEnabled() && slot == EquipmentSlot.LEGS) {
            for (var entry : enchantments.entrySet()) {
                var key = entry.getKey().unwrapKey().orElse(null);
                if (key != null && key.equals(Enchantments.BLAST_PROTECTION)) {
                    value += entry.getIntValue() * 2.0;
                }
            }
        }
        return value;
    }

    private static EquipmentSlot armorSlotOf(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.endsWith("_helmet") || path.equals("turtle_helmet")) {
            return EquipmentSlot.HEAD;
        }
        if (path.endsWith("_chestplate")) {
            return EquipmentSlot.CHEST;
        }
        if (path.endsWith("_leggings")) {
            return EquipmentSlot.LEGS;
        }
        if (path.endsWith("_boots")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }
}
