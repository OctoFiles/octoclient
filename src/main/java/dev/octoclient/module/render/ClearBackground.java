package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;

/** Removes selected vanilla UI backdrops without changing OctoClient panel styling. */
public class ClearBackground extends Module {
    private static ClearBackground instance;
    private final BooleanSetting chat = new BooleanSetting("Chat", "Remove the chat message backdrop", false);
    private final BooleanSetting inventory = new BooleanSetting("Inventory", "Remove the inventory screen backdrop", false);
    private final BooleanSetting scoreboard = new BooleanSetting("Scoreboard", "Remove the scoreboard panel backdrop", false);
    private final BooleanSetting nametags = new BooleanSetting("Nametags", "Remove the player nameplate backdrop", false);
    private final BooleanSetting tabList = new BooleanSetting("Tab List", "Remove the tab-list backdrop", false);

    public ClearBackground() {
        super("Clear Background", "Hide backgrounds for selected vanilla interfaces", 'C');
        addSetting(chat);
        addSetting(inventory);
        addSetting(scoreboard);
        addSetting(nametags);
        addSetting(tabList);
        instance = this;
    }

    public static boolean clearsChat() {
        return active() && instance.chat.isEnabled();
    }

    public static boolean clearsInventory() {
        return active() && instance.inventory.isEnabled();
    }

    public static boolean clearsScoreboard() {
        return active() && instance.scoreboard.isEnabled();
    }

    public static boolean clearsNametags() {
        return active() && instance.nametags.isEnabled();
    }

    public static boolean clearsTabList() {
        return active() && instance.tabList.isEnabled();
    }

    private static boolean active() {
        return instance != null && instance.isEnabled();
    }

    /** Retained for existing panel callers; this module no longer globally changes custom panel opacity. */
    public static float opacityMultiplier() {
        return 1f;
    }
}
