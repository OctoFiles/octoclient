package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Shared range override for the crystal modules. When enabled, the other
 * Crystal* modules use these values instead of their own defaults.
 */
public class CrystalRange extends Module {
    private static CrystalRange instance;

    private final NumberSetting placeRange = new NumberSetting("place-range",
            "Placement range used by the crystal modules.", 4.5, 0, 6, 0.1);
    private final NumberSetting breakRange = new NumberSetting("break-range",
            "Breaking range used by the crystal modules.", 4.5, 0, 6, 0.1);
    private final BooleanSetting announce = new BooleanSetting("announce",
            "Shows the active ranges in the action bar when enabled.", false);

    public CrystalRange() {
        super("CrystalRange", "Sets the place and break range of the crystal modules.", 'C');
        addSetting(placeRange);
        addSetting(breakRange);
        addSetting(announce);
        instance = this;
    }

    @Override
    protected void onEnable() {
        if (announce.isEnabled() && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendOverlayMessage(Component.literal(
                    "Crystal range: place " + placeRange.getValue()
                            + ", break " + breakRange.getValue()));
        }
    }

    static double placeRange(double fallback) {
        CrystalRange module = instance;
        return module != null && module.isEnabled() ? module.placeRange.getValue() : fallback;
    }

    static double breakRange(double fallback) {
        CrystalRange module = instance;
        return module != null && module.isEnabled() ? module.breakRange.getValue() : fallback;
    }
}
