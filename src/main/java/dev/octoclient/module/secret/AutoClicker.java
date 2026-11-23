package dev.octoclient.module.secret;

import com.mojang.blaze3d.platform.InputConstants;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Port of Meteor's AutoClicker (left/right click with Press/Hold modes). */
public class AutoClicker extends Module implements Module.Tickable {
    private final BooleanSetting whileInScreens = new BooleanSetting("while-in-screens",
            "Clicks while a screen is open.", true);
    private final ModeSetting modeLeft = new ModeSetting("mode-left",
            "The clicking mode for the left button.", "Press", List.of("Press", "Hold"));
    private final NumberSetting delayLeft = new NumberSetting("delay-left",
            "The delay between left clicks, in ticks.", 2, 0, 20, 1);
    private final ModeSetting modeRight = new ModeSetting("mode-right",
            "The clicking mode for the right button.", "Press", List.of("Press", "Hold"));
    private final NumberSetting delayRight = new NumberSetting("delay-right",
            "The delay between right clicks, in ticks.", 2, 0, 20, 1);

    private int leftTimer;
    private int rightTimer;

    public AutoClicker() {
        super("AutoClicker", "Clicks automatically while the buttons are held.", 'A');
        addSetting(whileInScreens);
        addSetting(modeLeft);
        addSetting(delayLeft);
        addSetting(modeRight);
        addSetting(delayRight);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.options == null) {
            return;
        }
        if (!whileInScreens.isEnabled() && client.gui.screen() != null) {
            return;
        }
        leftTimer = Math.max(0, leftTimer - 1);
        rightTimer = Math.max(0, rightTimer - 1);

        if (client.options.keyAttack.isDown() && leftTimer <= 0) {
            KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(0));
            leftTimer = delayLeft.getInt();
        }
        if (client.options.keyUse.isDown() && rightTimer <= 0) {
            KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(1));
            rightTimer = delayRight.getInt();
        }
    }
}
