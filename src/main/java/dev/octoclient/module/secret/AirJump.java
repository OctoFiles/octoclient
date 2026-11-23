package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

/** Port of Meteor's AirJump: jumping again while in the air. */
public class AirJump extends Module implements Module.Tickable {
    private final BooleanSetting maintainLevel = new BooleanSetting("maintain-level",
            "Maintains your current Y level when holding the jump key.", false);

    private int level;
    private boolean jumpWasDown;
    private boolean shiftWasDown;

    public AirJump() {
        super("AirJump", "Lets you jump in the air.", 'J');
        addSetting(maintainLevel);
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            level = client.player.blockPosition().getY();
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        boolean jumpDown = client.options.keyJump.isDown();
        boolean shiftDown = client.options.keyShift.isDown();

        if (shiftDown && !shiftWasDown) {
            level--;
        }
        shiftWasDown = shiftDown;

        if (client.player.onGround()) {
            jumpWasDown = jumpDown;
            return;
        }

        if (jumpDown && !jumpWasDown) {
            level = client.player.blockPosition().getY();
            client.player.jumpFromGround();
        } else if (maintainLevel.isEnabled() && jumpDown
                && client.player.blockPosition().getY() == level) {
            client.player.jumpFromGround();
        }
        jumpWasDown = jumpDown;
    }
}
