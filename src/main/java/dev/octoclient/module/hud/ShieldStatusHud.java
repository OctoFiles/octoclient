package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.ui.Theme;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Local shield cooldown indicator with optional gradual enabled/disabled color transition. */
public class ShieldStatusHud extends HudModule {
    private static ShieldStatusHud instance;
    private static final ThreadLocal<ShieldRenderContext> RENDER_CONTEXT = new ThreadLocal<>();

    private final ColorSetting readyColor = new ColorSetting("Ready Color", "Shield is ready to block", 0xFF42D392);
    private final ColorSetting disabledColor = new ColorSetting("Disabled Color", "Shield is on cooldown", 0xFFFF5F6D);
    private final BooleanSetting interpolation =
            new BooleanSetting("Interpolation", "Blend colors as the shield recharges", true);
    private final ModeSetting display =
            new ModeSetting("Display", "Show cooldown as text or a progress bar", "Text", List.of("Text", "Bar"));

    public ShieldStatusHud() {
        super("Shield Status", "Show shield block cooldown status", 'S');
        addSetting(readyColor);
        addSetting(disabledColor);
        addSetting(interpolation);
        addSetting(display);
        instance = this;
        x = 4;
        y = 82;
    }

    public static void beginFirstPersonRender(LivingEntity entity, ItemStack stack, ItemDisplayContext context) {
        if (instance != null && instance.isEnabled() && entity instanceof Player player
                && stack.is(Items.SHIELD) && isFirstPerson(context)) {
            RENDER_CONTEXT.set(new ShieldRenderContext(player, stack));
        } else {
            RENDER_CONTEXT.remove();
        }
    }

    public static void endFirstPersonRender() {
        RENDER_CONTEXT.remove();
    }

    public static int tintShieldBase(int originalColor) {
        ShieldRenderContext context = RENDER_CONTEXT.get();
        ShieldStatusHud module = instance;
        if (context == null || module == null || !module.isEnabled()) {
            return originalColor;
        }
        float cooldown = context.player().getCooldowns().getCooldownPercent(context.stack(), 0f);
        float readiness = 1f - cooldown;
        int status = module.interpolation.isEnabled()
                ? blend(module.disabledColor.getValue(), module.readyColor.getValue(), readiness)
                : cooldown > 0f ? module.disabledColor.getValue() : module.readyColor.getValue();
        float opacity = ((status >>> 24) & 0xFF) / 255f;
        int r = tintChannel((originalColor >>> 16) & 0xFF, (status >>> 16) & 0xFF, opacity);
        int g = tintChannel((originalColor >>> 8) & 0xFF, (status >>> 8) & 0xFF, opacity);
        int b = tintChannel(originalColor & 0xFF, status & 0xFF, opacity);
        return (originalColor & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        var player = ctx.player();
        if (player == null) {
            return;
        }
        ItemStack shield = player.getMainHandItem().is(Items.SHIELD) ? player.getMainHandItem()
                : player.getOffhandItem().is(Items.SHIELD) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (shield.isEmpty()) {
            return;
        }
        float cooldown = player.getCooldowns().getCooldownPercent(shield, tickDelta);
        float readiness = 1f - cooldown;
        int currentColor = interpolation.isEnabled()
                ? blend(disabledColor.getValue(), readyColor.getValue(), readiness)
                : cooldown > 0f ? disabledColor.getValue() : readyColor.getValue();
        String text = cooldown > 0f ? "Shield " + Math.round(readiness * 100f) + "%" : "Shield Ready";
        int textWidth = ctx.textWidth(text);
        int pad = 7;
        int panelWidth = textWidth + pad * 2;
        int panelHeight = display.getValue().equals("Bar") ? 24 : ctx.textHeight() + 8;
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x - pad, (int) y - 4, panelWidth, panelHeight, cornerRadiusPx(),
                0x8E97C4, 0.94f, 0, 60);
        ctx.drawText(text, x, y, currentColor, false);
        if (display.getValue().equals("Bar")) {
            ctx.fill(x - pad, y + 13, x - pad + panelWidth * readiness, y + 16,
                    currentColor);
        }
    }

    private static int blend(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(((from >>> 16) & 0xFF) * (1f - t) + ((to >>> 16) & 0xFF) * t);
        int g = Math.round(((from >>> 8) & 0xFF) * (1f - t) + ((to >>> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1f - t) + (to & 0xFF) * t);
        int a = Math.round(((from >>> 24) & 0xFF) * (1f - t) + ((to >>> 24) & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int tintChannel(int original, int tint, float opacity) {
        float tinted = original * tint / 255f;
        return Math.round(original * (1f - opacity) + tinted * opacity);
    }

    private static boolean isFirstPerson(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private record ShieldRenderContext(Player player, ItemStack stack) {
    }
}
