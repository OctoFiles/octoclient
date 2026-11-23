package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Port of Meteor's HighJump: multiplies the jump velocity. */
public class HighJump extends Module implements Module.Tickable {
    private final NumberSetting jumpMultiplier = new NumberSetting("jump-multiplier",
            "Jump height multiplier.", 1, 0, 10, 0.1);

    private boolean wasOnGround;

    public HighJump() {
        super("HighJump", "Makes you jump higher than normal.", 'J');
        addSetting(jumpMultiplier);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        boolean onGround = client.player.onGround();
        Vec3 delta = client.player.getDeltaMovement();
        if (wasOnGround && !onGround && client.options.keyJump.isDown() && delta.y > 0) {
            client.player.setDeltaMovement(delta.x, delta.y * jumpMultiplier.getValue(), delta.z);
        }
        wasOnGround = onGround;
    }
}
