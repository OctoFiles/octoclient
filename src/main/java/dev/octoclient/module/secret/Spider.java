package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Port of Meteor's Spider: climb walls you push against. */
public class Spider extends Module implements Module.Tickable {
    private final NumberSetting climbSpeed = new NumberSetting("climb-speed",
            "The speed you go up blocks.", 0.2, 0, 5, 0.01);

    public Spider() {
        super("Spider", "Allows you to climb walls like a spider.", 'S');
        addSetting(climbSpeed);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (!client.player.horizontalCollision) {
            return;
        }
        Vec3 delta = client.player.getDeltaMovement();
        if (delta.y >= 0.2) {
            return;
        }
        client.player.setDeltaMovement(delta.x, climbSpeed.getValue(), delta.z);
    }
}
