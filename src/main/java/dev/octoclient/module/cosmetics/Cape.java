package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.render.AnimatedCapeTextures;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Selects a cape texture and configures how it moves. The settings mirror
 * tr7zw's WaveyCapes exactly: same names, same values, same defaults and the
 * same ranges. The simulation itself lives in
 * {@link dev.octoclient.render.CapePhysics}. The selected texture is relayed by
 * compatible servers so other OctoClient users can render it locally.
 */
public class Cape extends Module implements CosmeticPart {
    public static final float STANDARD_TEXTURE_U0 = 0.06f;
    public static final float STANDARD_TEXTURE_U1 = 0.49f;
    public static final float STANDARD_TEXTURE_V0 = 0.08f;
    public static final float STANDARD_TEXTURE_V1 = 0.96f;

    /** WaveyCapes defaults: SMOOTH / NONE / BASIC_SIMULATION_3D / 25 / 6 / 2 / false. */
    public final ModeSetting style = new ModeSetting("Cape Style",
            "Switch between a smoothly bending and blocky cape", "Smooth", List.of("Blocky", "Smooth"));
    public final ModeSetting windMode = new ModeSetting("Wind Mode",
            "Add extra movement to the cape", "No Wind", List.of("No Wind", "Waves"));
    public final ModeSetting movement = new ModeSetting("Cape Movement",
            "Change the way the cape behaves", "Simulation V2",
            List.of("Classic", "Simulation", "Simulation V2", "Dungeons"));
    public final NumberSetting gravity = new NumberSetting("Gravity",
            "Change the amount of gravity applied to the cape", 25, 5, 32, 1);
    public final NumberSetting heightMultiplier = new NumberSetting("Height Multiplier",
            "Change how extreme the cape reacts to Y movement", 6, 4, 16, 1);
    public final BooleanSetting computeGravityVector = new BooleanSetting("Compute Gravity Vector",
            "Use the player's gravity vector instead of a world-down one", false);

    private final String fileName;
    private final boolean vanillaTexture;

    public Cape(String name, String fileName) {
        super(name, "Wear this cape; share it on supported servers", 'C');
        this.fileName = fileName;
        this.vanillaTexture = fileName.equals("skinmc-cape.png");
        addSetting(style);
        addSetting(windMode);
        addSetting(movement);
        addSetting(gravity);
        addSetting(heightMultiplier);
        addSetting(computeGravityVector);
    }

    public Identifier texture() {
        return AnimatedCapeTextures.textureId(fileName);
    }

    public boolean isAnimated() {
        return fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".gif");
    }

    public String fileName() {
        return fileName;
    }

    public float textureU0() {
        return vanillaTexture ? 1f / 64f : STANDARD_TEXTURE_U0;
    }

    public float textureU1() {
        return vanillaTexture ? 11f / 64f : STANDARD_TEXTURE_U1;
    }

    public float textureV0() {
        return vanillaTexture ? 1f / 32f : STANDARD_TEXTURE_V0;
    }

    public float textureV1() {
        return vanillaTexture ? 17f / 32f : STANDARD_TEXTURE_V1;
    }

    @Override
    protected void onEnable() {
        CatalogCosmetic.disableCatalogType("CAPE", this);
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof Cape other && other != this && other.isEnabled()) {
                other.setEnabled(false);
            }
        }
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.CAPE;
    }
}
