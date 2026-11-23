package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TiersHud extends HudModule {
    private final NumberSetting range = new NumberSetting("Range", "Maximum distance to show players", 32, 4, 128, 1);
    private final NumberSetting maxPlayers = new NumberSetting("Maximum Players", "Maximum number of entries", 5, 1, 12, 1);
    private final BooleanSetting showUnranked =
            new BooleanSetting("Show Unranked", "Include players without a scoreboard prefix", false);

    public TiersHud() {
        super("Tiers", "Show nearby players' scoreboard rank or tier prefixes", 'T');
        addSetting(range);
        addSetting(maxPlayers);
        addSetting(showUnranked);
        x = 10;
        y = 185;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (ctx.mc.player == null || ctx.mc.level == null) {
            width = 70;
            height = 18;
            return;
        }
        double limit = range.getSnapped();
        List<Player> players = new ArrayList<>();
        for (var entity : ctx.mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != ctx.mc.player
                    && ctx.mc.player.distanceToSqr(player) <= limit * limit) {
                PlayerTeam team = ctx.mc.level.getScoreboard().getPlayersTeam(player.getScoreboardName());
                if (showUnranked.isEnabled() || team != null && !team.getPlayerPrefix().getString().isBlank()) {
                    players.add(player);
                }
            }
        }
        players.sort(Comparator.comparingDouble(ctx.mc.player::distanceToSqr));
        if (players.size() > maxPlayers.getInt()) {
            players = players.subList(0, maxPlayers.getInt());
        }

        List<String> rows = new ArrayList<>();
        rows.add("Nearby Tiers");
        for (Player player : players) {
            PlayerTeam team = ctx.mc.level.getScoreboard().getPlayersTeam(player.getScoreboardName());
            String tier = team == null || team.getPlayerPrefix().getString().isBlank()
                    ? "Unranked" : team.getPlayerPrefix().getString().trim();
            rows.add(tier + " " + player.getName().getString());
        }
        if (players.isEmpty()) {
            rows.add("No ranked players");
        }

        int textWidth = rows.stream().mapToInt(ctx::textWidth).max().orElse(70);
        int rowHeight = ctx.textHeight() + 3;
        int panelWidth = textWidth + 16;
        int panelHeight = rows.size() * rowHeight + 10;
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, Math.min(cornerRadiusPx(), 8),
                0x8E97C4, 0.94f, 0, 70);
        int textY = (int) y + 5;
        for (int i = 0; i < rows.size(); i++) {
            ctx.drawText(rows.get(i), x + 8, textY, i == 0 ? 0xFFFFC857 : 0xFFFFFFFF, true);
            textY += rowHeight;
        }
    }
}
