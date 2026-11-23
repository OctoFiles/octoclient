package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

/** Smoothly rotates towards crystals, crystal bases or the crystal target. */
public class CrystalRotation extends Module implements Module.Tickable {
    private final NumberSetting range = new NumberSetting("range",
            "How far to look for a rotation target.", 6, 1, 12, 0.5);
    private final NumberSetting speed = new NumberSetting("speed",
            "How much of the remaining angle is turned per tick.", 0.45, 0.05, 1.0, 0.05);
    private final BooleanSetting vertical = new BooleanSetting("vertical",
            "Also turns up and down.", true);
    private final BooleanSetting onlyAura = new BooleanSetting("only-aura",
            "Only rotates while CrystalAura is enabled.", false);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between rotations.", 0, 0, 10, 1);

    private int timer;

    public CrystalRotation() {
        super("CrystalRotation", "Aims at crystals and crystal bases for you.", 'C');
        addSetting(range);
        addSetting(speed);
        addSetting(vertical);
        addSetting(onlyAura);
        addSetting(delay);
    }

    @Override
    protected void onDisable() {
        timer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
            return;
        }
        if (onlyAura.isEnabled() && !ModuleManager.get().byName("CrystalAura")
                .map(Module::isEnabled).orElse(false)) {
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        timer = delay.getInt();
        List<EndCrystal> crystals = CrystalUtil.crystals(client, range.getValue());
        if (!crystals.isEmpty()) {
            AuraUtil.smoothLookAt(client.player, crystals.getFirst().position().add(0, 0.5, 0),
                    speed.getValue(), vertical.isEnabled());
            return;
        }
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, range.getValue(), "Players", true, true);
        }
        if (target == null) {
            return;
        }
        BlockPos base = CrystalUtil.bestBase(client, target, range.getValue(), 0, 20, 0);
        if (base != null) {
            AuraUtil.smoothLookAt(client.player,
                    net.minecraft.world.phys.Vec3.atCenterOf(base.above()),
                    speed.getValue(), vertical.isEnabled());
        } else {
            AuraUtil.smoothLookAt(client.player, target.getBoundingBox().getCenter(),
                    speed.getValue(), vertical.isEnabled());
        }
    }
}
