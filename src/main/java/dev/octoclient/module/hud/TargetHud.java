package dev.octoclient.module.hud;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.render.NameProtect;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import dev.octoclient.util.TargetTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Info card about the last attacked target: name, HP bar and distance. */
public class TargetHud extends HudModule {
    private final NumberSetting scale = new NumberSetting("Scale", "Widget size", 1.0, 0.7, 2.0, 0.1);
    private final BooleanSetting showDistance = new BooleanSetting("Distance", "Show distance to target", true);
    private final BooleanSetting showItem = new BooleanSetting("Held Item", "Show target's held item", true);

    private float healthAnim = 1f;
    private float appearAnim;

    public TargetHud() {
        super("Target HUD", "Info about your last attack target", 'T');
        addSetting(scale);
        addSetting(showDistance);
        addSetting(showItem);
        this.x = 76;
        this.y = 40;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        LivingEntity target = TargetTracker.get().getTarget(mc);
        if (target == null || mc.player == null) {
            appearAnim = Anim.approach(appearAnim, 0, 0.35f, tickDelta);
            return;
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);
        if (appearAnim < 0.02f) {
            return;
        }
        int alpha = (int) (Anim.clamp01(appearAnim) * 255);

        float s = scale.getFloat();
        int w = (int) (110 * s);
        int h = (int) (38 * s);
        int pad = 8;
        int barH = (int) (5 * s);
        this.width = w;
        this.height = h;

        drawPanel(ctx, (int) x, (int) y, w, h, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);

        // Name (respect NameProtect). HP, percentage and distance share the
        // right-hand side so nothing can overlap the name any more.
        String name = target.getName().getString();
        Module nameProtect = ModuleManager.get().byName("Name Protect").orElse(null);
        if (nameProtect != null && nameProtect.isEnabled() && nameProtect instanceof NameProtect np) {
            name = np.protect(name);
        }

        float hp = Math.max(0f, target.getHealth());
        float maxHp = Math.max(1f, target.getMaxHealth());
        String hpText = String.format("%.1f HP (%d%%)", hp, Math.round(100f * hp / maxHp));
        if (showDistance.isEnabled() && ctx.player() != null) {
            hpText += String.format("  %.1fm", ctx.player().distanceTo(target));
        }
        int hpWidth = ctx.textWidth(hpText);
        int nameMax = Math.max(12, w - pad * 3 - hpWidth);
        ctx.drawText(fitWidth(ctx, name, nameMax), x + pad, y + pad,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), false);
        ctx.drawText(hpText, x + w - pad - hpWidth, y + pad,
                Theme.withAlpha(Theme.TEXT_MUTED, alpha), false);

        // Animated health bar.
        healthAnim = Anim.approach(healthAnim, hp / maxHp, 0.35f, tickDelta);
        int barX = (int) (x + pad);
        int barW = w - pad * 2 - (showItem.isEnabled() ? (int) (16 * s) : 0);
        int barY = (int) (y + h - pad - barH);
        Draw.progressBar(ctx.gfx, barX, barY, barW, barH, healthAnim,
                Theme.withAlpha(Theme.DARK_700, alpha),
                Theme.withAlpha(healthColor(healthAnim), alpha));

        // Distance is part of the HP line now, so the old extra row is gone.
        // Held item in the bottom-right corner.
        if (showItem.isEnabled()) {
            ItemStack held = target.getMainHandItem();
            if (!held.isEmpty()) {
                ctx.drawItem(held, x + w - (int) (20 * s), y + h - (int) (20 * s));
            }
        }
    }

    private static String fitWidth(HudDrawContext ctx, String text, int maxWidth) {
        if (ctx.textWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (ctx.textWidth(sb.toString() + text.charAt(i) + ellipsis) > maxWidth) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb + ellipsis;
    }

    private static int healthColor(float fraction) {
        float f = Anim.clamp01(fraction);
        if (f > 0.5f) {
            // emerald -> neutral near full
            return Theme.mix(Theme.EMERALD, Theme.NEUTRAL_200, (f - 0.5f) * 2f);
        }
        // red -> emerald at half
        return Theme.mix(Theme.RED, Theme.EMERALD, f * 2f);
    }
}
