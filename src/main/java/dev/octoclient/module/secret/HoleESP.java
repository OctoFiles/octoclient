package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Port of Meteor's HoleESP: highlights safe holes (bedrock/obsidian/mixed). */
public class HoleESP extends Module {
    private static HoleESP instance;

    private final NumberSetting horizontalRadius = new NumberSetting("horizontal-radius",
            "The horizontal radius to search in.", 10, 0, 32, 1);
    private final NumberSetting verticalRadius = new NumberSetting("vertical-radius",
            "The vertical radius to search in.", 5, 0, 16, 1);
    private final NumberSetting minHeight = new NumberSetting("min-height",
            "The minimum height of a hole.", 3, 1, 5, 1);
    private final BooleanSetting doubles = new BooleanSetting("doubles",
            "Also highlights 2x1 holes.", true);
    private final BooleanSetting ignoreOwn = new BooleanSetting("ignore-own",
            "Ignores the hole you are standing in.", false);
    private final BooleanSetting webs = new BooleanSetting("webs",
            "Treats cobwebs as safe hole blocks.", false);
    private final ModeSetting shapeMode = new ModeSetting("shape-mode",
            "How the boxes are drawn.", "Both", List.of("Both", "Lines", "Sides"));
    private final NumberSetting height = new NumberSetting("height",
            "The height of the rendered box.", 0.2, 0, 1, 0.05);
    private final BooleanSetting topQuad = new BooleanSetting("top-quad",
            "Renders the top quad.", true);
    private final BooleanSetting bottomQuad = new BooleanSetting("bottom-quad",
            "Renders the bottom quad.", false);
    private final ColorSetting bedrockTop = new ColorSetting("bedrock-top",
            "The color of bedrock holes.", 0xC864FF00);
    private final ColorSetting obsidianTop = new ColorSetting("obsidian-top",
            "The color of obsidian holes.", 0xC8FF0000);
    private final ColorSetting mixedTop = new ColorSetting("mixed-top",
            "The color of mixed holes.", 0xC8FF7F00);

    private long cacheTime;
    private List<AABB> cache = List.of();

    private record Hole(BlockPos pos, boolean doubles, int color) {
    }

    public HoleESP() {
        super("HoleESP", "Highlights safe holes in the ground.", 'H');
        addSetting(horizontalRadius);
        addSetting(verticalRadius);
        addSetting(minHeight);
        addSetting(doubles);
        addSetting(ignoreOwn);
        addSetting(webs);
        addSetting(shapeMode);
        addSetting(height);
        addSetting(topQuad);
        addSetting(bottomQuad);
        addSetting(bedrockTop);
        addSetting(obsidianTop);
        addSetting(mixedTop);
        instance = this;
    }

    public static void submitHoles(Minecraft client) {
        HoleESP module = instance;
        if (module == null || !module.isEnabled() || client.level == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - module.cacheTime > 250L) {
            module.cacheTime = now;
            module.cache = module.scan(client);
        }
        for (AABB box : module.cache) {
            Gizmos.cuboid(box, module.style(box));
        }
    }

    private GizmoStyle style(AABB box) {
        int color = boxColor(box);
        int fill = (color & 0x00FFFFFF) | (((color >>> 24) / 2) << 24);
        return shapeMode.getValue().equals("Lines")
                ? GizmoStyle.stroke(color, 1.5f)
                : shapeMode.getValue().equals("Sides") ? GizmoStyle.fill(fill)
                : GizmoStyle.strokeAndFill(color, 1.5f, fill);
    }

    private final java.util.Map<AABB, Integer> boxColors = new java.util.HashMap<>();

    private int boxColor(AABB box) {
        Integer color = boxColors.get(box);
        return color == null ? bedrockTop.getValue() : color;
    }

    private List<AABB> scan(Minecraft client) {
        List<AABB> boxes = new ArrayList<>();
        boxColors.clear();
        BlockPos center = client.player.blockPosition();
        int hr = horizontalRadius.getInt();
        int vr = verticalRadius.getInt();
        int min = minHeight.getInt();
        Set<BlockPos> holes = new HashSet<>();
        for (int x = -hr; x <= hr; x++) {
            for (int z = -hr; z <= hr; z++) {
                for (int y = -vr; y <= vr; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    int type = holeType(client, pos, min);
                    if (type == 0) {
                        continue;
                    }
                    if (ignoreOwn.isEnabled() && (pos.getX() == center.getX() && pos.getZ() == center.getZ())) {
                        continue;
                    }
                    holes.add(pos);
                    int color = type == 1 ? bedrockTop.getValue()
                            : type == 2 ? obsidianTop.getValue() : mixedTop.getValue();
                    AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1, pos.getY() + height.getValue(), pos.getZ() + 1);
                    boxes.add(box);
                    boxColors.put(box, color);
                    if (doubles.isEnabled()) {
                        BlockPos other = pos.offset(1, 0, 0);
                        if (holes.contains(other) || holeType(client, other, min) == type) {
                            AABB doubleBox = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                    pos.getX() + 2, pos.getY() + height.getValue(), pos.getZ() + 1);
                            boxes.add(doubleBox);
                            boxColors.put(doubleBox, color);
                        }
                    }
                }
            }
        }
        return boxes;
    }

    /** @return 0 = no hole, 1 = bedrock, 2 = obsidian, 3 = mixed */
    private int holeType(Minecraft client, BlockPos pos, int min) {
        int bedrock = 0;
        int obsidian = 0;
        for (int i = 0; i < min; i++) {
            if (!isFree(client.level.getBlockState(pos.above(i)))) {
                return 0;
            }
        }
        BlockState floor = client.level.getBlockState(pos.below());
        int floorType = material(floor);
        if (floorType == 0) {
            return 0;
        }
        if (floorType == 1) {
            bedrock++;
        } else {
            obsidian++;
        }
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            for (int i = 0; i < Math.min(min, 2); i++) {
                int type = material(client.level.getBlockState(pos.relative(direction).above(i)));
                if (type == 0) {
                    return 0;
                }
                if (type == 1) {
                    bedrock++;
                } else {
                    obsidian++;
                }
            }
        }
        if (bedrock > 0 && obsidian > 0) {
            return 3;
        }
        return bedrock > 0 ? 1 : 2;
    }

    private int material(BlockState state) {
        if (state.getBlock() == Blocks.BEDROCK) {
            return 1;
        }
        if (state.getBlock() == Blocks.OBSIDIAN) {
            return 2;
        }
        if (webs.isEnabled() && state.getBlock() == Blocks.COBWEB) {
            return 2;
        }
        return 0;
    }

    private boolean isFree(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }
}
