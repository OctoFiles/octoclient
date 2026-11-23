package dev.octoclient.module.hud;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.render.ToggleAnimations;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Right-aligned chips listing the enabled modules, ranked by length. */
public class ArraylistHud extends HudModule {
    private static final int[] RANK_COLORS = {
            0xFFFFFFFF,      // white
            0xFFA3A3A3,      // neutral-400
            0xFF737373,      // neutral-500
            0xFF525252,      // neutral-600
            0xFF3A3A3A,      // dark-600
    };

    private final BooleanSetting lowerCase = new BooleanSetting("Lowercase", "Render names in lowercase", false);

    private final Map<String, Float> slideAnimations = new HashMap<>();

    public ArraylistHud() {
        super("Arraylist", "Enabled modules list", 'L');
        addSetting(lowerCase);
        this.x = 210;
        this.y = 4;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        ToggleAnimations animModule = ModuleManager.get()
                .byName("Toggle Animations", ToggleAnimations.class).orElse(null);
        boolean animate = animModule == null || animModule.isEnabled();
        ToggleAnimations.Style style = animModule == null
                ? ToggleAnimations.Style.SLIDE
                : ToggleAnimations.Style.of(animModule.transitionStyle());
        float rate = 0.4f * (animModule == null ? 1f : animModule.speedMultiplier());
        boolean shadows = animModule == null || animModule.shadowsEnabled();

        List<Module> enabled = new ArrayList<>();
        for (Module module : ModuleManager.get().getModules()) {
            if (module.isEnabled() && module.getCategory() != ModuleCategory.SETTINGS
                    && module.getCategory() != ModuleCategory.HUD) {
                enabled.add(module);
            }
        }
        enabled.sort(Comparator.comparingInt((Module m) -> ctx.textWidth(m.getName())).reversed());

        slideAnimations.keySet().retainAll(
                enabled.stream().map(Module::getName).collect(java.util.stream.Collectors.toSet()));

        int lineH = ctx.textHeight() + 3;
        int padX = 8;
        int gap = 2;
        float right = x + width;
        float cy = y;
        int maxW = 0;

        for (Module module : enabled) {
            String text = lowerCase.isEnabled()
                    ? module.getName().toLowerCase(Locale.ROOT) : module.getName();
            int entryW = ctx.textWidth(text) + padX * 2 + 3;
            maxW = Math.max(maxW, entryW);

            float anim = slideAnimations.getOrDefault(module.getName(), animate ? 0f : 1f);
            anim = Anim.approach(anim, 1, rate, tickDelta);
            slideAnimations.put(module.getName(), anim);

            float eased = Anim.easeOut(anim);
            float ex;
            float scale = 1f;
            switch (style) {
                case FADE -> ex = right - entryW;
                case POP -> {
                    ex = right - entryW;
                    // Slight overshoot so chips pop past full size then settle.
                    scale = Anim.lerp(0.7f, 1f, Anim.easeOutBack(anim));
                }
                default -> ex = right - entryW + (1f - eased) * (entryW + 6);
            }
            int alpha = (int) (Anim.clamp01(anim) * 255);

            // Pop scales about the chip's right edge so it grows out of the screen edge.
            if (style == ToggleAnimations.Style.POP) {
                ctx.gfx.pose().pushMatrix();
                ctx.gfx.pose().translate(right, cy + lineH / 2f);
                ctx.gfx.pose().scale(scale, scale);
                ctx.gfx.pose().translate(-right, -(cy + lineH / 2f));
            }

            // Chip: dark rounded-left pill with a rank-colored right border.
            if (shadows) {
                Draw.roundedRect(ctx.gfx, (int) ex + 1, (int) cy + 1, entryW, lineH, 4,
                        Theme.withAlpha(0x000000, (int) (0.45f * alpha)));
            }
            Draw.roundedRect(ctx.gfx, (int) ex, (int) cy, entryW, lineH, 4,
                    Theme.withAlpha(Theme.DARK_900, (int) (0.9f * alpha)));
            int rank = Math.min(enabled.indexOf(module), RANK_COLORS.length - 1);
            ctx.fill(ex + entryW - 3, cy + 1, ex + entryW - 1, cy + lineH - 1,
                    Theme.withAlpha(RANK_COLORS[rank], alpha));
            ctx.drawText(text, ex + padX, cy + 2, Theme.withAlpha(Theme.TEXT_SECONDARY, alpha), false);

            if (style == ToggleAnimations.Style.POP) {
                ctx.gfx.pose().popMatrix();
            }
            cy += lineH + gap;
        }

        this.width = maxW;
        this.height = cy - y - gap;
    }
}
