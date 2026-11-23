package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;

/** Automatically uses held food when the configured hunger threshold is reached. */
public class AutoEat extends Module implements Module.Tickable {
    private final NumberSetting hungerThreshold =
            new NumberSetting("Hunger Threshold", "Start eating at or below this hunger level", 14, 1, 19, 1);
    private final BooleanSetting allowOffhand =
            new BooleanSetting("Allow Offhand", "Use food held in the offhand if needed", true);
    private final BooleanSetting eatWhenFull =
            new BooleanSetting("Eat When Full", "Allow food with the always-edible property", false);

    public AutoEat() {
        super("Auto Eat", "Eat food automatically when hunger is low", 'E');
        addSetting(hungerThreshold);
        addSetting(allowOffhand);
        addSetting(eatWhenFull);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || client.player.isUsingItem() || client.player.getFoodData().getFoodLevel() > hungerThreshold.getInt()
                || !client.player.canEat(eatWhenFull.isEnabled())) {
            return;
        }
        if (client.player.getMainHandItem().get(DataComponents.FOOD) != null) {
            client.player.startUsingItem(InteractionHand.MAIN_HAND);
        } else if (allowOffhand.isEnabled()
                && client.player.getOffhandItem().get(DataComponents.FOOD) != null) {
            client.player.startUsingItem(InteractionHand.OFF_HAND);
        }
    }
}
