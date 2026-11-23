package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class ShinyPots extends Module {
    private static ShinyPots instance;

    private final BooleanSetting splash =
            new BooleanSetting("Splash Potions", "Add glint to healing splash potions", true);
    private final BooleanSetting lingering =
            new BooleanSetting("Lingering Potions", "Add glint to healing lingering potions", true);

    public ShinyPots() {
        super("Shiny Pots", "Add an enchantment glint to healing potions", 'P');
        addSetting(splash);
        addSetting(lingering);
        instance = this;
    }

    public static boolean shouldGlow(ItemStack stack) {
        if (instance == null || !instance.isEnabled()) {
            return false;
        }
        boolean allowedItem = stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION) && instance.splash.isEnabled()
                || stack.is(Items.LINGERING_POTION) && instance.lingering.isEnabled();
        if (!allowedItem) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && (contents.is(Potions.HEALING) || contents.is(Potions.STRONG_HEALING));
    }
}
