package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.cosmetics.ColoredName;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.network.CosmeticLoadout;
import dev.octoclient.network.CosmeticSyncClient;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/** Customizes nearby player labels; health can be delegated to Health Indicator. */
public class Nametags extends Module {
    private static final Identifier ICON_FONT = Identifier.fromNamespaceAndPath("octoclient", "nametag_icons");
    private static Nametags instance;

    private final NumberSetting range = new NumberSetting("Range", "Maximum nametag distance", 64.0, 8.0, 128.0, 1.0);
    private final BooleanSetting showHealth = new BooleanSetting("Health", "Append numeric health", true);
    private final BooleanSetting showDistance = new BooleanSetting("Distance", "Append distance from the camera", false);
    private final BooleanSetting showSelf = new BooleanSetting("Own Nametag",
            "Show your own name tag above your head (visible in third person)", false);
    private final ColorSetting nameColor = new ColorSetting("Name Color", "Player name color", 0xFFFFFFFF);
    private final ModeSetting animation = new ModeSetting("Animation", "Movement played on player name tags",
            "Bounce", List.of("None", "Bounce", "Float", "Wave"));
    private final NumberSetting animationHeight = new NumberSetting("Animation Height",
            "How far the name tag travels", 0.18, 0.02, 0.6, 0.01);

    /** Resolved once instead of a module lookup per player per frame. */
    private static HealthIndicator healthIndicatorModule;

    public Nametags() {
        super("Nametags", "Customize player name labels and health information", 'N');
        addSetting(range);
        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(showSelf);
        addSetting(nameColor);
        addSetting(animation);
        addSetting(animationHeight);
        instance = this;
    }

    public static boolean isEnabledForSync() { return instance != null && instance.isEnabled(); }
    public static boolean showHealthForSync() { return instance == null || instance.showHealth.isEnabled(); }
    public static boolean showDistanceForSync() { return instance != null && instance.showDistance.isEnabled(); }
    public static int colorForSync() { return instance == null ? 0xFFFFFFFF : instance.nameColor.getRgb(); }
    public static String animationForSync() { return instance == null ? "None" : instance.animation.getValue(); }
    public static float animationHeightForSync() { return instance == null ? 0.18f : instance.animationHeight.getFloat(); }

    /**
     * Offset for one name tag, in blocks, or {@code null} when no animation is
     * selected. Each entity gets its own phase, so a group of players does not
     * bob in lockstep.
     */
    public static Vec3 animationOffset(LivingEntity entity) {
        if (instance == null || !instance.isEnabled() || !(entity instanceof Player)
                || entity == Minecraft.getInstance().player) {
            return null;
        }
        CosmeticLoadout remote = entity == Minecraft.getInstance().player ? null
                : CosmeticSyncClient.remoteLoadout(entity.getUUID());
        boolean shared = remote != null && remote.nametagEnabled();
        String mode = shared ? remote.nametagAnimation() : instance.animation.getValue();
        if (mode == null || "None".equals(mode)) {
            return null;
        }
        float amount = shared ? remote.nametagAnimationHeight() : instance.animationHeight.getFloat();
        if (amount <= 0f) {
            return null;
        }
        double t = System.nanoTime() / 1.0e9 + entity.getId() * 0.37;
        return switch (mode) {
            case "Bounce" -> new Vec3(0.0, Math.abs(Math.sin(t * 2.4)) * amount, 0.0);
            case "Float" -> new Vec3(0.0, Math.sin(t * 1.5) * amount * 0.5, 0.0);
            case "Wave" -> new Vec3(Math.sin(t * 1.8) * amount * 0.6, 0.0, 0.0);
            default -> null;
        };
    }

    private static boolean healthIndicatorActive() {
        if (healthIndicatorModule == null) {
            healthIndicatorModule = ModuleManager.get()
                    .byName("Health Indicator", HealthIndicator.class).orElse(null);
        }
        return healthIndicatorModule != null && healthIndicatorModule.isEnabled();
    }

    public static boolean shouldShow(LivingEntity entity, double distanceToCameraSq) {
        if (instance == null || !instance.isEnabled() || !(entity instanceof Player)) {
            return false;
        }
        // The local player is hidden from the vanilla name tag pass; the Own
        // Nametag setting opts back in, which is when the tag is actually
        // renderable (third person, since the camera entity is not drawn in
        // first person).
        if (entity == Minecraft.getInstance().player && !instance.showSelf.isEnabled()) {
            return false;
        }
        return distanceToCameraSq <= instance.range.getSnapped() * instance.range.getSnapped();
    }

    public static Component customize(Player player, Component name) {
        if (name == null) {
            return name;
        }
        CosmeticLoadout remote = player == Minecraft.getInstance().player
                ? null : CosmeticSyncClient.remoteLoadout(player.getUUID());
        boolean shared = remote != null && remote.nametagEnabled();
        if (!shared && (instance == null || !instance.isEnabled())) {
            return name;
        }
        boolean useHealth = shared ? remote.nametagHealth() : instance.showHealth.isEnabled();
        boolean useDistance = shared ? remote.nametagDistance() : instance.showDistance.isEnabled();
        int color = shared ? remote.nametagColor() : instance.nameColor.getRgb();
        Component result = !shared && ColoredName.appliesTo(player)
                ? name.copy()
                : name.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        boolean iconAtlas = BadOptimization.usesIconAtlas();
        if (iconAtlas) {
            var playerIcon = Component.literal("\ue000 ")
                    .withStyle(Style.EMPTY.withFont(new FontDescription.Resource(ICON_FONT))
                            .withColor(TextColor.fromRgb(color)));
            result = playerIcon.append(result);
        }
        if (useHealth && !healthIndicatorActive()) {
            if (iconAtlas) {
                Component healthIcon = Component.literal(" \ue001")
                        .withStyle(Style.EMPTY.withFont(new FontDescription.Resource(ICON_FONT))
                                .withColor(TextColor.fromRgb(0xFF5F6D)));
                result = result.copy().append(healthIcon)
                        .append(Component.literal(String.format(Locale.ROOT, " %.1f", player.getHealth())));
            } else {
                result = result.copy().append(Component.literal(String.format(Locale.ROOT, " %.1f❤", player.getHealth())));
            }
        }
        if (useDistance) {
            var camera = Minecraft.getInstance().getCameraEntity();
            if (camera != null) {
                result = result.copy().append(Component.literal(
                        String.format(Locale.ROOT, " %.1fm", Math.sqrt(player.distanceToSqr(camera)))));
            }
        }
        return result;
    }
}
