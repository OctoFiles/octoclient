package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/** Port of Meteor's NoSlow (item use, block slowdowns and sneaking). */
public class NoSlow extends Module {
    private static NoSlow instance;

    private final BooleanSetting items = new BooleanSetting("items",
            "Removes the slowdown from using items.", true);
    private final ModeSetting web = new ModeSetting("web",
            "Removes the slowdown from cobwebs.", "Vanilla", List.of("Vanilla", "None"));
    private final NumberSetting webTimer = new NumberSetting("web-timer",
            "The timer to use in cobwebs.", 10, 1, 20, 1);
    private final BooleanSetting honeyBlock = new BooleanSetting("honey-block",
            "Removes the slowdown from honey blocks.", true);
    private final BooleanSetting soulSand = new BooleanSetting("soul-sand",
            "Removes the slowdown from soul sand.", true);
    private final BooleanSetting slimeBlock = new BooleanSetting("slime-block",
            "Removes the slowdown from slime blocks.", true);
    private final BooleanSetting berryBush = new BooleanSetting("berry-bush",
            "Removes the slowdown from sweet berry bushes.", true);
    private final BooleanSetting airStrict = new BooleanSetting("air-strict",
            "Only applies while on the ground.", false);
    private final BooleanSetting fluidDrag = new BooleanSetting("fluid-drag",
            "Removes the drag from fluids.", false);
    private final BooleanSetting sneaking = new BooleanSetting("sneaking",
            "Removes the slowdown from sneaking.", false);
    private final BooleanSetting hunger = new BooleanSetting("hunger",
            "Removes the slowdown from hunger.", false);
    private final BooleanSetting slowness = new BooleanSetting("slowness",
            "Removes the slowdown from the slowness effect.", false);

    public NoSlow() {
        super("NoSlow", "Removes the slowdown from using items and blocks.", 'N');
        addSetting(items);
        addSetting(web);
        addSetting(webTimer);
        addSetting(honeyBlock);
        addSetting(soulSand);
        addSetting(slimeBlock);
        addSetting(berryBush);
        addSetting(airStrict);
        addSetting(fluidDrag);
        addSetting(sneaking);
        addSetting(hunger);
        addSetting(slowness);
        instance = this;
    }

    public static boolean shouldRemoveSlowdown(LocalPlayer player) {
        NoSlow module = instance;
        if (module == null || !module.isEnabled() || !module.items.isEnabled() || player == null) {
            return false;
        }
        return !module.airStrict.isEnabled() || player.onGround();
    }

    /** Removes the honey/soul sand/slime block speed factor. */
    public static boolean shouldRemoveBlockSlowdown() {
        NoSlow module = instance;
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return module.honeyBlock.isEnabled() || module.soulSand.isEnabled() || module.slimeBlock.isEnabled();
    }

    public static boolean shouldRemoveSneakSlowdown() {
        NoSlow module = instance;
        return module != null && module.isEnabled() && module.sneaking.isEnabled();
    }
}
