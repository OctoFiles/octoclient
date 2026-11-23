package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Port of Meteor's SafeWalk: stops you at block edges. */
public class SafeWalk extends Module implements Module.Tickable {
    private static SafeWalk instance;

    private final NumberSetting fallDistance = new NumberSetting("minimum-fall-distance",
            "The minimum number of blocks you are expected to fall before the module activates.", 1, 1, 255, 1);
    private final BooleanSetting sneak = new BooleanSetting("sneak",
            "Sneak when approaching edge of block.", false);
    private final BooleanSetting safeSneak = new BooleanSetting("safe-sneak",
            "Prevent you from falling if sneak doesn't trigger correctly.", true);
    private final BooleanSetting sneakSprint = new BooleanSetting("sneak-on-sprint",
            "Sneak even when sprinting at the block edge.", true);
    private final NumberSetting edgeDistance = new NumberSetting("edge-distance",
            "Distance offset before reaching an edge.", 0.30, 0.0, 0.30, 0.01);
    private final BooleanSetting render = new BooleanSetting("render",
            "Render edge distance helper.", false);
    private final BooleanSetting renderPlayerBox = new BooleanSetting("render-player-box",
            "Render player box helper.", false);

    public SafeWalk() {
        super("SafeWalk", "Prevents you from walking off blocks.", 'W');
        addSetting(fallDistance);
        addSetting(sneak);
        addSetting(safeSneak);
        addSetting(sneakSprint);
        addSetting(edgeDistance);
        addSetting(render);
        addSetting(renderPlayerBox);
        instance = this;
    }

    @Override
    public void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || !sneak.isEnabled()) {
            return;
        }
        if (!isSprinting(client) && player.onGround() && isCloseToEdge(client, player)
                && player instanceof net.minecraft.client.player.LocalPlayer local) {
            Input keys = local.input.keyPresses;
            if (!keys.shift()) {
                local.input.keyPresses = new Input(keys.forward(), keys.backward(),
                        keys.left(), keys.right(), keys.jump(), true, keys.sprint());
            }
        }
    }

    /** Called from the Player mixin: whether the player should be kept at the ledge. */
    public static boolean shouldClip(Minecraft client) {
        SafeWalk module = instance;
        if (module == null || !module.isEnabled() || client.player == null || client.level == null) {
            return false;
        }
        Player player = client.player;
        if (!player.onGround()) {
            return false;
        }
        if (!module.isSafeDrop(client, player)) {
            return false;
        }
        if (module.sneak.isEnabled()) {
            boolean sprinting = module.isSprinting(client);
            if (sprinting) {
                return false;
            }
            if (module.isCloseToEdge(client, player)) {
                return false;
            }
            return module.safeSneak.isEnabled();
        }
        return !player.isShiftKeyDown();
    }

    private boolean isSprinting(Minecraft client) {
        return !sneakSprint.isEnabled() && client.options.keySprint.isDown();
    }

    private boolean isCloseToEdge(Minecraft client, Player player) {
        AABB box = player.getBoundingBox()
                .expandTowards(0, -player.maxUpStep(), 0)
                .inflate(-edgeDistance.getValue(), 0, -edgeDistance.getValue());
        return client.level.noCollision(player, box) && player.onGround();
    }

    /** Mirrors Meteor's fall damage check: stay clipped while the drop is too small. */
    private boolean isSafeDrop(Minecraft client, Player player) {
        if (fallDistance.getValue() <= 1) {
            return true;
        }
        BlockHitResult hit = client.level.clip(new ClipContext(player.position(),
                new Vec3(player.getX(), client.level.getMinY(), player.getZ()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        double drop = player.getY() - hit.getBlockPos().above().getY();
        return drop >= fallDistance.getValue();
    }
}
