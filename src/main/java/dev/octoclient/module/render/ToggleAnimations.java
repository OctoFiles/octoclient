package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;

import java.util.List;

/**
 * Governs how HUD elements transition when a module is toggled.
 *
 * <p>Consumed by {@code ArraylistHud} (slide/fade of the chips) and by the
 * dashboard's own motion via {@link #transitionStyle()}.</p>
 */
public class ToggleAnimations extends Module {
    private final ModeSetting style = new ModeSetting("Style", "How chips enter and leave",
            "Slide", List.of("Slide", "Fade", "Pop"));
    private final NumberSetting speed = new NumberSetting("Speed", "Transition speed", 1.0, 0.3, 2.5, 0.1);
    private final BooleanSetting shadows = new BooleanSetting("Shadows", "Drop shadow on chips", true);

    public ToggleAnimations() {
        super("Toggle Animations", "Animated module list transitions", 'I');
        addSetting(style);
        addSetting(speed);
        addSetting(shadows);
    }

    /** Active transition style, one of {@code Style}. */
    public String transitionStyle() {
        return style.getValue();
    }

    /** Multiplier applied to HUD transition rates; 1.0 is the default feel. */
    public float speedMultiplier() {
        return speed.getFloat();
    }

    /** Whether chips should draw a soft drop shadow. */
    public boolean shadowsEnabled() {
        return shadows.isEnabled();
    }

    /** Named transition styles, so callers can switch on a constant. */
    public enum Style {
        SLIDE, FADE, POP;

        public static Style of(String name) {
            for (Style s : values()) {
                if (s.name().equalsIgnoreCase(name)) {
                    return s;
                }
            }
            return SLIDE;
        }
    }
}
