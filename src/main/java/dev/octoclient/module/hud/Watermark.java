package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import dev.octoclient.util.FpsCounter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/** "OCTOCLIENT" logo tile, version badge and FPS / ping line. */
public class Watermark extends HudModule {
    private final BooleanSetting showStats = new BooleanSetting("Stats", "Show FPS and ping line", true);
    private float appearAnim;

    public Watermark() {
        super("Watermark", "Client branding with FPS / ping", 'O');
        addSetting(showStats);
        this.x = 4;
        this.y = 4;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);
        int alpha = (int) (Anim.clamp01(appearAnim) * 255);

        int pad = 8;
        int tile = 24;
        int gap = 8;

        String name = "OCTOCLIENT";
        String badge = "LEGIT v1.0.0";
        int nameW = ctx.textWidth(name);
        int badgeW = ctx.textWidth(badge) + 8;
        String stats = statsLine(ctx);

        int textW = nameW + 6 + badgeW;
        if (stats != null) {
            textW = Math.max(textW, ctx.textWidth(stats));
        }
        int w = pad * 2 + tile + gap + textW;
        int h = stats == null ? pad * 2 + tile : pad * 2 + Math.max(tile, ctx.textHeight() * 2 + 3);
        this.width = w;
        this.height = h;

        drawPanel(ctx, (int) x, (int) y, w, h, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);

        // Logo tile.
        int tx = (int) x + pad;
        int ty = (int) y + (h - tile) / 2;
        Draw.tile(ctx.gfx, tx, ty, tile, 7, Theme.DARK_950, Theme.DARK_600);
        ctx.drawText("O", tx + (tile - ctx.textWidth("O")) / 2f, ty + (tile - ctx.textHeight()) / 2f + 1,
                Theme.TEXT_PRIMARY, false);

        // Name + version badge.
        float textX = tx + tile + gap;
        float nameY = stats == null ? y + (h - ctx.textHeight()) / 2f : y + pad - 1;
        ctx.drawText(name, textX, nameY, Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), false);
        int bx = (int) (textX + nameW + 6);
        int by = (int) nameY - 1;
        Draw.roundedRectBorder(ctx.gfx, bx, by, badgeW, ctx.textHeight() + 2, 4,
                Theme.withAlpha(Theme.DARK_700, alpha), Theme.withAlpha(Theme.DARK_800, alpha));
        ctx.drawText(badge, bx + 4, nameY, Theme.withAlpha(Theme.TEXT_MUTED, alpha), false);

        // FPS / ping line.
        if (stats != null) {
            ctx.drawText(stats, textX, nameY + ctx.textHeight() + 3,
                    Theme.withAlpha(Theme.TEXT_MUTED, alpha), false);
        }
    }

    private String statsLine(HudDrawContext ctx) {
        if (!showStats.isEnabled()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(FpsCounter.get().getFps()).append(" FPS");
        int ping = ping();
        if (ping >= 0) {
            sb.append("  |  ").append(ping).append(" ms");
        }
        return sb.toString();
    }

    private int ping() {
        ClientPacketListener connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (connection == null || player == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(player.getUUID());
        return info == null ? -1 : info.getLatency();
    }
}
