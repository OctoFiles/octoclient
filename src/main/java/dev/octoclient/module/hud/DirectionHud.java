package dev.octoclient.module.hud;

import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class DirectionHud extends HudModule {
    private final NumberSetting scale = new NumberSetting("Scale", "Size of the widget", 1.0, 0.7, 2.0, 0.1);

    public DirectionHud() {
        super("Direction", "Show player facing direction", 'D');
        addSetting(scale);
        this.x = 10;
        this.y = 130;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        float s = scale.getFloat();
        int pad = (int) (6 * s);
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        float yaw = Mth.wrapDegrees(mc.player.getYRot());
        String direction = getDirection(yaw);
        String text = direction + " (" + Math.round(yaw) + "°)";
        
        int textW = (int) (ctx.textWidth(text) * s);
        int textH = (int) (ctx.textHeight() * s);
        
        this.width = textW + pad * 2;
        this.height = textH + pad * 2;
        
        drawPanel(ctx, (int) x, (int) y, (int) this.width, (int) this.height, (int) (cornerRadiusPx() * s), 0x8E97C4, 0.94f, 0f, 70);
        ctx.drawTextScaled(text, x + pad, y + pad, Theme.TEXT_PRIMARY, s, false);
    }

    private String getDirection(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw >= 337.5 || yaw < 22.5) return "South";
        if (yaw >= 22.5 && yaw < 67.5) return "South-West";
        if (yaw >= 67.5 && yaw < 112.5) return "West";
        if (yaw >= 112.5 && yaw < 157.5) return "North-West";
        if (yaw >= 157.5 && yaw < 202.5) return "North";
        if (yaw >= 202.5 && yaw < 247.5) return "North-East";
        if (yaw >= 247.5 && yaw < 292.5) return "East";
        if (yaw >= 292.5 && yaw < 337.5) return "South-East";
        return "South";
    }
}
