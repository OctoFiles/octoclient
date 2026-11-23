package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class Friends extends HudModule {
    private static Friends instance;

    private final TextSetting[] names = new TextSetting[10];
    private final BooleanSetting hideOffline =
            new BooleanSetting("Hide Offline", "Show only friends present in the current world", false);

    public Friends() {
        super("Friends", "Manage a local friend list and show online status", 'F');
        addSetting(hideOffline);
        for (int i = 0; i < names.length; i++) {
            names[i] = new TextSetting("Friend " + (i + 1), "Player name for friend entry " + (i + 1), "", 16);
            addSetting(names[i]);
        }
        instance = this;
        x = 10;
        y = 285;
    }

    public static boolean addFriend(String playerName) {
        if (instance == null || playerName == null || !playerName.matches("[A-Za-z0-9_]{1,16}")) {
            return false;
        }
        for (TextSetting name : instance.names) {
            if (name.getValue().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        for (TextSetting name : instance.names) {
            if (name.getValue().isBlank()) {
                name.setValue(playerName);
                return true;
            }
        }
        return false;
    }

    public static boolean isFriend(String playerName) {
        return instance != null && playerName != null
                && java.util.Arrays.stream(instance.names).anyMatch(name -> name.getValue().equalsIgnoreCase(playerName));
    }

    public static boolean removeFriend(String playerName) {
        if (instance == null || playerName == null) {
            return false;
        }
        for (TextSetting name : instance.names) {
            if (name.getValue().equalsIgnoreCase(playerName)) {
                name.setValue("");
                return true;
            }
        }
        return false;
    }

    public static boolean toggleFriend(String playerName) {
        return isFriend(playerName) ? removeFriend(playerName) : addFriend(playerName);
    }

    public static String invitationText(String playerName, String serverAddress) {
        return serverAddress == null || serverAddress.isBlank()
                ? "Hey " + playerName + ", want to play Minecraft together?"
                : "Hey " + playerName + ", join me on " + serverAddress + "!";
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        List<String> online = ctx.mc.level == null ? List.of()
                : ctx.mc.level.players().stream().map(Player::getScoreboardName).toList();
        List<String> rows = new ArrayList<>();
        rows.add("Friends");
        for (TextSetting setting : names) {
            String name = setting.getValue().trim();
            if (name.isEmpty()) {
                continue;
            }
            boolean present = online.stream().anyMatch(value -> value.equalsIgnoreCase(name));
            if (!present && hideOffline.isEnabled()) {
                continue;
            }
            rows.add((present ? "[online] " : "[offline] ") + name);
        }
        if (rows.size() == 1) {
            rows.add("Add names in module settings");
        }

        int textWidth = rows.stream().mapToInt(ctx::textWidth).max().orElse(60);
        int rowHeight = ctx.textHeight() + 3;
        int panelWidth = textWidth + 16;
        int panelHeight = rows.size() * rowHeight + 10;
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, Math.min(cornerRadiusPx(), 8),
                0x8E97C4, 0.94f, 0, 70);
        int textY = (int) y + 5;
        for (int i = 0; i < rows.size(); i++) {
            int color = i == 0 ? 0xFFFFC857 : rows.get(i).startsWith("[online]") ? 0xFF42D392 : 0xFF9AA0AF;
            ctx.drawText(rows.get(i), x + 8, textY, color, true);
            textY += rowHeight;
        }
    }
}
