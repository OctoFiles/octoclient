package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.render.ShieldCosmetics;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class SideShield extends Module {
    private static SideShield instance;

    private final NumberSetting offsetX = new NumberSetting("Side Offset", "Move the first-person shield sideways", 0.18, -0.5, 0.5, 0.01);
    private final NumberSetting offsetY = new NumberSetting("Vertical Offset", "Move the first-person shield up or down", 0, -0.5, 0.5, 0.01);
    private final NumberSetting sideAngle =
            new NumberSetting("Side Angle", "Angle the shield away from a front-facing pose", 35.0, -80.0, 80.0, 1.0);
    private final NumberSetting blockingScale =
            new NumberSetting("Blocking Scale", "Shield size while blocking", 0.72, 0.3, 1.0, 0.02);
    private final BooleanSetting smallBlocking =
            new BooleanSetting("Small Blocking", "Shrink the shield while it is raised", true);

    public SideShield() {
        super("Side Shield", "Show the first-person shield at the side", 'S');
        addSetting(offsetX);
        addSetting(offsetY);
        addSetting(sideAngle);
        addSetting(blockingScale);
        addSetting(smallBlocking);
        // Shields belong beside the player in first person, not centred in the
        // camera. Existing saved configs can still turn the module off again.
        setEnabled(true);
        instance = this;
    }

    public static boolean shouldApply(ItemStack stack, ItemDisplayContext context) {
        return instance != null && instance.isEnabled() && !stack.isEmpty()
                && stack.getItem() instanceof ShieldItem && ItemModel.firstPerson(context);
    }

    public static void transform(PoseStack pose, LivingEntity entity) {
        pose.translate(instance.offsetX.getSnapped(), instance.offsetY.getSnapped(), 0);
        float angle = instance.sideAngle.getFloat();
        // Round catalog shields are authored on the opposite face; mirror only the
        // yaw so the design turns toward the player while the side offset stays.
        if (ShieldCosmetics.shouldMirrorFirstPersonSideAngle()) {
            angle = -angle;
        }
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        if (instance.smallBlocking.isEnabled() && entity.isBlocking()) {
            float scale = instance.blockingScale.getFloat();
            pose.scale(scale, scale, scale);
        }
    }
}
