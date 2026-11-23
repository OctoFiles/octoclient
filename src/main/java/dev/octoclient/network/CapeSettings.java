package dev.octoclient.network;

import dev.octoclient.module.cosmetics.Cape;

/**
 * The parts of a cape loadout that change how it moves. This travels with the
 * cape id so other OctoClient users can reproduce the wearer's WaveyCapes look
 * instead of falling back to their own local settings.
 */
public record CapeSettings(String style, String windMode, String movement,
                           float gravity, float heightMultiplier,
                           boolean computeGravityVector) {
    public static final CapeSettings DEFAULT =
            new CapeSettings("Smooth", "No Wind", "Simulation V2", 25f, 6f, false);

    public static CapeSettings from(Cape cape) {
        return new CapeSettings(
                cape.style.getValue(),
                cape.windMode.getValue(),
                cape.movement.getValue(),
                cape.gravity.getFloat(),
                cape.heightMultiplier.getFloat(),
                cape.computeGravityVector.isEnabled());
    }

    /** Compact, fixed-field form for the cosmetic loadout string. */
    public String encode() {
        return String.join("~",
                style,
                windMode,
                movement,
                Float.toString(gravity),
                Float.toString(heightMultiplier),
                computeGravityVector ? "1" : "0");
    }

    public static CapeSettings decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return DEFAULT;
        }
        String[] parts = raw.split("~", -1);
        if (parts.length < 6) {
            return DEFAULT;
        }
        return new CapeSettings(
                parts[0], parts[1], parts[2],
                parseFloat(parts[3], DEFAULT.gravity()),
                parseFloat(parts[4], DEFAULT.heightMultiplier()),
                "1".equals(parts[5]));
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
