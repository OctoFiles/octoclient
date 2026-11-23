package dev.octoclient.module.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.ui.WaypointScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Persistent world waypoints, deathpoints, and a compact target HUD. */
public class Waypoints extends HudModule implements Module.Tickable {
    public record Entry(int id, String name, String world, double x, double y, double z,
                        boolean deathpoint, long createdAt) {
    }

    private final KeybindSetting createKey =
            new KeybindSetting("Create Waypoint Key", "Create a waypoint at your current position", 0);
    private final ActionSetting openButton = new ActionSetting("Open Waypoints", "Manage saved waypoints",
            "Open List", () -> Minecraft.getInstance().setScreenAndShow(
                    new WaypointScreen(Minecraft.getInstance().gui.screen(), this)));
    private final BooleanSetting autoOnDeath =
            new BooleanSetting("Switch to Auto on Death", "Create and select a deathpoint after dying", true);
    private final BooleanSetting deathpoints =
            new BooleanSetting("Deathpoints", "Automatically save death locations", true);
    private final BooleanSetting oldDeathpoints =
            new BooleanSetting("Old Deathpoints", "Keep and display deathpoints from earlier sessions", true);
    private final BooleanSetting deleteReached =
            new BooleanSetting("Delete Reached Deathpoints", "Delete a deathpoint within two blocks", false);
    private final TextSetting worlds = new TextSetting("Worlds", "Filter waypoints by dimension; blank shows all", "", 128);
    private final TextSetting waypointName =
            new TextSetting("Waypoint Name", "Name used by the Create button and key", "Waypoint", 32);
    private final ActionSetting createButton =
            new ActionSetting("Create", "Save a waypoint at your current position", "+ Create", this::createWaypoint);

    private final List<Entry> entries = new ArrayList<>();
    private int nextId = 1;
    private int targetId = -1;
    private boolean createKeyWasDown;
    private boolean wasAlive = true;
    private final long sessionStartedAt = System.currentTimeMillis();

    public Waypoints() {
        super("Waypoints", "Save and navigate to locations in each world", 'W');
        addSetting(createKey);
        addSetting(openButton);
        addSetting(autoOnDeath);
        addSetting(deathpoints);
        addSetting(oldDeathpoints);
        addSetting(deleteReached);
        addSetting(worlds);
        addSetting(waypointName);
        addSetting(createButton);
        x = 8;
        y = 235;
    }

    @Override
    public void onTick(Minecraft client) {
        boolean canInput = client.player != null && client.level != null && client.gui.screen() == null;
        int key = createKey.getKeyCode();
        boolean down = canInput && key != 0
                && GLFW.glfwGetKey(client.getWindow().handle(), key) == GLFW.GLFW_PRESS;
        if (down && !createKeyWasDown) {
            createWaypoint();
        }
        createKeyWasDown = down;

        boolean alive = client.player != null && !client.player.isDeadOrDying();
        if (wasAlive && !alive && deathpoints.isEnabled() && client.player != null) {
            Entry death = add(waypointName.getValue() + " Death", client, true);
            if (autoOnDeath.isEnabled()) {
                targetId = death.id();
            }
        }
        wasAlive = alive;

        if (deleteReached.isEnabled() && client.player != null) {
            Entry target = target();
            if (target != null && target.deathpoint()
                    && client.player.distanceToSqr(target.x(), target.y(), target.z()) <= 4.0) {
                remove(target.id());
            }
        }
    }

    private void createWaypoint() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        add(waypointName.getValue(), client, false);
    }

    private Entry add(String name, Minecraft client, boolean deathpoint) {
        Entry entry = new Entry(nextId++, name.isBlank() ? "Waypoint" : name,
                client.level.dimension().identifier().toString(),
                client.player.getX(), client.player.getY(), client.player.getZ(), deathpoint,
                System.currentTimeMillis());
        entries.add(entry);
        ConfigManager.save();
        return entry;
    }

    public List<Entry> entries() {
        String worldFilter = worlds.getValue().trim();
        return entries.stream()
                .filter(entry -> oldDeathpoints.isEnabled() || !entry.deathpoint()
                        || entry.createdAt() >= sessionStartedAt)
                .filter(entry -> worldFilter.isEmpty() || entry.world().toLowerCase(java.util.Locale.ROOT)
                        .contains(worldFilter.toLowerCase(java.util.Locale.ROOT)))
                .toList();
    }

    public Entry target() {
        return entries.stream().filter(entry -> entry.id() == targetId).findFirst().orElse(null);
    }

    public void select(int id) {
        if (entries.stream().anyMatch(entry -> entry.id() == id)) {
            targetId = id;
            ConfigManager.save();
        }
    }

    public void remove(int id) {
        if (entries.removeIf(entry -> entry.id() == id)) {
            if (targetId == id) {
                targetId = -1;
            }
            ConfigManager.save();
        }
    }

    public JsonArray saveEntries() {
        JsonArray array = new JsonArray();
        for (Entry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("id", entry.id());
            object.addProperty("name", entry.name());
            object.addProperty("world", entry.world());
            object.addProperty("x", entry.x());
            object.addProperty("y", entry.y());
            object.addProperty("z", entry.z());
            object.addProperty("deathpoint", entry.deathpoint());
            object.addProperty("createdAt", entry.createdAt());
            array.add(object);
        }
        return array;
    }

    public void loadEntries(JsonArray array) {
        entries.clear();
        nextId = 1;
        targetId = -1;
        for (var element : array) {
            JsonObject object = element.getAsJsonObject();
            Entry entry = new Entry(object.get("id").getAsInt(), object.get("name").getAsString(),
                    object.get("world").getAsString(), object.get("x").getAsDouble(),
                    object.get("y").getAsDouble(), object.get("z").getAsDouble(),
                    object.has("deathpoint") && object.get("deathpoint").getAsBoolean(),
                    object.has("createdAt") ? object.get("createdAt").getAsLong() : 0L);
            entries.add(entry);
            nextId = Math.max(nextId, entry.id() + 1);
        }
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Entry target = target();
        if (target == null || ctx.player() == null) {
            width = 90;
            height = 20;
            return;
        }
        double distance = Math.sqrt(ctx.player().distanceToSqr(target.x(), target.y(), target.z()));
        String label = target.name() + " " + Math.round(distance) + "m";
        chip(ctx, label, target.deathpoint() ? 0xFFFF7777 : 0xFF75D8FF);
    }
}
