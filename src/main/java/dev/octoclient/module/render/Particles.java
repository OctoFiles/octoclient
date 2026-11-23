package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.Setting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import dev.octoclient.ui.ParticleSelectorScreen;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** Selectively suppresses noisy vanilla particle categories at their spawn point. */
public class Particles extends Module {
    private static Particles instance;
    private final BooleanSetting hideExplosions = new BooleanSetting("Hide Explosions", "Suppress explosion particles", false);
    private final BooleanSetting hideDamage = new BooleanSetting("Hide Damage", "Suppress damage indicators and crit particles", false);
    private final BooleanSetting hideWeather = new BooleanSetting("Hide Weather", "Suppress rain and snow particles", false);
    private final BooleanSetting hideSmoke = new BooleanSetting("Hide Smoke", "Suppress smoke and campfire particles", false);
    private final Map<String, BooleanSetting> particleSettings = new LinkedHashMap<>();

    public Particles() {
        super("Particles", "Filter selected particle types", 'P');
        addSetting(hideExplosions);
        addSetting(hideDamage);
        addSetting(hideWeather);
        addSetting(hideSmoke);
        addSetting(new ActionSetting("Particle List", "Search and toggle individual particle types",
                "Choose Particles", () -> {
                    Screen parent = Minecraft.getInstance().gui.screen();
                    Minecraft.getInstance().setScreenAndShow(new ParticleSelectorScreen(parent, this));
                }));
        for (var id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            String path = id.getPath();
            String label = path.replace('_', ' ');
            BooleanSetting setting = new BooleanSetting(label, "Suppress minecraft:" + path, false);
            particleSettings.put(id.toString(), setting);
            addSetting(setting);
        }
        instance = this;
    }

    public Map<String, BooleanSetting> particleSettings() {
        return Map.copyOf(particleSettings);
    }

    @Override
    public List<Setting<?>> getConfigSettings() {
        List<Setting<?>> settings = new ArrayList<>(super.getConfigSettings());
        settings.addAll(particleSettings.values());
        return List.copyOf(settings);
    }

    public static boolean shouldSuppress(ParticleOptions particle) {
        Particles module = instance;
        if (module == null || !module.isEnabled()) {
            return false;
        }
        var type = particle.getType();
        String id = BuiltInRegistries.PARTICLE_TYPE.getKey(type).toString();
        BooleanSetting individual = module.particleSettings.get(id);
        return (individual != null && individual.isEnabled())
                || (module.hideExplosions.isEnabled()
                && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER))
                || (module.hideDamage.isEnabled()
                && (type == ParticleTypes.DAMAGE_INDICATOR || type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT))
                || (module.hideWeather.isEnabled()
                && (type == ParticleTypes.RAIN || type == ParticleTypes.SNOWFLAKE))
                || (module.hideSmoke.isEnabled()
                && (type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE
                || type == ParticleTypes.WHITE_SMOKE || type == ParticleTypes.CAMPFIRE_COSY_SMOKE
                || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE));
    }
}
