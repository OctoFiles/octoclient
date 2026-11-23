package dev.octoclient.module.render;

import dev.octoclient.mixin.OptionInstanceAccessor;
import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/** Cranks gamma up for cave/night visibility. */
public class Fullbright extends Module implements Module.Tickable {
    private final NumberSetting brightness = new NumberSetting("Brightness", "Gamma multiplier", 15.0, 1.0, 30.0, 0.5);

    private double previousGamma = 0.5;
    private boolean capturedGamma;

    public Fullbright() {
        super("Fullbright", "See in the dark", 'B');
        addSetting(brightness);
    }

    @Override
    protected void onEnable() {
        capturedGamma = false;
    }

    @Override
    protected void onDisable() {
        if (capturedGamma) {
            applyGamma(previousGamma);
            capturedGamma = false;
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (!capturedGamma) {
            previousGamma = client.options.gamma().get();
            capturedGamma = true;
        }
        applyGamma(brightness.getSnapped());
    }

    public void onGammaChanged() {
        if (isEnabled()) {
            applyGamma(brightness.getSnapped());
        }
    }

    /**
     * The vanilla gamma option only accepts slider values (-1.5..1.5) and
     * silently drops anything else. Write the field directly so large
     * multipliers actually take effect.
     */
    @SuppressWarnings("unchecked")
    private static void applyGamma(double gamma) {
        OptionInstance<Double> gammaOption = Minecraft.getInstance().options.gamma();
        ((OptionInstanceAccessor<Double>) (Object) gammaOption).octoclient$forceValue(gamma);
    }
}
