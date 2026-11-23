package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * AutoWeapon: keeps the best weapon in the hotbar selected while a target is
 * close. It never attacks on its own.
 */
public class AutoWeapon extends Module implements Module.Tickable {
    private final ModeSetting weapon = new ModeSetting("weapon",
            "Which weapon type to prefer.", "Auto",
            List.of("Auto", "Sword", "Axe", "Mace", "Spear"));
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities trigger the swap.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "How close a target has to be.", 6, 1, 12, 0.5);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only swaps while you hold the attack key.", true);
    private final BooleanSetting keepTotem = new BooleanSetting("keep-totem",
            "Never swaps a totem out of your hand.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between checks.", 2, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never swaps for friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;
    private int idleTicks;

    public AutoWeapon() {
        super("AutoWeapon", "Selects the best weapon in your hotbar for the target.", 'A');
        addSetting(weapon);
        addSetting(targets);
        addSetting(range);
        addSetting(onlyOnClick);
        addSetting(keepTotem);
        addSetting(swapBack);
        addSetting(delay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        swap.release(Minecraft.getInstance(), swapBack.isEnabled());
        timer = 0;
        idleTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || !client.player.isAlive()) {
            return;
        }
        swap.respectManualChoice(client);
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            idle(client);
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        timer = Math.max(1, delay.getInt());
        LivingEntity target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (target == null) {
            idle(client);
            return;
        }
        idleTicks = 0;
        if (keepTotem.isEnabled() && AuraUtil.isTotem(client.player.getMainHandItem())) {
            return;
        }
        int slot = findSlot(client);
        if (slot >= 0) {
            swap.select(client, slot);
        }
    }

    private int findSlot(Minecraft client) {
        int best = -1;
        int bestScore = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (!matches(stack)) {
                continue;
            }
            int score = AuraUtil.weaponScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    private boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String path = AuraUtil.pathOf(stack);
        return switch (weapon.getValue()) {
            case "Sword" -> AuraUtil.isSword(stack);
            case "Axe" -> AuraUtil.isAxe(stack);
            case "Mace" -> path.equals("mace");
            case "Spear" -> AuraUtil.isSpear(stack);
            default -> AuraUtil.isWeapon(stack) || stack.is(Items.TRIDENT);
        };
    }

    private void idle(Minecraft client) {
        if (swap.swapped() && ++idleTicks > 60) {
            swap.release(client, swapBack.isEnabled());
            idleTicks = 0;
        }
    }
}
