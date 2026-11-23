package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;

import java.util.List;

public class Tips extends Module {
    private static final List<String> TIPS = List.of(
            "Use a water bucket to break a fall.",
            "Sneaking prevents you from walking off most edges.",
            "Torches can keep hostile mobs from spawning nearby.",
            "A shield can block many incoming attacks.",
            "Beds set your respawn point outside the Nether and End.",
            "You can repair and combine gear in an anvil.",
            "Crops grow faster when hydrated farmland is nearby.",
            "Check your coordinates with F3 when exploring.",
            "A boat is useful for crossing water and carrying mobs.",
            "Smelt raw ores for their ingots."
    );
    private static Tips instance;

    private final ModeSetting order =
            new ModeSetting("Tip Order", "Choose sequential or changing tips", "Random", List.of("Random", "Cycle"));
    private final NumberSetting interval =
            new NumberSetting("Change Interval", "Seconds between tip changes", 10, 3, 60, 1);

    public Tips() {
        super("Tips", "Show gameplay tips on resource-loading screens", 'T');
        addSetting(order);
        addSetting(interval);
        instance = this;
    }

    public static String currentTip() {
        if (instance == null || !instance.isEnabled()) {
            return null;
        }
        long slot = System.currentTimeMillis() / (instance.interval.getInt() * 1000L);
        int index = instance.order.getValue().equals("Cycle")
                ? (int) Math.floorMod(slot, TIPS.size())
                : new java.util.Random(slot).nextInt(TIPS.size());
        return TIPS.get(index);
    }
}
