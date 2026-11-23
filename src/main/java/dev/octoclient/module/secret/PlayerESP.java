package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.hud.Friends;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Outlines players through walls, with optional tracers and names. */
public class PlayerESP extends Module {
    private static PlayerESP instance;

    private final ModeSetting shapeMode = new ModeSetting("shape-mode",
            "How the boxes are drawn.", "Both", List.of("Both", "Lines", "Sides"));
    private final NumberSetting fillOpacity = new NumberSetting("fill-opacity",
            "The opacity of the fill.", 35, 0, 100, 1);
    private final NumberSetting width = new NumberSetting("width",
            "The width of the lines.", 1.5, 1, 5, 0.5);
    private final NumberSetting maxDistance = new NumberSetting("max-distance",
            "The maximum distance to highlight players.", 128, 0, 512, 8);
    private final BooleanSetting tracers = new BooleanSetting("tracers",
            "Draws lines from your eyes to each player.", false);
    private final BooleanSetting showNames = new BooleanSetting("show-names",
            "Draws the player name above the box.", false);
    private final BooleanSetting showInvisible = new BooleanSetting("show-invisible",
            "Highlights invisible players too.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Skips friends entirely.", false);
    private final ColorSetting playersColor = new ColorSetting("players-color",
            "The color for players.", 0xFF3FA9FF);
    private final ColorSetting friendsColor = new ColorSetting("friends-color",
            "The color for friends.", 0xFF55FF55);

    public PlayerESP() {
        super("PlayerESP", "Outlines players through walls.", 'E');
        addSetting(shapeMode);
        addSetting(fillOpacity);
        addSetting(width);
        addSetting(maxDistance);
        addSetting(tracers);
        addSetting(showNames);
        addSetting(showInvisible);
        addSetting(ignoreFriends);
        addSetting(playersColor);
        addSetting(friendsColor);
        instance = this;
    }

    /** Called from the level-render hook next to the other world gizmos. */
    public static void submitPlayers(Minecraft client) {
        PlayerESP module = instance;
        if (module == null || !module.isEnabled() || client.level == null || client.player == null) {
            return;
        }
        double max = module.maxDistance.getValue();
        for (AbstractClientPlayer player : client.level.players()) {
            if (player == client.player || player == client.getCameraEntity()) {
                continue;
            }
            if (!player.isAlive() || player.isRemoved()) {
                continue;
            }
            if (!module.showInvisible.isEnabled() && player.isInvisible()) {
                continue;
            }
            double distance = client.player.distanceToSqr(player);
            if (distance > max * max) {
                continue;
            }
            boolean friend = Friends.isFriend(player.getName().getString());
            if (friend && module.ignoreFriends.isEnabled()) {
                continue;
            }
            int color = friend ? module.friendsColor.getValue() : module.playersColor.getValue();
            AABB box = player.getBoundingBox().inflate(0.05);
            Gizmos.cuboid(box, module.style(color));
            if (module.tracers.isEnabled()) {
                Gizmos.line(client.player.getEyePosition(), box.getCenter(), color, module.width.getFloat());
            }
            if (module.showNames.isEnabled()) {
                Gizmos.billboardTextOverMob(player, 0, player.getName().getString(), color, 0.9f);
            }
        }
    }

    private GizmoStyle style(int color) {
        int alpha = (int) Math.round(fillOpacity.getValue() / 100.0 * 255);
        int fill = (color & 0x00FFFFFF) | (alpha << 24);
        return switch (shapeMode.getValue()) {
            case "Lines" -> GizmoStyle.stroke(color, width.getFloat());
            case "Sides" -> GizmoStyle.fill(fill);
            default -> GizmoStyle.strokeAndFill(color, width.getFloat(), fill);
        };
    }
}
