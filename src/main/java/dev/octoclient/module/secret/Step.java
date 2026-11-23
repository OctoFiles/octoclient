package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Port of Meteor's Step: raises the vanilla step-height attribute. */
public class Step extends Module implements Module.Tickable {
    private final NumberSetting height = new NumberSetting("height",
            "Step height.", 1.25, 0.0, 3.0, 0.05);
    private final ModeSetting activeWhen = new ModeSetting("active-when",
            "Step is active when you meet these requirements.", "Always",
            List.of("Always", "Sneaking", "NotSneaking"));
    private final BooleanSetting safeStep = new BooleanSetting("safe-step",
            "Doesn't let you step out of a hole if you are low on health.", false);
    private final NumberSetting stepHealth = new NumberSetting("step-health",
            "The health you stop being able to step at.", 5, 1, 36, 1);

    private float prevStepHeight = -1;

    public Step() {
        super("Step", "Allows you to walk up full blocks instantly.", 'S');
        addSetting(height);
        addSetting(activeWhen);
        addSetting(safeStep);
        addSetting(stepHealth);
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            prevStepHeight = client.player.getAttributeValue(Attributes.STEP_HEIGHT) == 0.6
                    ? 0.6f : (float) client.player.getAttributeBaseValue(Attributes.STEP_HEIGHT);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || prevStepHeight < 0) {
            return;
        }
        AttributeInstance attribute = client.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute != null) {
            attribute.setBaseValue(prevStepHeight);
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) {
            return;
        }
        String mode = activeWhen.getValue();
        boolean work = mode.equals("Always")
                || (mode.equals("Sneaking") && client.player.isShiftKeyDown())
                || (mode.equals("NotSneaking") && !client.player.isShiftKeyDown());
        AttributeInstance attribute = client.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute == null) {
            return;
        }
        if (work && isSafe(client)) {
            attribute.setBaseValue(height.getValue());
        } else if (prevStepHeight >= 0) {
            attribute.setBaseValue(prevStepHeight);
        }
    }

    private boolean isSafe(Minecraft client) {
        if (!safeStep.isEnabled()) {
            return true;
        }
        float health = client.player.getHealth() + client.player.getAbsorptionAmount();
        return health > stepHealth.getValue();
    }
}
