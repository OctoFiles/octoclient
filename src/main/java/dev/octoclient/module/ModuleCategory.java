package dev.octoclient.module;

/** Categories shown as tabs in the Dashboard screen. */
public enum ModuleCategory {
    HUD("HUD", "hud"),
    RENDER("Render", "render"),
    UTILITY("Utility", "utility"),
    COSMETICS("Cosmetics", "cosmetics"),
    SETTINGS("Settings", "settings"),
    SECRET("???", "secret");

    public final String title;
    public final String id;

    ModuleCategory(String title, String id) {
        this.title = title;
        this.id = id;
    }
}
