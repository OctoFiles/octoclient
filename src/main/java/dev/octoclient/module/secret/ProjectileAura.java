package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * ProjectileAura: throws snowballs, eggs, pearls, XP bottles or splash potions
 * at the target, leading the shot by the configured number of ticks.
 */
public class ProjectileAura extends Module implements Module.Tickable {
    private final ModeSetting item = new ModeSetting("item",
            "Which projectile to throw.", "Auto",
            List.of("Auto", "Snowball", "Egg", "Pearl", "XP", "Potion"));
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to throw at.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum throw distance.", 8, 2, 24, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Leads the target by this many ticks.", 4, 0, 20, 1);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between throws.", 4, 0, 20, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the predicted position.", true);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only throws while you hold the attack key.", false);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to a projectile automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never throws at friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;
    private int idleTicks;

    public ProjectileAura() {
        super("ProjectileAura", "Throws projectiles at nearby targets automatically.", 'P');
        addSetting(item);
        addSetting(targets);
        addSetting(range);
        addSetting(predictTicks);
        addSetting(delay);
        addSetting(rotate);
        addSetting(onlyOnClick);
        addSetting(autoSwitch);
        addSetting(swapBack);
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
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || !client.player.isAlive()) {
            return;
        }
        swap.respectManualChoice(client);
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            idle(client);
            return;
        }
        LivingEntity target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (target == null) {
            idle(client);
            return;
        }
        idleTicks = 0;
        Vec3 aim = AuraUtil.predictedEye(target, predictTicks.getInt());
        if (rotate.isEnabled()) {
            AuraUtil.lookAt(client.player, aim);
        }
        if (timer > 0) {
            timer--;
            return;
        }
        if (!matches(client.player.getMainHandItem()) && autoSwitch.isEnabled()) {
            swap.ensure(client, this::matches);
        }
        if (!matches(client.player.getMainHandItem())) {
            return;
        }
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        client.player.swing(InteractionHand.MAIN_HAND);
        timer = Math.max(1, delay.getInt());
    }

    private boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String path = AuraUtil.pathOf(stack);
        return switch (item.getValue()) {
            case "Snowball" -> path.equals("snowball");
            case "Egg" -> path.equals("egg");
            case "Pearl" -> path.equals("ender_pearl");
            case "XP" -> path.equals("experience_bottle");
            case "Potion" -> path.equals("splash_potion") || path.equals("lingering_potion");
            default -> path.equals("snowball") || path.equals("egg")
                    || path.equals("ender_pearl") || path.equals("experience_bottle")
                    || path.equals("splash_potion") || path.equals("lingering_potion");
        };
    }

    private void idle(Minecraft client) {
        if (swap.swapped() && ++idleTicks > 60) {
            swap.release(client, swapBack.isEnabled());
            idleTicks = 0;
        }
    }
}
