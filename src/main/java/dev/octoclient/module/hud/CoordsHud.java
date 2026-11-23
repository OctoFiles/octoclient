package dev.octoclient.module.hud;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.utility.StreamerMode;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

/** Coordinates, biome and sprint state without opening F3. */
public class CoordsHud extends HudModule {
    private final BooleanSetting showBiome = new BooleanSetting("Biome", "Show the current biome", true);
    private final BooleanSetting showSprint = new BooleanSetting("Sprint", "Show sprint status", true);

    private float appearAnim;

    public CoordsHud() {
        super("Coordinates", "XYZ, biome and sprint state", 'X');
        addSetting(showBiome);
        addSetting(showSprint);
        this.x = 4;
        this.y = 96;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        if (mc.player == null) {
            return;
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);

        StreamerMode streamer = ModuleManager.get().byName("Streamer Mode", StreamerMode.class).orElse(null);
        boolean hideXyz = streamer != null && streamer.hidesCoords();
        String xyz = hideXyz ? "XYZ: Hidden"
                : "XYZ: " + (int) Math.floor(mc.player.getX()) + " / "
                + (int) Math.floor(mc.player.getY()) + " / " + (int) Math.floor(mc.player.getZ());
        String biome = showBiome.isEnabled() ? "Biome: " + biomeName(mc) : null;
        String sprint = showSprint.isEnabled() ? "Sprint: " : null;

        int pad = 8;
        int lineH = ctx.textHeight() + 1;
        int lines = 1 + (biome == null ? 0 : 1) + (sprint == null ? 0 : 1);
        int w = ctx.textWidth(xyz);
        if (biome != null) {
            w = Math.max(w, ctx.textWidth(biome));
        }
        if (sprint != null) {
            w = Math.max(w, ctx.textWidth(sprint + "[Toggled]"));
        }
        w += pad * 2;
        int h = pad * 2 + lines * lineH - 1;
        this.width = w;
        this.height = h;

        int alpha = (int) (Anim.clamp01(appearAnim) * 255);
        drawPanel(ctx, (int) x, (int) y, w, h, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);

        float cy = y + pad;
        ctx.drawText(xyz, x + pad, cy, Theme.withAlpha(Theme.TEXT_SECONDARY, alpha), false);
        cy += lineH;
        if (biome != null) {
            ctx.drawText(biome, x + pad, cy, Theme.withAlpha(Theme.TEXT_MUTED, alpha), false);
            cy += lineH;
        }
        if (sprint != null) {
            ctx.drawText(sprint, x + pad, cy, Theme.withAlpha(Theme.TEXT_MUTED, alpha), false);
            boolean sprinting = mc.player.isSprinting();
            int tagX = (int) (x + pad + ctx.textWidth(sprint));
            ctx.drawText("[Toggled]", tagX, cy,
                    Theme.withAlpha(sprinting ? Theme.EMERALD : Theme.TEXT_FAINT, alpha), false);
        }
    }

    private static String biomeName(Minecraft mc) {
        try {
            Identifier id = mc.level.registryAccess()
                    .lookupOrThrow(Registries.BIOME)
                    .getKey(mc.level.getBiome(mc.player.blockPosition()).value());
            if (id != null) {
                String path = id.getPath();
                return Character.toUpperCase(path.charAt(0)) + path.substring(1).replace('_', ' ');
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }
}
