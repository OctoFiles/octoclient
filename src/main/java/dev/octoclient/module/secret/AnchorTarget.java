package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Locks one anchor target so the other Anchor* modules all attack the same
 * entity. The lock is announced in the action bar when enabled.
 */
public class AnchorTarget extends Module implements Module.Tickable {
    private static AnchorTarget instance;

    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities can be locked.", "Players", List.of("Players", "Mobs", "All"));
    private final ModeSetting priority = new ModeSetting("priority",
            "How the locked target is chosen.", "Closest",
            List.of("Closest", "LowestHealth", "Angle"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum target distance.", 6, 1, 16, 0.5);
    private final BooleanSetting announce = new BooleanSetting("announce",
            "Shows the locked target in the action bar.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never locks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private LivingEntity locked;
    private int announceTicks;

    public AnchorTarget() {
        super("AnchorTarget", "Locks a single target for all anchor modules.", 'A');
        addSetting(targets);
        addSetting(priority);
        addSetting(range);
        addSetting(announce);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
        instance = this;
    }

    @Override
    protected void onDisable() {
        locked = null;
        announceTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            locked = null;
            return;
        }
        List<LivingEntity> list = AuraUtil.targets(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (list.isEmpty()) {
            locked = null;
            return;
        }
        locked = switch (priority.getValue()) {
            case "LowestHealth" -> list.stream()
                    .min(java.util.Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
            case "Angle" -> list.stream()
                    .min(java.util.Comparator.comparingDouble(e -> AuraUtil.angleTo(client.player, e)))
                    .orElse(null);
            default -> list.getFirst();
        };
        if (announce.isEnabled() && locked != null && --announceTicks <= 0) {
            announceTicks = 10;
            client.player.sendOverlayMessage(Component.literal(
                    "Anchor target: " + locked.getName().getString()
                            + " (" + Math.round(locked.getHealth()) + " HP)"));
        }
    }

    /** The locked target, or {@code null} when this module is off. */
    static LivingEntity lockedTarget(Minecraft client) {
        AnchorTarget module = instance;
        return module != null && module.isEnabled() ? module.locked : null;
    }
}
