package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;

/**
 * Ring of light floating above the head, with a few orbs circling it.
 * Drawn by {@link dev.octoclient.render.CosmeticLayers}; client-side only.
 */
public class Halo extends Module implements CosmeticPart {
    public final ColorSetting color = new ColorSetting("Color", "Halo colour", 0xFFFFD86B);
    public final NumberSetting size = new NumberSetting("Size", "Halo radius", 1.0, 0.6, 1.8, 0.05);
    public final NumberSetting orbs = new NumberSetting("Orbs", "How many orbs circle the ring", 4, 0, 8, 1);
    public final NumberSetting speed = new NumberSetting("Speed", "How fast it spins", 1.0, 0.2, 3.0, 0.1);
    public final BooleanSetting bob = new BooleanSetting("Bob", "Let the orbs bob up and down", true);

    public Halo() {
        super("Halo", "Glowing ring above your head", 'O');
        addSetting(color);
        addSetting(size);
        addSetting(orbs);
        addSetting(speed);
        addSetting(bob);
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.AURA;
    }
}
