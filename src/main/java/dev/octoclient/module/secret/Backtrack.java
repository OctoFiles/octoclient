package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Backtrack: briefly withholds your movement packets while a target is close,
 * so the server keeps seeing your slightly older position. That widens the
 * server-side hit window ("backtrack" reach) and can make incoming hits land
 * late. The hold is short and always followed by a release tick, so the
 * position cannot drift far enough for a rubber-band kick.
 *
 * <p>The packet itself is suppressed by {@code BacktrackMixin}; this module
 * only decides when.
 */
public class Backtrack extends Module implements Module.Tickable {
    private static Backtrack instance;

    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities trigger the lag window.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "How close a target has to be for the lag window.", 6, 1, 16, 0.5);
    private final NumberSetting lagTicks = new NumberSetting("lag-ticks",
            "Movement packets withheld in a row.", 2, 1, 10, 1);
    private final NumberSetting releaseTicks = new NumberSetting("release-ticks",
            "Ticks of normal movement after each lag window.", 1, 0, 10, 1);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only lags while you hold the attack key.", false);
    private final BooleanSetting requireVisible = new BooleanSetting("require-visible",
            "Only lags for targets you can see.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never lags for friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int holding;
    private int releasing;
    private LivingEntity target;

    public Backtrack() {
        super("Backtrack", "Withholds movement packets briefly to widen the hit window.", 'B');
        addSetting(targets);
        addSetting(range);
        addSetting(lagTicks);
        addSetting(releaseTicks);
        addSetting(onlyOnClick);
        addSetting(requireVisible);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
        instance = this;
    }

    @Override
    protected void onDisable() {
        holding = 0;
        releasing = 0;
        target = null;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null) {
            holding = 0;
            releasing = 0;
            target = null;
            return;
        }
        target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        boolean active = target != null && target.isAlive()
                && (!onlyOnClick.isEnabled() || client.options.keyAttack.isDown())
                && (!requireVisible.isEnabled() || client.player.hasLineOfSight(target));
        if (!active) {
            holding = 0;
            releasing = 0;
            return;
        }
        if (releasing > 0) {
            releasing--;
            return;
        }
        holding++;
        if (holding > lagTicks.getInt()) {
            holding = 0;
            releasing = releaseTicks.getInt();
        }
    }

    /** True while the local position packet must be withheld. */
    public static boolean hold() {
        Backtrack module = instance;
        if (module == null || !module.isEnabled() || module.holding <= 0) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        return client.player != null && !client.player.isPassenger()
                && !client.player.isFallFlying();
    }
}
