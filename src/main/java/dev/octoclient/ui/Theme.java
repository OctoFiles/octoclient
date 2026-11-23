package dev.octoclient.ui;

/**
 * Central color palette - enhanced glassmorphism design with vibrant accents
 * and improved visual hierarchy.
 */
public final class Theme {
    private Theme() {
    }

    // Grays (Enhanced scale)
    public static final int DARK_950 = 0xFF050506;
    public static final int DARK_900 = 0xFF0F0F11;
    public static final int DARK_850 = 0xFF151517;
    public static final int DARK_800 = 0xFF1C1C1F;
    public static final int DARK_750 = 0xFF232326;
    public static final int DARK_700 = 0xFF2B2B2F;
    public static final int DARK_650 = 0xFF353539;
    public static final int DARK_600 = 0xFF3F3F44;
    public static final int DARK_500 = 0xFF525256;

    public static final int NEUTRAL_100 = 0xFFF8F8F8;
    public static final int NEUTRAL_150 = 0xFFF0F0F0;
    public static final int NEUTRAL_200 = 0xFFE8E8E8;
    public static final int NEUTRAL_300 = 0xFFD6D6D6;
    public static final int NEUTRAL_400 = 0xFFB4B4B4;
    public static final int NEUTRAL_500 = 0xFF8A8A8A;
    public static final int NEUTRAL_600 = 0xFF6B6B6B;

    // Vibrant accent colors
    public static final int EMERALD = 0xFF10B981;
    public static final int EMERALD_GLOW = 0xFF34D399;
    public static final int CYAN = 0xFF06B6D4;
    public static final int CYAN_GLOW = 0xFF38BDF8;
    public static final int BLUE = 0xFF3B82F6;
    public static final int PURPLE = 0xFF8B5CF6;
    public static final int PINK = 0xFFEC4899;
    public static final int ORANGE = 0xFFF59E0B;
    public static final int YELLOW = 0xFFFBBF24;
    public static final int RED = 0xFFEF4444;
    public static final int RED_GLOW = 0xFFF87171;

    // Gradient accents
    public static final int ACCENT_START = 0xFF9B6BFF;
    public static final int ACCENT_MID = 0xFF4F8BF7;
    public static final int ACCENT_END = 0xFF06B6D4;

    // Translucent composites - Apple-style liquid glass over the gameplay
    public static final int CARD_GRAD_TOP = 0xEC1C1C1F;      // translucent glass tint
    public static final int CARD_GRAD_BOTTOM = 0xE80F0F11;   // slightly see-through
    public static final int CARD_HOVER_TOP = 0xE8242428;
    public static final int CARD_HOVER_BOTTOM = 0xE6161619;
    /** Window body: translucent glass over the visible Minecraft world. */
    public static final int WINDOW_BG = 0xD8121214;
    /** Soft scrim behind the window so panels keep contrast. */
    public static final int SCREEN_BG = 0xBF000000;          // keep legacy alias
    public static final int HEADER_BG = 0xEC141417;
    public static final int SIDEBAR_BG = 0xEC0F0F11;
    // Soft input field background
    public static final int INPUT_BG = 0xEC101012;
    public static final int FOOTER_BG = 0xEC141417;

    public static final int BORDER_SUBTLE = 0x1AFFFFFF;      // More visible border
    public static final int BORDER_CARD = 0x22FFFFFF;
    public static final int BORDER_GLOW = 0x4D8B5CF6;        // Purple glow border
    public static final int BORDER_ACCENT = 0x663B82F6;

    // Dark frosted glass - "Dark Frosted Glass" / "Dark Glassmorphism".
    // A matte graphite pane over a heavy backdrop frost: no glossy sheen, only
    // a very fine light rim. Used by Draw.frostedGlass / Draw.frostedPanel and
    // selectable per module through the Card Style / Module Style settings.
    public static final int FROST_BODY_TOP = 0xFF191C23;
    public static final int FROST_BODY_BOTTOM = 0xFF101216;
    public static final int FROST_TINT = 0xC9D2E0;
    public static final int FROST_RIM = 0x2BFFFFFF;

    public static final int TEXT_PRIMARY = NEUTRAL_100;
    public static final int TEXT_SECONDARY = NEUTRAL_300;
    public static final int TEXT_MUTED = NEUTRAL_400;
    public static final int TEXT_FAINT = NEUTRAL_500;

    // Toggle - Enhanced
    public static final int TOGGLE_OFF_BG = 0xE6050506;
    public static final int TOGGLE_OFF_BORDER = DARK_600;
    public static final int TOGGLE_KNOB_OFF = 0xFFEDEDED;
    public static final int TOGGLE_ON_BG = 0xEAF8F8F8;
    public static final int TOGGLE_ON_GLOW = 0x40FFFFFF;

    // HUD edit
    public static final int HUD_FRAME = 0xCCFFFFFF;
    public static final int HUD_FRAME_SELECTED = 0xFFFFFFFF;
    public static final int SNAP_GUIDE = 0x9938BDF8;
    public static final int SNAP_GUIDE_GLOW = 0x3338BDF8;

    // Effects
    public static final int GLOW_PURPLE = 0x408B5CF6;
    public static final int GLOW_BLUE = 0x403B82F6;
    public static final int GLOW_CYAN = 0x4006B6D4;
    public static final int SHADOW_SOFT = 0x80000000;
    public static final int SHADOW_HARD = 0xA0000000;

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    /** Subtle top->bottom gradient for cards. */
    public static int cardTop(int row, int rows) {
        float t = rows <= 1 ? 0f : row / (float) (rows - 1);
        return mix(CARD_GRAD_TOP, CARD_GRAD_BOTTOM, t);
    }

    public static int mix(int from, int to, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, f);
        int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, f);
        int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, f);
        int b = lerp(from & 0xFF, to & 0xFF, f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
