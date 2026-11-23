package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Port of Meteor's Nuker (sphere/cube selection, flatten/all modes). */
public class Nuker extends Module implements Module.Tickable {
    private final ModeSetting shape = new ModeSetting("shape", "The shape of the area.",
            "Sphere", List.of("Sphere", "Cube"));
    private final ModeSetting mode = new ModeSetting("mode", "What blocks to break.",
            "Flatten", List.of("Flatten", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "The radius to break blocks in.", 4.0, 0, 6, 0.1);
    private final NumberSetting up = new NumberSetting("up", "Blocks above you.", 1, 0, 6, 1);
    private final NumberSetting down = new NumberSetting("down", "Blocks below you.", 1, 0, 6, 1);
    private final NumberSetting left = new NumberSetting("left", "Blocks to the left.", 1, 0, 6, 1);
    private final NumberSetting right = new NumberSetting("right", "Blocks to the right.", 1, 0, 6, 1);
    private final NumberSetting forward = new NumberSetting("forward", "Blocks in front.", 1, 0, 6, 1);
    private final NumberSetting back = new NumberSetting("back", "Blocks behind.", 1, 0, 6, 1);
    private final NumberSetting wallsRange = new NumberSetting("walls-range",
            "The range through walls.", 4.0, 0, 6, 0.1);
    private final NumberSetting delay = new NumberSetting("delay",
            "The delay between breaking blocks, in ticks.", 0, 0, 20, 1);
    private final NumberSetting maxBlocksPerTick = new NumberSetting("max-blocks-per-tick",
            "The maximum blocks to break per tick.", 1, 1, 10, 1);
    private final ModeSetting sortMode = new ModeSetting("sort-mode",
            "How blocks are sorted.", "Closest", List.of("Closest", "Furthest"));
    private final BooleanSetting packetMine = new BooleanSetting("packet-mine",
            "Mines blocks with packets.", false);
    private final BooleanSetting onlySuitableTools = new BooleanSetting("only-suitable-tools",
            "Only breaks blocks you have the right tool for.", false);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the block being broken.", true);
    private final BooleanSetting swing = new BooleanSetting("swing",
            "Swings your hand.", true);

    private int timer;

    public Nuker() {
        super("Nuker", "Breaks blocks around you.", 'N');
        addSetting(shape);
        addSetting(mode);
        addSetting(range);
        addSetting(up);
        addSetting(down);
        addSetting(left);
        addSetting(right);
        addSetting(forward);
        addSetting(back);
        addSetting(wallsRange);
        addSetting(delay);
        addSetting(maxBlocksPerTick);
        addSetting(sortMode);
        addSetting(packetMine);
        addSetting(onlySuitableTools);
        addSetting(rotate);
        addSetting(swing);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        timer = delay.getInt();
        List<BlockPos> blocks = findBlocks(client);
        if (blocks.isEmpty()) {
            return;
        }
        int limit = Math.min(maxBlocksPerTick.getInt(), blocks.size());
        for (int i = 0; i < limit; i++) {
            BlockPos pos = blocks.get(i);
            if (rotate.isEnabled()) {
                lookAt(client, pos);
            }
            client.gameMode.continueDestroyBlock(pos, Direction.UP);
            if (swing.isEnabled()) {
                client.player.swing(InteractionHand.MAIN_HAND);
            }
            client.gameMode.destroyBlock(pos);
        }
    }

    private List<BlockPos> findBlocks(Minecraft client) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos center = client.player.blockPosition();
        int r = (int) Math.ceil(range.getValue());
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r, center.getY() + r, center.getZ() + r)) {
            double distance = Math.sqrt(client.player.distanceToSqr(pos.getX() + 0.5,
                    pos.getY() + 0.5, pos.getZ() + 0.5));
            if (distance > range.getValue()) {
                continue;
            }
            if (shape.getValue().equals("Cube") && !inBox(client, pos, center)) {
                continue;
            }
            if (mode.getValue().equals("Flatten")
                    && (pos.getY() > center.getY() + up.getInt() || pos.getY() < center.getY() - down.getInt())) {
                continue;
            }
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0) {
                continue;
            }
            found.add(pos.immutable());
        }
        boolean closest = sortMode.getValue().equals("Closest");
        found.sort(Comparator.comparingDouble(pos -> (closest ? 1 : -1)
                * client.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        return found;
    }

    private boolean inBox(Minecraft client, BlockPos pos, BlockPos center) {
        return Math.abs(pos.getY() - center.getY()) <= up.getInt()
                && Math.abs(pos.getY() - center.getY()) <= down.getInt()
                && Math.abs(pos.getX() - center.getX()) <= Math.max(left.getInt(), right.getInt())
                && Math.abs(pos.getZ() - center.getZ()) <= Math.max(forward.getInt(), back.getInt());
    }

    private static void lookAt(Minecraft client, BlockPos pos) {
        double dx = pos.getX() + 0.5 - client.player.getX();
        double dy = pos.getY() + 0.5 - client.player.getEyePosition().y;
        double dz = pos.getZ() + 0.5 - client.player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        client.player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horizontal)));
    }
}
