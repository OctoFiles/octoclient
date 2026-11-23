package dev.octoclient.network;

/**
 * The cosmetic loadout a player has selected, in a form that is cheap to send
 * over the network and to compare for changes.
 *
 * <p>Designs are referenced by their display name (the same name the module
 * uses), an empty string meaning "none". Cape and accessory names are optional
 * trailing fields so older loadout strings remain readable.</p>
 */
public record CosmeticLoadout(String bandana, String wings, boolean hat, int hatColor,
                              boolean halo, int haloColor, boolean aura, int auraColor,
                              String auraShape, String cape, String accessory, String hatDesign,
                              String minimeSkinUser, String catalogSelection,
                              boolean nametagEnabled, boolean nametagHealth, boolean nametagDistance,
                              int nametagColor, String nametagAnimation, float nametagAnimationHeight,
                              String capeSettings) {
    public static final CosmeticLoadout EMPTY =
            new CosmeticLoadout("", "", false, 0, false, 0, false, 0,
                    "Ring", "", "", "", "", "");
    public static final String DEFAULT_AURA_SHAPE = "Ring";

    public CosmeticLoadout(String bandana, String wings, boolean hat, int hatColor,
                           boolean halo, int haloColor, boolean aura, int auraColor,
                           String auraShape, String cape, String accessory, String hatDesign,
                           String minimeSkinUser) {
        this(bandana, wings, hat, hatColor, halo, haloColor, aura, auraColor, auraShape,
                cape, accessory, hatDesign, minimeSkinUser, "");
    }

    public CosmeticLoadout(String bandana, String wings, boolean hat, int hatColor,
                           boolean halo, int haloColor, boolean aura, int auraColor,
                           String auraShape, String cape, String accessory, String hatDesign,
                           String minimeSkinUser, String catalogSelection) {
        this(bandana, wings, hat, hatColor, halo, haloColor, aura, auraColor, auraShape,
                cape, accessory, hatDesign, minimeSkinUser, catalogSelection,
                false, true, false, 0xFFFFFFFF, "None", 0.18f, "");
    }

    public CosmeticLoadout(String bandana, String wings, boolean hat, int hatColor,
                           boolean halo, int haloColor, boolean aura, int auraColor,
                           String auraShape, String cape, String accessory, String hatDesign,
                           String minimeSkinUser, String catalogSelection,
                           boolean nametagEnabled, boolean nametagHealth, boolean nametagDistance,
                           int nametagColor, String nametagAnimation, float nametagAnimationHeight) {
        this(bandana, wings, hat, hatColor, halo, haloColor, aura, auraColor, auraShape,
                cape, accessory, hatDesign, minimeSkinUser, catalogSelection,
                nametagEnabled, nametagHealth, nametagDistance, nametagColor,
                nametagAnimation, nametagAnimationHeight, "");
    }

    /**
     * The shared OctoClient API only accepts the original ten-field loadout.
     * Its cape field is free-form, so the movement settings ride along there
     * after a {@code ~}; {@link #decode(String)} unpacks them again. The rich
     * {@link #encode()} form is still used by the custom server payload.
     */
    public String encodeLegacy() {
        String legacyCape = cape;
        if (!capeSettings.isEmpty() && !legacyCape.isEmpty()) {
            legacyCape = legacyCape + "~" + capeSettings;
        }
        return String.join("|",
                bandana,
                wings,
                hat ? "1" : "0",
                Integer.toString(hatColor),
                halo ? "1" : "0",
                Integer.toString(haloColor),
                aura ? "1" : "0",
                Integer.toString(auraColor),
                auraShape,
                legacyCape);
    }

    public String encode() {
        String base = String.join("|",
                bandana,
                wings,
                hat ? "1" : "0",
                Integer.toString(hatColor),
                halo ? "1" : "0",
                Integer.toString(haloColor),
                aura ? "1" : "0",
                Integer.toString(auraColor),
                auraShape);
        // Cape movement settings are a trailing field, so use the full canonical
        // layout whenever they are present instead of the compact short forms.
        if (!capeSettings.isEmpty()) {
            return base + "|" + cape + "|" + accessory + "|" + hatDesign
                    + "|" + minimeSkinUser + "|" + catalogSelection
                    + "|" + nametagFields() + "|" + capeSettings;
        }
        String result;
        if (!catalogSelection.isEmpty()) {
            result = base + "|" + cape + "|" + accessory + "|" + hatDesign
                    + "|" + minimeSkinUser + "|" + catalogSelection
                    + "|" + (nametagEnabled ? "1" : "0") + "|" + (nametagHealth ? "1" : "0")
                    + "|" + (nametagDistance ? "1" : "0") + "|" + nametagColor
                    + "|" + nametagAnimation + "|" + nametagAnimationHeight;
        } else if (cape.isEmpty() && accessory.isEmpty() && hatDesign.isEmpty() && minimeSkinUser.isEmpty()
                && !nametagEnabled) {
            result = base;
        } else if (cape.isEmpty() && accessory.isEmpty() && hatDesign.isEmpty() && minimeSkinUser.isEmpty()) {
            result = base + "|||||" + nametagFields();
        } else {
            String withCape = base + "|" + cape;
            if (!accessory.isEmpty() || !hatDesign.isEmpty() || !minimeSkinUser.isEmpty()) {
                withCape += "|" + accessory;
            }
            if (!hatDesign.isEmpty() || !minimeSkinUser.isEmpty()) {
                withCape += "|" + hatDesign;
            }
            result = minimeSkinUser.isEmpty() ? withCape : withCape + "|" + minimeSkinUser;
        }
        return capeSettings.isEmpty() ? result : result + "|" + capeSettings;
    }

    private String nametagFields() {
        return (nametagEnabled ? "1" : "0") + "|" + (nametagHealth ? "1" : "0")
                + "|" + (nametagDistance ? "1" : "0") + "|" + nametagColor
                + "|" + nametagAnimation + "|" + nametagAnimationHeight;
    }

    public static CosmeticLoadout decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return EMPTY;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 9) {
            return EMPTY;
        }
        String capeField = parts.length > 9 ? parts[9] : "";
        String cape = capeField;
        String capeSettings = parts.length > 20 ? parts[20] : "";
        int packed = capeField.indexOf('~');
        if (packed > 0) {
            cape = capeField.substring(0, packed);
            if (capeSettings.isEmpty()) {
                capeSettings = capeField.substring(packed + 1);
            }
        }
        return new CosmeticLoadout(
                parts[0],
                parts[1],
                "1".equals(parts[2]),
                parseInt(parts[3], 0),
                "1".equals(parts[4]),
                parseInt(parts[5], 0),
                "1".equals(parts[6]),
                parseInt(parts[7], 0),
                parts[8].isEmpty() ? DEFAULT_AURA_SHAPE : parts[8],
                cape,
                parts.length > 10 ? parts[10] : "",
                parts.length > 11 ? parts[11] : "",
                parts.length > 12 ? parts[12] : "",
                parts.length > 13 ? parts[13] : "",
                parts.length > 14 && "1".equals(parts[14]),
                parts.length <= 15 || "1".equals(parts[15]),
                parts.length > 16 && "1".equals(parts[16]),
                parseInt(parts.length > 17 ? parts[17] : "-1", -1),
                parts.length > 18 && !parts[18].isEmpty() ? parts[18] : "None",
                parseFloat(parts.length > 19 ? parts[19] : "0.18", 0.18f),
                capeSettings);
    }

    public boolean isEmpty() {
        return bandana.isEmpty() && wings.isEmpty() && !hat && !halo && !aura
                && cape.isEmpty() && accessory.isEmpty() && hatDesign.isEmpty()
                && minimeSkinUser.isEmpty() && catalogSelection.isEmpty();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
