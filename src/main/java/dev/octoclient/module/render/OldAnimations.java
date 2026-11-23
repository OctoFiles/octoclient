package dev.octoclient.module.render;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;

/** Individually configurable legacy first-person, player-model and HUD animations. */
public class OldAnimations extends Module {
    private static OldAnimations instance;
    private static final Item[] LAST_ITEMS = new Item[2];
    private static final long[] SWITCH_STARTS = new long[2];

    private final BooleanSetting blockHitting =
            new BooleanSetting("Block Hitting", "Use the legacy sword blocking pose", true);
    private final BooleanSetting oldItemSwitch =
            new BooleanSetting("Old Item Switch", "Animate held-item changes with a legacy dip", true);
    private final BooleanSetting itemUsageSwinging =
            new BooleanSetting("Item Usage Swinging", "Add the legacy swing while using an item", true);
    private final BooleanSetting oldSneakAnimation =
            new BooleanSetting("Old Sneak Animation", "Use the flatter legacy crouch pose", true);
    private final BooleanSetting armorHurtTint =
            new BooleanSetting("Armor Hurt Tint", "Keep armor red-tinted when the player is hurt", true);
    private final BooleanSetting rightClickFix =
            new BooleanSetting("Right Click Fix", "Keep legacy blocking responsive to right-click input", true);
    private final BooleanSetting oldDamageTilt =
            new BooleanSetting("Old Damage Tilt", "Keep the legacy hurt-camera tilt", true);
    private final BooleanSetting oldHeartAnimation =
            new BooleanSetting("Old Heart Animation", "Use the classic health-heart flash animation", true);

    public OldAnimations() {
        super("Old Animations", "Legacy first-person item and damage animations", 'A');
        addSetting(blockHitting);
        addSetting(oldItemSwitch);
        addSetting(itemUsageSwinging);
        addSetting(oldSneakAnimation);
        addSetting(armorHurtTint);
        addSetting(rightClickFix);
        addSetting(oldDamageTilt);
        addSetting(oldHeartAnimation);
        instance = this;
    }

    public static boolean swordBlockingEnabled() {
        return instance != null && instance.isEnabled() && instance.blockHitting.isEnabled();
    }

    public static boolean rightClickFixEnabled() {
        return instance != null && instance.isEnabled() && instance.rightClickFix.isEnabled();
    }

    public static boolean damageAnimationEnabled() {
        return instance == null || !instance.isEnabled() || instance.oldDamageTilt.isEnabled();
    }

    public static boolean oldSneakAnimationEnabled() {
        return instance != null && instance.isEnabled() && instance.oldSneakAnimation.isEnabled();
    }

    public static boolean armorHurtTintEnabled() {
        return instance == null || !instance.isEnabled() || instance.armorHurtTint.isEnabled();
    }

    public static boolean oldHeartAnimationEnabled() {
        return instance == null || !instance.isEnabled() || instance.oldHeartAnimation.isEnabled();
    }

    public static void applyFirstPersonPose(LivingEntity entity, ItemStack stack,
                                            ItemDisplayContext context, PoseStack pose) {
        OldAnimations module = instance;
        Minecraft client = Minecraft.getInstance();
        if (module == null || !module.isEnabled() || entity != client.player || stack.isEmpty()
                || (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
            return;
        }

        int handIndex = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? 0 : 1;
        Item currentItem = stack.getItem();
        long now = System.nanoTime();
        if (module.oldItemSwitch.isEnabled()) {
            if (LAST_ITEMS[handIndex] != null && LAST_ITEMS[handIndex] != currentItem) {
                SWITCH_STARTS[handIndex] = now;
            }
            LAST_ITEMS[handIndex] = currentItem;
            long elapsed = now - SWITCH_STARTS[handIndex];
            if (SWITCH_STARTS[handIndex] != 0 && elapsed < 180_000_000L) {
                float progress = elapsed / 180_000_000f;
                pose.translate(0, -0.16f * (1f - progress), 0);
            }
        }
        if (module.itemUsageSwinging.isEnabled() && entity.isUsingItem()
                && entity.getUseItem().getItem() == currentItem) {
            float phase = (float) ((now / 1_000_000_000.0) % 1.0);
            pose.translate(0, (float) Math.sin(phase * Math.PI * 2) * 0.015f, 0);
            pose.mulPose(Axis.XP.rotationDegrees((float) Math.sin(phase * Math.PI * 2) * 3f));
        }
    }

    public static boolean shouldUseLegacyBlockPose(LivingEntity entity, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (!swordBlockingEnabled() || entity != client.player || !stack.is(ItemTags.SWORDS)) {
            return false;
        }
        return rightClickFixEnabled()
                ? client.options.keyUse.isDown()
                : entity.isUsingItem();
    }
}
