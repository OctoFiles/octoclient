package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.ClickCounter;
import dev.octoclient.util.Draw;
import net.minecraft.client.Minecraft;

public class CpsHud extends HudModule {
    private final NumberSetting scale = new NumberSetting("Scale", "Size of the widget", 1.0, 0.7, 2.0, 0.1);
    private final BooleanSetting showBoth = new BooleanSetting("Show Both", "Show LMB and RMB CPS", true);
    private float appearAnim;

    public CpsHud() {
        super("CPS", "Clicks per second counter", 'C');
        addSetting(scale);
        addSetting(showBoth);
        this.x = 10;
        this.y = 40;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);
        float s = scale.getFloat();
        int pad = (int) (6 * s);
        int gap = (int) (4 * s);
        
        ClickCounter clicks = ClickCounter.get();
        String leftCps = clicks.getLeftCps() + " CPS";
        String rightCps = clicks.getRightCps() + " CPS";
        
        int leftW = (int) (ctx.textWidth(leftCps) * s) + pad * 2;
        int rightW = (int) (ctx.textWidth(rightCps) * s) + pad * 2;
        int height = (int) (ctx.textHeight() * s) + pad * 2;
        
        if (showBoth.isEnabled()) {
            this.width = leftW + gap + rightW;
            this.height = height;
            drawPanel(ctx, (int) x, (int) y, (int) this.width, (int) this.height, (int) (cornerRadiusPx() * s), 0x8E97C4, 0.94f, 0f, 70);
            ctx.drawTextScaled(leftCps, x + pad, y + pad, Theme.TEXT_PRIMARY, s, false);
            // Divider
            Draw.roundedRect(ctx.gfx, (int) (x + leftW + gap/2), (int) (y + 4*s), 1, (int) (height - 8*s), (int) (2*s), Theme.BORDER_SUBTLE);
            // Right click
            ctx.drawTextScaled(rightCps, x + leftW + gap + pad, y + pad, Theme.TEXT_PRIMARY, s, false);
        } else {
            this.width = Math.max(leftW, rightW);
            this.height = height;
            drawPanel(ctx, (int) x, (int) y, (int) this.width, (int) this.height, (int) (cornerRadiusPx() * s), 0x8E97C4, 0.94f, 0f, 70);
            int totalCps = clicks.getLeftCps() + clicks.getRightCps();
            String cps = totalCps + " CPS";
            ctx.drawTextScaled(cps, x + (this.width - ctx.textWidth(cps)*s)/2, y + pad, Theme.TEXT_PRIMARY, s, false);
        }
    }
}
