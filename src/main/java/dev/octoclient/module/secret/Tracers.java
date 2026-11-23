package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.hud.Friends;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Port of Meteor's Tracers: world-space lines to entities. */
public class Tracers extends Module {
    private static Tracers instance;

    private final ModeSetting entities = new ModeSetting("entities",
            "Which entities to draw lines to.", "Players", List.of("Players", "Mobs", "All"));
    private final BooleanSetting ignoreSelf = new BooleanSetting("ignore-self",
            "Ignores your own player.", false);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Ignores friends.", false);
    private final BooleanSetting showInvisible = new BooleanSetting("show-invisible",
            "Draws lines to invisible entities.", true);
    private final ModeSetting target = new ModeSetting("target",
            "The part of the entity the line points at.", "Body", List.of("Body", "Head"));
    private final NumberSetting maxDistance = new NumberSetting("max-distance",
            "The maximum distance to draw lines.", 256, 0, 512, 8);
    private final ColorSetting playersColors = new ColorSetting("players-colors",
            "The color for players.", 0x7FCDCDCD);
    private final ColorSetting animalsColor = new ColorSetting("animals-color",
            "The color for animals.", 0x7F91FF91);
    private final ColorSetting monstersColor = new ColorSetting("monsters-color",
            "The color for monsters.", 0x7FFF9191);
    private final ColorSetting miscColor = new ColorSetting("misc-color",
            "The color for everything else.", 0x7F919191);

    public Tracers() {
        super("Tracers", "Draws lines to entities.", 'T');
        addSetting(entities);
        addSetting(ignoreSelf);
        addSetting(ignoreFriends);
        addSetting(showInvisible);
        addSetting(target);
        addSetting(maxDistance);
        addSetting(playersColors);
        addSetting(animalsColor);
        addSetting(monstersColor);
        addSetting(miscColor);
        instance = this;
    }

    public static void submitTracers(Minecraft client) {
        Tracers module = instance;
        if (module == null || !module.isEnabled() || client.level == null || client.player == null) {
            return;
        }
        Vec3 start = client.player.getEyePosition();
        double max = module.maxDistance.getValue();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player && module.ignoreSelf.isEnabled()) {
                continue;
            }
            if (entity == client.player) {
                continue;
            }
            if (!entity.isAlive()) {
                continue;
            }
            if (!module.showInvisible.isEnabled() && entity.isInvisible()) {
                continue;
            }
            double distance = client.player.distanceToSqr(entity);
            if (distance > max * max) {
                continue;
            }
            int color = module.colorFor(entity);
            if (color == 0) {
                continue;
            }
            Vec3 end = module.target.getValue().equals("Head")
                    ? new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() - 0.2, entity.getZ())
                    : entity.getBoundingBox().getCenter();
            Gizmos.line(start, end, color, 1.2f);
        }
    }

    private int colorFor(Entity entity) {
        if (entity instanceof Player player) {
            if (ignoreFriends.isEnabled() && Friends.isFriend(player.getName().getString())) {
                return 0;
            }
            if (entities.getValue().equals("Mobs")) {
                return 0;
            }
            return playersColors.getValue();
        }
        if (entities.getValue().equals("Players")) {
            return 0;
        }
        if (entity instanceof ItemEntity) {
            return 0;
        }
        if (entity instanceof Enemy) {
            return monstersColor.getValue();
        }
        if (entity instanceof Animal) {
            return animalsColor.getValue();
        }
        if (entity instanceof LivingEntity || entity instanceof Mob) {
            return miscColor.getValue();
        }
        return miscColor.getValue();
    }
}
