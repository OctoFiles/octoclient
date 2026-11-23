package dev.octoclient.module.hud;

import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.util.Anim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PingHud extends HudModule {
    private final NumberSetting widthSetting =
            new NumberSetting("Width", "Fixed widget width; zero sizes to its text", 0, 0, 400, 1);
    private final NumberSetting heightSetting =
            new NumberSetting("Height", "Fixed widget height; zero sizes to its text", 0, 0, 100, 1);
    private final NumberSetting goodPing = new NumberSetting("Good Ping Up To", "Upper bound for good ping in ms", 50, 0, 500, 5);
    private final NumberSetting mediumPing = new NumberSetting("Medium Ping Up To", "Upper bound for medium ping in ms", 100, 1, 1000, 5);
    private final ColorSetting color = new ColorSetting("Color", "Ping text color when dynamic coloring is disabled", 0xFFFFFFFF);
    private final ColorSetting goodColor = new ColorSetting("Good Ping Color", "Color for low latency", 0xFF42D392);
    private final ColorSetting mediumColor = new ColorSetting("Medium Ping Color", "Color for moderate latency", 0xFFFFC857);
    private final ColorSetting badColor = new ColorSetting("Bad Ping Color", "Color for high latency or unavailable ping", 0xFFFF5F6D);
    private final TextSetting textFormat =
            new TextSetting("Text Format", "Use {ping} for the current latency", "{ping} ms", 48);
    private final BooleanSetting dynamicPadding =
            new BooleanSetting("Dynamic Padding", "Size padding to the rendered text", true);
    private final BooleanSetting showHud =
            new BooleanSetting("Show HUD", "Show the ping widget on the in-game HUD", true);
    private final BooleanSetting usePingColor =
            new BooleanSetting("Use Ping Color", "Color the widget by latency", true);
    private final BooleanSetting showInTab =
            new BooleanSetting("Show in Tab", "Append local ping to your name in the player list", false);
    private float appearAnim;

    public PingHud() {
        super("Ping", "Show your ping to server", 'P');
        addSetting(textFormat);
        addSetting(color);
        addSetting(dynamicPadding);
        addSetting(widthSetting);
        addSetting(heightSetting);
        addSetting(showHud);
        addSetting(usePingColor);
        addSetting(showInTab);
        addSetting(goodPing);
        addSetting(mediumPing);
        addSetting(goodColor);
        addSetting(mediumColor);
        addSetting(badColor);
        this.x = 10;
        this.y = 70;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (!showHud.isEnabled()) {
            width = 0;
            height = 0;
            return;
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);
        int ping = getPing();
        String text = textFormat.getValue().replace("{ping}", ping >= 0 ? Integer.toString(ping) : "N/A");
        int textW = ctx.textWidth(text);
        int textH = ctx.textHeight();
        int pad = dynamicPadding.isEnabled() ? 6 : 0;
        int widgetWidth = widthSetting.getInt() == 0 ? textW + pad * 2 : Math.max(widthSetting.getInt(), textW);
        int widgetHeight = heightSetting.getInt() == 0 ? textH + pad * 2 : Math.max(heightSetting.getInt(), textH);
        width = widgetWidth;
        height = widgetHeight;
        drawPanel(ctx, (int) x, (int) y, widgetWidth, widgetHeight,
                cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);

        int pingColor = ping < 0 ? badColor.getValue()
                : ping <= goodPing.getInt() ? goodColor.getValue()
                : ping <= Math.max(goodPing.getInt() + 1, mediumPing.getInt())
                ? mediumColor.getValue() : badColor.getValue();
        int textColor = usePingColor.isEnabled() ? pingColor : color.getValue();
        float textX = x + (widgetWidth - textW) / 2f;
        float textY = y + (widgetHeight - textH) / 2f;
        ctx.drawText(text, textX, textY, textColor, false);
    }

    public static String tabPingSuffix() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        ClientPacketListener connection = mc.getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(mc.player.getUUID());
        PingHud module = dev.octoclient.module.ModuleManager.get()
                .byName("Ping", PingHud.class).orElse(null);
        if (module == null || !module.isEnabled() || !module.showInTab.isEnabled() || info == null) {
            return null;
        }
        return " (" + info.getLatency() + "ms)";
    }

    private int getPing() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }
}
