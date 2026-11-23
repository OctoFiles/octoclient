package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Port of Meteor's StorageESP: highlights storage blocks and spawners. */
public class StorageESP extends Module {
    private static StorageESP instance;

    private final ModeSetting shapeMode = new ModeSetting("shape-mode",
            "How the boxes are drawn.", "Both", List.of("Both", "Lines", "Sides"));
    private final NumberSetting fillOpacity = new NumberSetting("fill-opacity",
            "The opacity of the fill.", 50, 0, 100, 1);
    private final NumberSetting width = new NumberSetting("width",
            "The width of the lines.", 1, 1, 5, 1);
    private final BooleanSetting tracers = new BooleanSetting("tracers",
            "Draws lines to the blocks.", false);
    private final ColorSetting chest = new ColorSetting("chest", "The color for chests.", 0xFFFFA000);
    private final ColorSetting trappedChest = new ColorSetting("trapped-chest",
            "The color for trapped chests.", 0xFFFF0000);
    private final ColorSetting barrel = new ColorSetting("barrel",
            "The color for barrels.", 0xFFFFA000);
    private final ColorSetting shulker = new ColorSetting("shulker",
            "The color for shulker boxes.", 0xFFFFA000);
    private final ColorSetting enderChest = new ColorSetting("ender-chest",
            "The color for ender chests.", 0xFF7800FF);
    private final ColorSetting other = new ColorSetting("other",
            "The color for other storage blocks.", 0xFF8C8C8C);

    private long cacheTime;
    private List<Marker> cache = List.of();

    private record Marker(BlockPos pos, int color) {
    }

    public StorageESP() {
        super("StorageESP", "Highlights storage blocks through walls.", 'S');
        addSetting(shapeMode);
        addSetting(fillOpacity);
        addSetting(width);
        addSetting(tracers);
        addSetting(chest);
        addSetting(trappedChest);
        addSetting(barrel);
        addSetting(shulker);
        addSetting(enderChest);
        addSetting(other);
        instance = this;
    }

    public static void submitStorage(Minecraft client) {
        StorageESP module = instance;
        if (module == null || !module.isEnabled() || client.level == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - module.cacheTime > 500L) {
            module.cacheTime = now;
            module.cache = module.scan(client);
        }
        Vec3 eyes = client.player.getEyePosition();
        for (Marker marker : module.cache) {
            AABB box = new AABB(marker.pos()).inflate(0.002);
            Gizmos.cuboid(box, module.style(marker.color()));
            if (module.tracers.isEnabled()) {
                Gizmos.line(eyes, Vec3.atCenterOf(marker.pos()), marker.color(), module.width.getFloat());
            }
        }
    }

    private GizmoStyle style(int color) {
        int alpha = (int) Math.round(fillOpacity.getValue() / 100.0 * 255);
        int fill = (color & 0x00FFFFFF) | (alpha << 24);
        return shapeMode.getValue().equals("Lines")
                ? GizmoStyle.stroke(color, width.getFloat())
                : shapeMode.getValue().equals("Sides") ? GizmoStyle.fill(fill)
                : GizmoStyle.strokeAndFill(color, width.getFloat(), fill);
    }

    private List<Marker> scan(Minecraft client) {
        List<Marker> markers = new ArrayList<>();
        int chunkRadius = Math.min(8, client.options.renderDistance().get());
        int playerChunkX = client.player.chunkPosition().x();
        int playerChunkZ = client.player.chunkPosition().z();
        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = client.level.getChunk(cx, cz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    int color = colorFor(blockEntity);
                    if (color != 0) {
                        markers.add(new Marker(blockEntity.getBlockPos(), color));
                    }
                }
            }
        }
        return markers;
    }

    private int colorFor(BlockEntity blockEntity) {
        if (blockEntity instanceof EnderChestBlockEntity) {
            return enderChest.getValue();
        }
        if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return shulker.getValue();
        }
        if (blockEntity instanceof BarrelBlockEntity) {
            return barrel.getValue();
        }
        if (blockEntity instanceof ChestBlockEntity) {
            String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(blockEntity.getBlockState().getBlock()).getPath();
            return path.contains("trapped") ? trappedChest.getValue() : chest.getValue();
        }
        if (blockEntity instanceof HopperBlockEntity || blockEntity instanceof FurnaceBlockEntity
                || blockEntity instanceof SpawnerBlockEntity || blockEntity instanceof BeaconBlockEntity
                || blockEntity instanceof BrewingStandBlockEntity) {
            return other.getValue();
        }
        return 0;
    }
}
