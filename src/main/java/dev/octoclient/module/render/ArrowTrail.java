package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

/** Draws a configurable world-space trail behind flying arrows. */
public class ArrowTrail extends Module {
    private static ArrowTrail instance;
    private final ModeSetting style =
            new ModeSetting("Trail Style", "Arrow trail rendering", "Particles", List.of("Particles", "Line"));
    private final NumberSetting length = new NumberSetting("Length", "Trail points retained per arrow", 12, 2, 40, 1);
    private final NumberSetting size = new NumberSetting("Size", "Trail particle size", 0.1, 0.04, 0.35, 0.01);
    private final ColorSetting color = new ColorSetting("Color", "Trail color", 0xFF61D8C8);
    private final Map<Integer, ArrayDeque<Vec3>> history = new HashMap<>();

    public ArrowTrail() {
        super("Arrowtrail", "Add a visual trail behind flying arrows", 'A');
        addSetting(style);
        addSetting(length);
        addSetting(size);
        addSetting(color);
        instance = this;
    }

    public static void submitTrails(ClientLevel level) {
        ArrowTrail module = instance;
        if (module == null || !module.isEnabled()) {
            return;
        }
        Set<Integer> visibleArrows = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof AbstractArrow arrow) || !arrow.isAlive()) {
                continue;
            }
            int id = arrow.getId();
            visibleArrows.add(id);
            ArrayDeque<Vec3> points = module.history.computeIfAbsent(id, ignored -> new ArrayDeque<>());
            points.addLast(arrow.position());
            while (points.size() > module.length.getInt()) {
                points.removeFirst();
            }
            Vec3 previous = null;
            for (Vec3 point : points) {
                if (module.style.getValue().equals("Line")) {
                    if (previous != null) {
                        Gizmos.line(previous, point, module.color.getValue(), module.size.getFloat() * 8f);
                    }
                } else {
                    Gizmos.point(point, module.color.getValue(), module.size.getFloat());
                }
                previous = point;
            }
        }
        module.history.keySet().removeIf(id -> !visibleArrows.contains(id));
    }
}
