package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * FastBow: re-draws and releases a bow as soon as the configured charge is
 * reached, which turns slow full draws into a fast fire rate. Crossbows are
 * released as soon as they are charged.
 *
 * <p>Arrows still need the usual charge for damage; lowering charge-ticks makes
 * the bow fire faster at the cost of arrow speed and damage.
 */
public class FastBow extends Module implements Module.Tickable {
    private final NumberSetting chargeTicks = new NumberSetting("charge-ticks",
            "Ticks a bow is drawn before it is released.", 3, 0, 20, 1);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between shots.", 1, 0, 20, 1);
    private final NumberSetting range = new NumberSetting("range",
            "Target search distance when require-target is on.", 16, 2, 32, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Leads the target by this many ticks.", 4, 0, 20, 1);
    private final BooleanSetting requireTarget = new BooleanSetting("require-target",
            "Only shoots while a target is in range.", true);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only draws the bow while you hold the use key.", true);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the target.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to a bow or crossbow automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never shoots at friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;
    private int idleTicks;
    private boolean drawing;

    public FastBow() {
        super("FastBow", "Fires a bow or crossbow as soon as it is charged.", 'F');
        addSetting(chargeTicks);
        addSetting(delay);
        addSetting(range);
        addSetting(predictTicks);
        addSetting(requireTarget);
        addSetting(onlyOnClick);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        release(client);
        swap.release(client, swapBack.isEnabled());
        timer = 0;
        idleTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || !client.player.isAlive()) {
            release(client);
            return;
        }
        swap.respectManualChoice(client);
        if (onlyOnClick.isEnabled() && !client.options.keyUse.isDown()) {
            release(client);
            idle(client);
            return;
        }
        LivingEntity target = null;
        if (requireTarget.isEnabled()) {
            target = AuraUtil.target(client, range.getValue(), "Players",
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
            if (target == null) {
                release(client);
                idle(client);
                return;
            }
            idleTicks = 0;
        }
        if (target != null && rotate.isEnabled()) {
            AuraUtil.lookAt(client.player, AuraUtil.predictedEye(target, predictTicks.getInt()));
        }
        if (!isBow(client.player.getMainHandItem()) && autoSwitch.isEnabled()) {
            swap.ensure(client, this::isBow);
        }
        if (!isBow(client.player.getMainHandItem())) {
            release(client);
            return;
        }
        if (client.player.isUsingItem()) {
            ItemStack using = client.player.getUseItem();
            boolean ready;
            if (using.is(Items.CROSSBOW)) {
                ready = CrossbowItem.isCharged(using);
            } else {
                ready = client.player.getTicksUsingItem() >= chargeTicks.getInt();
            }
            if (ready) {
                client.gameMode.releaseUsingItem(client.player);
                client.player.swing(InteractionHand.MAIN_HAND);
                drawing = false;
                timer = Math.max(1, delay.getInt());
            }
            return;
        }
        drawing = false;
        if (timer > 0) {
            timer--;
            return;
        }
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        drawing = true;
    }

    private boolean isBow(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(Items.BOW) || stack.is(Items.CROSSBOW));
    }

    /** Cancels a draw this module started, so it does not fire on its own. */
    private void release(Minecraft client) {
        if (drawing && client.player != null && client.player.isUsingItem()) {
            client.gameMode.releaseUsingItem(client.player);
        }
        drawing = false;
    }

    private void idle(Minecraft client) {
        if (swap.swapped() && ++idleTicks > 60) {
            swap.release(client, swapBack.isEnabled());
            idleTicks = 0;
        }
    }
}
