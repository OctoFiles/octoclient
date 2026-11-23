package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Port of Meteor's Jesus: treats water and lava as walkable surfaces. */
public class Jesus extends Module implements Module.Tickable {
    private static Jesus instance;

    private final BooleanSetting powderSnow = new BooleanSetting("powder-snow",
            "Walk on powder snow.", true);
    private final BooleanSetting ncpBypass = new BooleanSetting("ncp-bypass",
            "Whether to apply a bypass for NCP.", false);
    private final BooleanSetting slowDown = new BooleanSetting("slow-down",
            "Further movement option to try bypassing NCP", false);

    private final ModeSetting waterMode = new ModeSetting("Water Mode",
            "How to treat the water.", "Solid", List.of("Solid", "Ignore"));
    private final BooleanSetting dipIfBurning = new BooleanSetting("dip-if-burning",
            "Lets you go into the water when you are burning.", true);
    private final BooleanSetting dipOnSneakWater = new BooleanSetting("dip-on-sneak",
            "Lets you go into the water when your sneak key is held.", true);
    private final BooleanSetting dipOnFallWater = new BooleanSetting("dip-on-fall",
            "Lets you go into the water when you fall over a certain height.", true);
    private final NumberSetting dipFallHeightWater = new NumberSetting("dip-fall-height",
            "The fall height at which you will go into the water.", 4, 1, 255, 1);

    private final ModeSetting lavaMode = new ModeSetting("Lava Mode",
            "How to treat the lava.", "Solid", List.of("Solid", "Ignore"));
    private final BooleanSetting dipIfFireResistant = new BooleanSetting("dip-if-resistant",
            "Lets you go into the lava if you have Fire Resistance effect.", true);
    private final BooleanSetting dipOnSneakLava = new BooleanSetting("dip-on-sneak (Lava)",
            "Lets you go into the lava when your sneak key is held.", true);
    private final BooleanSetting dipOnFallLava = new BooleanSetting("dip-on-fall (Lava)",
            "Lets you go into the lava when you fall over a certain height.", true);
    private final NumberSetting dipFallHeightLava = new NumberSetting("dip-fall-height (Lava)",
            "The fall height at which you will go into the lava.", 4, 1, 255, 1);

    private int ascending;
    private int swimmingTicks;

    public Jesus() {
        super("Jesus", "Walk on water and lava like it is solid.", 'J');
        addSetting(powderSnow);
        addSetting(ncpBypass);
        addSetting(slowDown);
        addSetting(waterMode);
        addSetting(dipIfBurning);
        addSetting(dipOnSneakWater);
        addSetting(dipOnFallWater);
        addSetting(dipFallHeightWater);
        addSetting(lavaMode);
        addSetting(dipIfFireResistant);
        addSetting(dipOnSneakLava);
        addSetting(dipOnFallLava);
        addSetting(dipFallHeightLava);
        instance = this;
    }

    @Override
    protected void onEnable() {
        ascending = 0;
        swimmingTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        if (player.isVisuallySwimming()) {
            return;
        }
        if (player.isInWater() && !waterShouldBeSolid(client)) {
            return;
        }
        if (player.isInLava() && !lavaShouldBeSolid(client)) {
            return;
        }

        Entity moving = player.isPassenger() && player.getVehicle() != null
                ? player.getVehicle() : player;

        if (moving.isInWater() || moving.isInLava()) {
            Vec3 delta = moving.getDeltaMovement();
            moving.setDeltaMovement(delta.x, 0.11, delta.z);
            ascending = 0;
            return;
        }

        BlockPos below = moving.blockPosition().below();
        BlockState belowState = client.level.getBlockState(below);
        boolean waterLogged = belowState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);

        if (ascending == 0) {
            Vec3 delta = moving.getDeltaMovement();
            moving.setDeltaMovement(delta.x, 0.11, delta.z);
        } else if (ascending == 1 && (belowState.getBlock() == Blocks.WATER
                || belowState.getBlock() == Blocks.LAVA || waterLogged)) {
            Vec3 delta = moving.getDeltaMovement();
            moving.setDeltaMovement(delta.x, 0, delta.z);
        }
        ascending++;
    }

    /** Called from the block-ledge/fluid mixin. */
    public static boolean canWalkOnFluid(FluidState state, Minecraft client) {
        Jesus module = instance;
        if (module == null || !module.isEnabled() || client.player == null
                || client.player.isSwimming()) {
            return false;
        }
        if (isWater(state)) {
            return module.waterMode.getValue().equals("Solid") && module.waterShouldBeSolid(client);
        }
        if (isLava(state)) {
            return module.lavaMode.getValue().equals("Solid") && module.lavaShouldBeSolid(client);
        }
        return false;
    }

    public static boolean canWalkOnPowderSnow(Minecraft client) {
        return instance != null && instance.isEnabled() && instance.powderSnow.isEnabled();
    }

    private static boolean isWater(FluidState state) {
        return state.getType() == Fluids.WATER || state.getType() == Fluids.FLOWING_WATER;
    }

    private static boolean isLava(FluidState state) {
        return state.getType() == Fluids.LAVA || state.getType() == Fluids.FLOWING_LAVA;
    }

    private boolean waterShouldBeSolid(Minecraft client) {
        Player player = client.player;
        if (player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        if (dipIfBurning.isEnabled() && player.isOnFire()) {
            return false;
        }
        if (dipOnSneakWater.isEnabled() && client.options.keyShift.isDown()) {
            return false;
        }
        if (dipOnFallWater.isEnabled() && player.fallDistance > dipFallHeightWater.getValue()) {
            return false;
        }
        return waterMode.getValue().equals("Solid");
    }

    private boolean lavaShouldBeSolid(Minecraft client) {
        Player player = client.player;
        if (player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        if (isLavaDangerous(player) && lavaMode.getValue().equals("Solid")) {
            return true;
        }
        if (dipOnSneakLava.isEnabled() && client.options.keyShift.isDown()) {
            return false;
        }
        if (dipOnFallLava.isEnabled() && player.fallDistance > dipFallHeightLava.getValue()) {
            return false;
        }
        return lavaMode.getValue().equals("Solid");
    }

    private boolean isLavaDangerous(Player player) {
        if (!dipIfFireResistant.isEnabled()) {
            return true;
        }
        var effect = player.getEffect(MobEffects.FIRE_RESISTANCE);
        return effect == null || effect.getDuration() <= 300;
    }
}
