package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.ClickCounter;
import dev.octoclient.util.Draw;
import dev.octoclient.util.FpsCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;

/** FPS / CPS / Ping pill. */
public class StatsHud extends HudModule {
    private final ModeSetting mode = new ModeSetting("Mode", "Layout style",
            "Pill", List.of("Pill", "Plain"));
    private final BooleanSetting showFps = new BooleanSetting("FPS", "Show frames per second", true);
    private final BooleanSetting showCps = new BooleanSetting("CPS", "Show clicks per second", true);
    private final BooleanSetting showPing = new BooleanSetting("Ping", "Show server latency", true);

    private float appearAnim;

    public StatsHud() {
        super("Stats", "FPS, CPS and ping counter", 'S');
        addSetting(mode);
        addSetting(showFps);
        addSetting(showCps);
        addSetting(showPing);
        this.x = 4;
        this.y = 40;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        if (mc.player == null) {
            return;
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);

        StringBuilder sb = new StringBuilder();
        if (showFps.isEnabled()) {
            sb.append(FpsCounter.get().getFps()).append(" FPS");
        }
        if (showCps.isEnabled()) {
            ClickCounter clicks = ClickCounter.get();
            if (!sb.isEmpty()) {
                sb.append("  |  ");
            }
            sb.append(clicks.getLeftCps()).append(" CPS");
            if (clicks.getRightCps() > 0) {
                sb.append(" (").append(clicks.getRightCps()).append(")");
            }
        }
        if (showPing.isEnabled()) {
            int ping = ping(mc);
            if (ping >= 0) {
                if (!sb.isEmpty()) {
                    sb.append("  |  ");
                }
                sb.append(ping).append(" ms");
            }
        }
        if (sb.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }
        String text = sb.toString();
        int pad = 8;
        int w = ctx.textWidth(text) + pad * 2;
        int h = ctx.textHeight() + pad * 2 - 2;
        this.width = w;
        this.height = h;

        int alpha = (int) (Anim.clamp01(appearAnim) * 255);
        if ("Pill".equals(mode.getValue())) {
            drawPanel(ctx, (int) x, (int) y, w, h, Math.min(cornerRadiusPx(), h / 2), 0x8E97C4, 0.94f, 0f, 70);
        }
        ctx.drawText(text, x + pad, y + pad - 1, Theme.withAlpha(Theme.TEXT_SECONDARY, alpha), false);
    }

    private static int ping(Minecraft mc) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }
}
