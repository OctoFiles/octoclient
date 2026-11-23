package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Configurable world-space bounds and facing vectors for entity categories. */
public class Hitbox extends Module {
    private static Hitbox instance;
    private final CategorySettings self = new CategorySettings("Self", 0xFF55FFFF);
    private final CategorySettings players = new CategorySettings("Players", 0xFFFF5555);
    private final CategorySettings mobs = new CategorySettings("Mobs", 0xFFFFAA33);
    private final CategorySettings items = new CategorySettings("Items", 0xFF55FF55);
    private final CategorySettings projectiles = new CategorySettings("Projectiles", 0xFFFFFF55);
    private final CategorySettings experienceOrbs = new CategorySettings("Experience Orbs", 0xFF55FFAA);

    public Hitbox() {
        super("Hitbox", "Draw configurable outlines around entity categories", 'B');
        self.addTo(this);
        players.addTo(this);
        mobs.addTo(this);
        items.addTo(this);
        projectiles.addTo(this);
        experienceOrbs.addTo(this);
        instance = this;
    }

    public static void submitWorldHitboxes(Minecraft client) {
        Hitbox module = instance;
        if (module == null || !module.isEnabled() || client.level == null || client.player == null) {
            return;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            CategorySettings settings = module.settingsFor(entity, client.player);
            if (settings == null || !settings.visible.isEnabled()
                    || (entity != client.player && entity.isInvisible())) {
                continue;
            }
            boolean targeted = settings.targetHighlight.isEnabled()
                    && client.hitResult instanceof EntityHitResult hit && hit.getEntity() == entity;
            int color = targeted ? settings.facingColor.getValue() : settings.color.getValue();
            Gizmos.cuboid(entity.getBoundingBox(), GizmoStyle.stroke(color, settings.lineWidth.getFloat()));
            if (settings.lookVector.isEnabled()) {
                Vec3 start = entity.getEyePosition();
                Vec3 end = start.add(entity.getLookAngle().scale(1.5));
                Gizmos.line(start, end, settings.facingColor.getValue(), settings.lineWidth.getFloat());
            }
        }
    }

    private CategorySettings settingsFor(Entity entity, Player localPlayer) {
        if (entity == localPlayer) {
            return self;
        }
        if (entity instanceof Player) {
            return players;
        }
        if (entity instanceof ItemEntity) {
            return items;
        }
        if (entity instanceof ExperienceOrb) {
            return experienceOrbs;
        }
        if (entity instanceof Projectile) {
            return projectiles;
        }
        return entity instanceof LivingEntity ? mobs : null;
    }

    private static final class CategorySettings {
        private final BooleanSetting visible;
        private final NumberSetting lineWidth;
        private final ColorSetting color;
        private final BooleanSetting targetHighlight;
        private final BooleanSetting lookVector;
        private final ColorSetting facingColor;

        private CategorySettings(String category, int defaultColor) {
            visible = new BooleanSetting(category + " Visible", "Render " + category.toLowerCase() + " hitboxes", false);
            lineWidth = new NumberSetting(category + " Line Width", "Hitbox line thickness", 1.5, 0.5, 5, 0.25);
            color = new ColorSetting(category + " Color", "Hitbox outline color", defaultColor);
            targetHighlight = new BooleanSetting(category + " Target Highlight",
                    "Use the facing color when this entity is targeted", true);
            lookVector = new BooleanSetting(category + " Look Vector", "Draw the entity's facing direction", false);
            facingColor = new ColorSetting(category + " Facing Color", "Facing vector and target highlight color",
                    0xFFFFFFFF);
        }

        private void addTo(Hitbox module) {
            module.addSetting(visible);
            module.addSetting(lineWidth);
            module.addSetting(color);
            module.addSetting(targetHighlight);
            module.addSetting(lookVector);
            module.addSetting(facingColor);
        }
    }
}
