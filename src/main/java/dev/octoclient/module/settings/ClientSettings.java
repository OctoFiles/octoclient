package dev.octoclient.module.settings;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.ModeSetting;

import java.util.List;

/** Client-wide dashboard settings. */
public class ClientSettings extends Module {
    /** Style value for the dark frosted glass card look (Module/Card Style). */
    public static final String STYLE_FROSTED = "Dark Frosted Glass";
    /** Corner shape values shared by the module and client shape settings. */
    public static final String SHAPE_ROUNDED = "Rounded";
    public static final String SHAPE_SQUARE = "Square";
    /** Default tint of the dashboard glass sheet (RGB). */
    private static final int DEFAULT_DASHBOARD_COLOR = 0x9AA1BC;

    private static ClientSettings instance;

    private final ColorSetting accent = new ColorSetting("Accent Color", "UI accent color", 0xFFE2E8F0);
    private final NumberSetting animationSpeed = new NumberSetting("Animation Speed",
            "How fast UI animations settle", 1.0, 0.3, 2.5, 0.1);
    /**
     * Writes into every module's own Radius setting. Per-module sliders keep
     * working afterwards; this one is the "round everything at once" control.
     */
    private final NumberSetting allRadius = new NumberSetting("Module Radius",
            "Corner rounding applied to every module at once", 10, 0, 24, 1);
    private final NumberSetting buttonRadius = new NumberSetting("Button Roundness",
            "Corner radius for Minecraft menu buttons", 0, 0, 24, 1);
    private final NumberSetting dashboardScale = new NumberSetting("Client Size",
            "Scale the in-game OctoClient dashboard", 1.0, 0.6, 1.0, 0.05);
    private final BooleanSetting tooltips = new BooleanSetting("Tooltips", "Show hover tooltips", true);
    private final BooleanSetting noBackground =
            new BooleanSetting("No Background", "Hide backgrounds on all HUD elements", false);
    private final BooleanSetting dashboardBackground = new BooleanSetting("Dashboard Background",
            "Show the sheet behind the dashboard", true);
    /** Background sheet style; Frosted reuses the dark-glassmorphism look. */
    private final ModeSetting dashboardStyle = new ModeSetting("Dashboard Style",
            "Dashboard background: glass, clear, dark frosted glass or solid",
            "Glass", List.of("Glass", "Clear", STYLE_FROSTED, "Solid"));
    private final ColorSetting dashboardColor = new ColorSetting("Background Color",
            "Tint of the dashboard glass sheet", 0xFF000000 | DEFAULT_DASHBOARD_COLOR);
    private final BooleanSetting clearMultiplayerBackground = new BooleanSetting("Clear Multiplayer Background",
            "Keep the multiplayer menu panorama clear instead of blurred", false);
    private final ModeSetting moduleStyle = new ModeSetting("Module Style",
            "Default card look: glass, clear outline, dark frosted glass or solid panel",
            "Glass", List.of("Glass", "Clear", STYLE_FROSTED, "Solid"));
    /** Forces every module card and HUD panel square without touching saved radii. */
    private final ModeSetting moduleShape = new ModeSetting("Module Corners",
            "Square or rounded module cards and HUD panels", SHAPE_ROUNDED,
            List.of(SHAPE_ROUNDED, SHAPE_SQUARE));
    /** Corner shape of the dashboard window itself. */
    private final ModeSetting clientShape = new ModeSetting("Client Corners",
            "Square or rounded dashboard window", SHAPE_ROUNDED,
            List.of(SHAPE_ROUNDED, SHAPE_SQUARE));
    private final NumberSetting cardWidth = new NumberSetting("Card Width",
            "Width of every dashboard card", 1.0, 0.6, 1.0, 0.05);
    private final ModeSetting settingsPresentation = new ModeSetting("Module Settings",
            "Open module settings in the inspector panel or inline accordion", "Popup", List.of("Popup", "Accordion"));

    /** Last value pushed to the modules; {@code -1} until the first sync. */
    private double pushedRadius = -1;

    public ClientSettings() {
        super("Client Settings", "Dashboard appearance and behavior", 'D');
        addSetting(accent);
        addSetting(animationSpeed);
        addSetting(allRadius);
        addSetting(buttonRadius);
        addSetting(dashboardScale);
        addSetting(tooltips);
        addSetting(noBackground);
        addSetting(dashboardBackground);
        addSetting(dashboardStyle);
        addSetting(dashboardColor);
        addSetting(clearMultiplayerBackground);
        addSetting(moduleStyle);
        addSetting(moduleShape);
        addSetting(clientShape);
        addSetting(cardWidth);
        addSetting(settingsPresentation);
        instance = this;
    }

    public ColorSetting accentSetting() {
        return accent;
    }

    /** Accent color without alpha for fills. */
    public static int accentRgb() {
        if (instance == null) {
            return 0xE2E8F0;
        }
        return instance.accent.getRgb();
    }

    public static float animationSpeed() {
        if (instance == null) {
            return 1.0f;
        }
        return instance.animationSpeed.getFloat();
    }

    public static int buttonRadius() {
        return instance == null ? 0 : instance.buttonRadius.getInt();
    }

    public static float dashboardScale() {
        return instance == null ? 1f : instance.dashboardScale.getFloat();
    }

    public static boolean clearMultiplayerBackground() {
        return instance != null && instance.clearMultiplayerBackground.isEnabled();
    }

    public static boolean tooltipsEnabled() {
        return instance != null && instance.tooltips.isEnabled();
    }

    public static boolean noBackgroundEnabled() {
        return instance != null && instance.noBackground.isEnabled();
    }

    /** When off the dashboard drops the glass sheet and its chrome washes. */
    public static boolean dashboardBackgroundEnabled() {
        return instance == null || instance.dashboardBackground.isEnabled();
    }

    /** Sheet look behind the dashboard: Glass, Clear, Dark Frosted Glass or Solid. */
    public static String dashboardStyle() {
        return instance == null ? "Glass" : instance.dashboardStyle.getValue();
    }

    /** Tint of the dashboard glass sheet, RGB only. */
    public static int dashboardColorRgb() {
        if (instance == null) {
            return DEFAULT_DASHBOARD_COLOR;
        }
        return instance.dashboardColor.getRgb() & 0xFFFFFF;
    }

    /** True when all module cards and HUD panels should have square corners. */
    public static boolean modulesSquare() {
        return instance != null && SHAPE_SQUARE.equals(instance.moduleShape.getValue());
    }

    /** Corner radius of the dashboard window and its side/inspector panels. */
    public static float clientRadius() {
        return instance != null && SHAPE_SQUARE.equals(instance.clientShape.getValue()) ? 0f : 22f;
    }

    /** Width factor applied to every dashboard card (1.0 = full column). */
    public static float cardWidthScale() {
        if (instance == null) {
            return 1f;
        }
        return instance.cardWidth.getFloat();
    }

    /** Global dashboard card style: Glass, Clear or Solid. */
    public static String moduleStyle() {
        if (instance == null) {
            return "Glass";
        }
        return instance.moduleStyle.getValue();
    }

    public static boolean popupSettingsEnabled() {
        return instance != null && instance.settingsPresentation.getValue().equals("Popup");
    }

    /**
     * Applies the global radius to every module when the user moves it.
     *
     * <p>The first call only records the loaded value: otherwise every launch
     * would flatten the per-module radii that were saved to the config. After
     * that, a changed value is copied into all modules — including this one.</p>
     */
    public static void syncGlobalRadius() {
        if (instance == null) {
            return;
        }
        double value = instance.allRadius.getSnapped();
        if (instance.pushedRadius < 0) {
            instance.pushedRadius = value;
            return;
        }
        if (Math.abs(value - instance.pushedRadius) < 1.0e-6) {
            return;
        }
        instance.pushedRadius = value;
        for (Module module : ModuleManager.get().getModules()) {
            if (module == instance) {
                continue;
            }
            module.roundness().setRaw(value);
        }
    }
}
