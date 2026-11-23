package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Port of Meteor's Scaffold: places blocks under your feet while walking. */
public class Scaffold extends Module implements Module.Tickable {
    private final BooleanSetting fastTower = new BooleanSetting("fast-tower",
            "Towers up quickly while holding jump.", false);
    private final NumberSetting towerSpeed = new NumberSetting("tower-speed",
            "The speed of fast tower.", 5, 1, 20, 1);
    private final BooleanSetting whileMoving = new BooleanSetting("while-moving",
            "Places blocks while moving.", true);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only places while holding right click.", false);
    private final BooleanSetting swing = new BooleanSetting("swing",
            "Swings your hand.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to a block automatically.", true);
    private final NumberSetting radius = new NumberSetting("radius",
            "The radius to place blocks in.", 3, 1, 6, 1);
    private final NumberSetting blocksPerTick = new NumberSetting("blocks-per-tick",
            "The maximum blocks to place per tick.", 1, 1, 5, 1);

    private int towerTicks;

    public Scaffold() {
        super("Scaffold", "Places blocks under your feet.", 'S');
        addSetting(fastTower);
        addSetting(towerSpeed);
        addSetting(whileMoving);
        addSetting(onlyOnClick);
        addSetting(swing);
        addSetting(autoSwitch);
        addSetting(radius);
        addSetting(blocksPerTick);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
            return;
        }
        if (onlyOnClick.isEnabled() && !client.options.keyUse.isDown()) {
            return;
        }
        if (!whileMoving.isEnabled() && isMoving(client)) {
            return;
        }
        int placed = 0;
        for (int dx = -1; dx <= 1 && placed < blocksPerTick.getInt(); dx++) {
            for (int dz = -1; dz <= 1 && placed < blocksPerTick.getInt(); dz++) {
                BlockPos feet = client.player.blockPosition().offset(dx, 0, dz);
                BlockPos target = feet.below();
                if (!client.level.getBlockState(target).canBeReplaced()) {
                    continue;
                }
                BlockPos support = findSupport(client, target);
                if (support == null) {
                    continue;
                }
                int slot = findBlockSlot(client);
                if (slot < 0) {
                    return;
                }
                if (autoSwitch.isEnabled()
                        && slot != client.player.getInventory().getSelectedSlot()) {
                    client.player.getInventory().setSelectedSlot(slot);
                }
                Direction face = Direction.UP;
                for (Direction direction : Direction.values()) {
                    if (support.relative(direction).equals(target)) {
                        face = direction;
                        break;
                    }
                }
                Vec3 hitVec = Vec3.atCenterOf(support).add(
                        face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
                BlockHitResult hit = new BlockHitResult(hitVec, face, support, false);
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
                if (swing.isEnabled()) {
                    client.player.swing(InteractionHand.MAIN_HAND);
                }
                placed++;
            }
        }
        if (fastTower.isEnabled() && client.options.keyJump.isDown() && client.player.onGround()
                && ++towerTicks >= towerSpeed.getInt()) {
            towerTicks = 0;
            client.player.jumpFromGround();
        }
    }

    private static boolean isMoving(Minecraft client) {
        Vec3 delta = client.player.getDeltaMovement();
        return delta.x * delta.x + delta.z * delta.z > 0.0001;
    }

    private static BlockPos findSupport(Minecraft client, BlockPos target) {
        for (Direction direction : Direction.values()) {
            BlockPos support = target.relative(direction);
            BlockState state = client.level.getBlockState(support);
            if (!state.canBeReplaced() && !state.isAir()) {
                return support;
            }
        }
        return null;
    }

    private static int findBlockSlot(Minecraft client) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem block
                    && !block.getBlock().defaultBlockState().isAir()) {
                return slot;
            }
        }
        return -1;
    }
}
