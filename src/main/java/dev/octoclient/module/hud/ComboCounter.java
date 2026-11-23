package dev.octoclient.module.hud;

import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.TextSetting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/** Consecutive hits on living targets, ported from NoRisk's combo counter. */
public class ComboCounter extends HudModule {
    private final BooleanSetting background = new BooleanSetting("Background", "Show the counter panel", true);
    private final NumberSetting corners = new NumberSetting("Corners", "Counter panel corner radius", 10, 0, 24, 1);
    private final TextSetting text = new TextSetting("Text", "Text template; supports {combo}", "Combo \u00d7{combo}", 64);
    private final ColorSetting color = new ColorSetting("Color", "Combo text color", 0xFFFFFFFF);
    private final BooleanSetting dynamicPadding = new BooleanSetting("Dynamic Padding", "Fit the panel to the text", true);
    private final NumberSetting panelWidth = new NumberSetting("Width", "Fixed panel width when dynamic padding is off", 100, 40, 300, 2);
    private final NumberSetting panelHeight = new NumberSetting("Height", "Fixed panel height when dynamic padding is off", 20, 12, 80, 1);
    private final NumberSetting activeTime =
            new NumberSetting("Active Time", "Seconds the combo stays on screen", 2.0, 0.5, 5.0, 0.5);

    private int combo;
    private long lastHit;

    public ComboCounter() {
        super("Combo", "Hits in a row on a target", 'K');
        addSetting(background);
        addSetting(corners);
        addSetting(text);
        addSetting(color);
        addSetting(dynamicPadding);
        addSetting(panelWidth);
        addSetting(panelHeight);
        addSetting(activeTime);
        this.x = 4;
        this.y = 180;
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (isEnabled() && player != null && entity instanceof LivingEntity && entity != player) {
                long now = System.currentTimeMillis();
                combo = now - lastHit <= 1500L ? combo + 1 : 1;
                lastHit = now;
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    protected void onDisable() {
        combo = 0;
        lastHit = 0;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        long elapsed = System.currentTimeMillis() - lastHit;
        if (combo <= 0 || elapsed > (long) (activeTime.getFloat() * 1000f)) {
            combo = 0;
            return;
        }
        String label = text.getValue().replace("{combo}", Integer.toString(combo));
        int padX = 8;
        int padY = 4;
        int naturalWidth = ctx.textWidth(label) + padX * 2;
        int naturalHeight = ctx.textHeight() + padY * 2;
        int panelW = dynamicPadding.isEnabled() ? naturalWidth : panelWidth.getInt();
        int panelH = dynamicPadding.isEnabled() ? naturalHeight : panelHeight.getInt();
        width = panelW;
        height = panelH;
        if (background.isEnabled()) {
            drawPanel(ctx, (int) x, (int) y, panelW, panelH, corners.getInt(), 0x8E97C4, 0.94f, 0, 70);
        }
        float textX = x + (panelW - ctx.textWidth(label)) / 2f;
        float textY = y + (panelH - ctx.textHeight()) / 2f;
        ctx.drawText(label, textX, textY, color.getValue(), false);
    }
}
