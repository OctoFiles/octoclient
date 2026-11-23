package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Light level HUD plus a world overlay in the spirit of lugosieben/lightoverlay:
 * marks blocks where the light level allows mobs to spawn, with carpet, cross or
 * marker rendering and per-dimension thresholds.
 */
public class LightLevelHud extends HudModule {
    private static LightLevelHud instance;

    private final NumberSetting warnBelow =
            new NumberSetting("Warn Below", "Turn red under this light level (0 = off)", 8, 0, 15, 1);
    private final BooleanSetting overlay = new BooleanSetting("Spawn Overlay",
            "Draw markers on blocks where mobs can spawn", false);
    private final ModeSetting overlayMode = new ModeSetting("Overlay Mode", "How the spawn markers look",
            "Carpet", List.of("Carpet", "Cross", "Marker"));
    private final NumberSetting radius =
            new NumberSetting("Overlay Radius", "Block radius scanned around the player", 8, 2, 24, 1);
    private final NumberSetting height =
            new NumberSetting("Overlay Height", "Vertical scan range", 6, 1, 16, 1);
    private final NumberSetting thresholdOverworld =
            new NumberSetting("Threshold Overworld", "Light level below which mobs can spawn", 1, 0, 15, 1);
    private final NumberSetting thresholdNether =
            new NumberSetting("Threshold Nether", "Nether spawn light threshold", 12, 0, 15, 1);
    private final NumberSetting thresholdEnd =
            new NumberSetting("Threshold End", "End spawn light threshold", 1, 0, 15, 1);
    private final BooleanSetting hideGreen =
            new BooleanSetting("Hide Green", "Only draw spawnable (red) blocks", true);
    private final BooleanSetting hideWater =
            new BooleanSetting("Hide Water", "Skip blocks covered by water", true);

    private long cacheTime;
    private List<Marker> cache = List.of();

    private record Marker(BlockPos pos, int color) {
    }

    public LightLevelHud() {
        super("Light Level", "Light at your feet where mobs can spawn", 'L');
        addSetting(warnBelow);
        addSetting(overlay);
        addSetting(overlayMode);
        addSetting(radius);
        addSetting(height);
        addSetting(thresholdOverworld);
        addSetting(thresholdNether);
        addSetting(thresholdEnd);
        addSetting(hideGreen);
        addSetting(hideWater);
        this.x = 4;
        this.y = 165;
        instance = this;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (ctx.mc.player == null || ctx.mc.level == null) {
            return;
        }
        int light = ctx.mc.level.getMaxLocalRawBrightness(ctx.mc.player.blockPosition());
        int limit = warnBelow.getInt();
        boolean warn = limit > 0 && light < limit;
        chip(ctx, "Light " + light, warn ? Theme.RED : Theme.TEXT_SECONDARY);
    }

    /** Collects and draws world markers; called from the LevelRenderer mixin. */
    public static void submitWorldOverlay(Minecraft client) {
        LightLevelHud module = instance;
        if (module == null || !module.isEnabled() || !module.overlay.isEnabled()
                || client.level == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - module.cacheTime > 400L) {
            module.cacheTime = now;
            module.cache = module.scan(client);
        }
        for (Marker marker : module.cache) {
            BlockPos pos = marker.pos();
            int color = marker.color();
            double x = pos.getX();
            double y = pos.getY() + 1.002;
            double z = pos.getZ();
            switch (module.overlayMode.getValue()) {
                case "Cross" -> {
                    Gizmos.line(new Vec3(x + 0.15, y, z + 0.15), new Vec3(x + 0.85, y, z + 0.85), color, 2f);
                    Gizmos.line(new Vec3(x + 0.15, y, z + 0.85), new Vec3(x + 0.85, y, z + 0.15), color, 2f);
                }
                case "Marker" -> Gizmos.point(new Vec3(x + 0.5, y, z + 0.5), color, 7f);
                default -> Gizmos.cuboid(new AABB(x + 0.05, y, z + 0.05, x + 0.95, y + 0.01, z + 0.95),
                        GizmoStyle.fill(color));
            }
        }
    }

    private List<Marker> scan(Minecraft client) {
        List<Marker> markers = new ArrayList<>();
        if (client.level == null || client.player == null) {
            return markers;
        }
        Level level = client.level;
        BlockPos center = client.player.blockPosition();
        int r = radius.getInt();
        int h = height.getInt();
        int threshold = threshold(level);
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - r, center.getY() - h,
                center.getZ() - r, center.getX() + r, center.getY() + h, center.getZ() + r)) {
            BlockPos floor = pos.immutable();
            BlockState below = level.getBlockState(floor);
            if (!below.canOcclude()) {
                continue;
            }
            if (hideWater.isEnabled() && !level.getFluidState(floor.above()).isEmpty()) {
                continue;
            }
            BlockState above = level.getBlockState(floor.above());
            if (above.canOcclude() || !level.getFluidState(floor.above()).isEmpty()) {
                continue;
            }
            int light = level.getMaxLocalRawBrightness(floor.above());
            boolean safe = light >= threshold;
            if (safe && hideGreen.isEnabled()) {
                continue;
            }
            markers.add(new Marker(floor, safe ? 0xFF46D160 : 0xFFFF4A4A));
        }
        return markers;
    }

    private int threshold(Level level) {
        if (Level.NETHER.equals(level.dimension())) {
            return thresholdNether.getInt();
        }
        if (Level.END.equals(level.dimension())) {
            return thresholdEnd.getInt();
        }
        return thresholdOverworld.getInt();
    }
}
