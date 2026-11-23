package dev.octoclient.module.secret;

import dev.octoclient.mixin.LivingEntityAttackStrengthAccessor;
import dev.octoclient.module.Module;
import dev.octoclient.module.hud.Friends;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mace Aura: dives from cruise height onto the closest target, hits it with a
 * mace and climbs back up for the next slam. The same up/down cycle works with
 * a spear or trident, where the fall-based smash damage does not apply.
 */
public class MaceAura extends Module implements Module.Tickable {
    private static MaceAura instance;

    /**
     * Largest height difference per spoofed move packet. The server drops a
     * move that is farther than 10 blocks ("moved too quickly"), so the faked
     * fall is split into steps well below that limit.
     */
    private static final double SPOOF_STEP = 6.0;

    /** Weapon forced by a subclass; {@code null} keeps the mode setting. */
    private final String forcedMode;

    private final ModeSetting mode = new ModeSetting("mode",
            "The weapon used for the dive.", "Mace", List.of("Mace", "Spear", "Auto"));
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to dive on.", "Players", List.of("Players", "Mobs", "All"));
    private final BooleanSetting visibleOnly = new BooleanSetting("visible-only",
            "Only targets with a clear line of sight, so it never flies up for something behind a wall.", true);
    private final NumberSetting fieldOfView = new NumberSetting("field-of-view",
            "Only targets inside this view cone (degrees). 360 disables the check.", 180, 30, 360, 10);
    private final NumberSetting range = new NumberSetting("range",
            "Horizontal search radius around you.", 8, 2, 24, 0.5);
    private final NumberSetting cruiseHeight = new NumberSetting("cruise-height",
            "How high above the target the dive starts.", 5, 2, 20, 0.5);
    private final NumberSetting diveSpeed = new NumberSetting("dive-speed",
            "Downward speed of the dive, in blocks per tick.", 0.9, 0.2, 2, 0.05);
    private final NumberSetting climbSpeed = new NumberSetting("climb-speed",
            "Upward speed of the climb, in blocks per tick.", 0.55, 0.1, 1.5, 0.05);
    private final NumberSetting attackRange = new NumberSetting("attack-range",
            "Distance at which the hit is sent.", 3, 1, 6, 0.1);
    private final NumberSetting hitDelay = new NumberSetting("hit-delay",
            "Ticks between hits. 1 hits every tick, 2 every other tick.", 2, 1, 40, 1);
    private final BooleanSetting noCooldown = new BooleanSetting("no-cooldown",
            "Keeps the attack bar full and every hit fully charged.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Swaps to a matching weapon before diving.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores the previous hotbar slot afterwards.", true);
    private final BooleanSetting keepTotem = new BooleanSetting("keep-totem",
            "Never swaps a totem out of your hand and pauses while you hold one.", true);
    private final BooleanSetting requireAirborne = new BooleanSetting("require-airborne",
            "Only engages while you are in the air.", true);
    private final BooleanSetting waitForSmash = new BooleanSetting("wait-for-smash",
            "Mace only: waits until the fall is long enough for the smash bonus.", false);
    private final NumberSetting simulatedHeight = new NumberSetting("simulated-height",
            "Fakes a fall from this height, so a hit from below still deals the smash damage. 0 disables it.",
            0, 0, 20, 0.5);
    private final BooleanSetting autoMove = new BooleanSetting("auto-move",
            "Flies the climb/dive cycle itself. Off: only aims and hits, you move yourself.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never dives on friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Never dives on creative players.", true);

    private int cooldown;
    private int chargeWait;
    private int inRangeTicks;
    private int idleTicks;
    private int previousSlot = -1;
    private int moduleSlot = -1;
    private boolean swapped;
    private boolean diving;
    private Entity lastTarget;

    public MaceAura() {
        this("MaceAura", "Dives onto targets with a mace and hits them automatically.", 'M', null);
    }

    /**
     * @param forcedMode weapon the module is locked to, or {@code null} to
     *                   expose the mode setting
     */
    protected MaceAura(String name, String description, char icon, String forcedMode) {
        super(name, description, icon);
        this.forcedMode = forcedMode;
        if (forcedMode == null) {
            addSetting(mode);
        }
        addSetting(targets);
        addSetting(visibleOnly);
        addSetting(fieldOfView);
        addSetting(range);
        addSetting(cruiseHeight);
        addSetting(diveSpeed);
        addSetting(climbSpeed);
        addSetting(attackRange);
        addSetting(hitDelay);
        addSetting(noCooldown);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(keepTotem);
        addSetting(requireAirborne);
        addSetting(waitForSmash);
        addSetting(simulatedHeight);
        addSetting(autoMove);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
        if (forcedMode == null) {
            instance = this;
        }
    }

    @Override
    protected void onDisable() {
        releaseWeapon(Minecraft.getInstance());
        stop();
        cooldown = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            stop();
            return;
        }
        Player player = client.player;
        if (!player.isAlive() || player.isSpectator()) {
            releaseWeapon(client);
            stop();
            return;
        }
        // The player picked a different hotbar slot himself: respect that and
        // drop the swap-back target instead of overriding his choice later.
        if (swapped && moduleSlot >= 0 && player.getInventory().getSelectedSlot() != moduleSlot) {
            swapped = false;
            previousSlot = -1;
            moduleSlot = -1;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (chargeWait > 0) {
            chargeWait--;
        }

        if (requireAirborne.isEnabled() && player.onGround()) {
            stop();
            return;
        }

        Entity target = findTarget(client);
        if (target == null) {
            idle(client);
            return;
        }
        if (!selectWeapon(client)) {
            idle(client);
            return;
        }
        idleTicks = 0;
        refillCharge(player);

        Vec3 center = target.getBoundingBox().getCenter();
        double top = target.getBoundingBox().maxY;
        // Distance to the hitbox (not its center), so the hit lands as soon as
        // the target is really in reach instead of a body-width later.
        double reach = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
        // The server drops attacks that are farther away than its own limit
        // (base reach + 3 block verification buffer). Asking it the same way
        // avoids hits that look fine on the client but deal no damage.
        boolean serverReach = player.isWithinAttackRange(
                player.getMainHandItem(), target.getBoundingBox(), 3.0);

        if (reach <= attackRange.getFloat()) {
            inRangeTicks++;
            if (serverReach && canHit()
                    && (!waitForSmash.isEnabled() || smashReady(player) || inRangeTicks > 20)) {
                lookAt(player, target);
                spoofHeight(client, player);
                client.gameMode.attack(player, target);
                // Local swing only: the swing packet makes the server reset its
                // attack strength, which would turn every hit into spam damage.
                player.swing(InteractionHand.MAIN_HAND, false);
                refillCharge(player);
                cooldown = Math.max(1, hitDelay.getInt());
                inRangeTicks = 0;
                diving = false;
                return;
            }
            if (serverReach) {
                // In reach but still cooling down: hold position and aim
                // instead of drifting through the target.
                lookAt(player, target);
                steer(player, center, 0, 0);
                return;
            }
            // Too far for the server even though the setting allows it: keep
            // moving in instead of hovering out of range.
        } else {
            inRangeTicks = 0;
        }

        if (!autoMove.isEnabled()) {
            // The player flies; the module only aims and hits (e.g. from below).
            diving = false;
            return;
        }

        if (target != lastTarget) {
            lastTarget = target;
            diving = false;
        }

        double cruise = top + cruiseHeight.getFloat();
        double horizontal = Math.hypot(center.x - player.getX(), center.z - player.getZ());
        boolean highEnough = player.getY() >= cruise - 0.5;
        boolean overTarget = horizontal <= 1.5;
        if (!diving && highEnough && overTarget) {
            diving = true;
        }
        if (diving) {
            // Keep the dive going until the target is reached or passed; using
            // the cruise height as a per-tick condition would flip back to the
            // climb after the first tick and the player would hover in place.
            if (player.getY() < top - 1.0) {
                diving = false;
            } else {
                steer(player, center, diveSpeed.getFloat(), -diveSpeed.getFloat());
                return;
            }
        }
        if (player.getY() < cruise - 0.5) {
            // Climb towards cruise height while drifting over the target.
            steer(player, center, climbSpeed.getFloat(), climbSpeed.getFloat());
        } else {
            // At cruise height but not above the target yet: hold altitude.
            steer(player, center, climbSpeed.getFloat(), 0);
        }
    }

    /** True when another hit may be sent this tick. */
    private boolean canHit() {
        return cooldown <= 0 && chargeWait <= 0;
    }

    /**
     * Refills the local attack strength. The client ticker is only used for the
     * HUD bar and the local prediction; the server keeps its own charge because
     * no swing packet is sent.
     */
    private void refillCharge(Player player) {
        if (noCooldown.isEnabled()) {
            ((LivingEntityAttackStrengthAccessor) player).octoclient$setAttackStrengthTicker(100);
        }
    }

    /** A smash hit counts either by really falling or through the spoof. */
    private boolean smashReady(Player player) {
        return simulatedHeight.getValue() > 0 || MaceItem.canSmashAttack(player);
    }

    /**
     * Fakes a fall for the server's smash check. The mace bonus is computed
     * from the fall distance the server tracks, so a short down/up packet
     * sequence right before the attack gives the damage of a hit from above
     * even when the target is above you. Same technique as Criticals.
     *
     * <p>The server rejects a single move that is farther than 10 blocks
     * ("moved too quickly") and teleports the player back, which also throws
     * away the faked fall. The height is therefore walked in small steps.
     */
    private void spoofHeight(Minecraft client, Player player) {
        double height = simulatedHeight.getValue();
        if (height <= 0 || client.getConnection() == null || player.isFallFlying()) {
            return;
        }
        if (!pathOf(player.getMainHandItem()).equals("mace")) {
            return;
        }
        if (player.fallDistance >= height) {
            return;
        }
        // Going up does not add fall distance, so climb first and then fall all
        // the way back down to the real position.
        for (double offset = SPOOF_STEP; offset < height - 0.01; offset += SPOOF_STEP) {
            sendY(client, offset);
        }
        sendY(client, height);
        for (double offset = height - SPOOF_STEP; offset > 0.01; offset -= SPOOF_STEP) {
            sendY(client, offset);
        }
        sendY(client, 0);
    }

    private static void sendY(Minecraft client, double offset) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                client.player.getX(), client.player.getY() + offset, client.player.getZ(), false, false));
    }

    /** True while the module spoofs the smash height, so Criticals leaves it alone. */
    public static boolean simulationActive() {
        MaceAura module = instance;
        if (module == null || !module.isEnabled() || module.simulatedHeight.getValue() <= 0) {
            return false;
        }
        // Only the mace reads the spoofed fall distance; spear/trident hits are
        // unaffected, so Criticals must keep working there.
        Minecraft client = Minecraft.getInstance();
        return client.player != null && pathOf(client.player.getMainHandItem()).equals("mace");
    }

    private Entity findTarget(Minecraft client) {
        Player player = client.player;
        double max = range.getValue();
        double vertical = cruiseHeight.getValue() + 6;
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == player || entity == client.getCameraEntity()) {
                continue;
            }
            if (!entityCheck(client, entity, max, vertical)) {
                continue;
            }
            double distance = player.distanceToSqr(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private boolean entityCheck(Minecraft client, Entity entity, double max, double vertical) {
        if (!entity.isAlive() || entity.isInvisible()) {
            return false;
        }
        if (visibleOnly.isEnabled() && !client.player.hasLineOfSight(entity)) {
            return false;
        }
        if (fieldOfView.getValue() < 360
                && Math.toDegrees(angleTo(client.player, entity)) > fieldOfView.getValue() / 2) {
            return false;
        }
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return false;
        }
        boolean isPlayer = entity instanceof Player;
        if (targets.getValue().equals("Players") && !isPlayer) {
            return false;
        }
        if (targets.getValue().equals("Mobs") && isPlayer) {
            return false;
        }
        if (!isPlayer && targets.getValue().equals("Mobs")
                && !(entity instanceof net.minecraft.world.entity.monster.Enemy)) {
            return false;
        }
        if (!isPlayer && !(entity instanceof LivingEntity)) {
            return false;
        }
        double dx = entity.getX() - client.player.getX();
        double dz = entity.getZ() - client.player.getZ();
        if (dx * dx + dz * dz > max * max) {
            return false;
        }
        double dy = entity.getY() - client.player.getY();
        if (dy > 2 || dy < -vertical) {
            return false;
        }
        if (isPlayer) {
            Player other = (Player) entity;
            if (other.isSpectator() || other.isDeadOrDying()) {
                return false;
            }
            if (ignoreCreative.isEnabled() && other.isCreative()) {
                return false;
            }
            if (ignoreFriends.isEnabled() && Friends.isFriend(other.getName().getString())) {
                return false;
            }
        }
        return true;
    }

    /** Angle between the player's view direction and the target, in radians. */
    private static double angleTo(Player player, Entity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = entity.getBoundingBox().getCenter();
        Vec3 look = player.getLookAngle();
        Vec3 diff = center.subtract(eye).normalize();
        return Math.acos(Math.max(-1, Math.min(1, look.dot(diff))));
    }

    /** Holds or swaps to a matching weapon; false when none is available. */
    private boolean selectWeapon(Minecraft client) {
        Player player = client.player;
        if (matches(player.getMainHandItem())) {
            return true;
        }
        if (keepTotem.isEnabled() && isTotem(player.getMainHandItem())) {
            // A totem in the hand is worth more than a dive: leave it alone and
            // pause instead of swapping it away.
            swapped = false;
            previousSlot = -1;
            moduleSlot = -1;
            return false;
        }
        if (!autoSwitch.isEnabled()) {
            return false;
        }
        int slot = findWeapon(client);
        if (slot < 0) {
            return false;
        }
        if (!swapped) {
            previousSlot = player.getInventory().getSelectedSlot();
            swapped = true;
        }
        if (slot != player.getInventory().getSelectedSlot()) {
            player.getInventory().setSelectedSlot(slot);
            // Player.tick() resets the server-side attack strength when the
            // main-hand item type changes. 36 ticks is one full charge of the
            // slowest supported weapon (mace) plus a small safety margin.
            chargeWait = Math.max(chargeWait, 36);
        }
        moduleSlot = slot;
        return true;
    }

    private static boolean isTotem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    private int findWeapon(Minecraft client) {
        // Auto prefers the mace, then any spear.
        int fallback = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (matches(stack)) {
                if (pathOf(stack).equals("mace")) {
                    return slot;
                }
                if (fallback < 0) {
                    fallback = slot;
                }
            }
        }
        return fallback;
    }

    private boolean matches(ItemStack stack) {
        String path = pathOf(stack);
        if (path.isEmpty()) {
            return false;
        }
        return switch (forcedMode != null ? forcedMode : mode.getValue()) {
            case "Mace" -> path.equals("mace");
            case "Spear" -> isSpear(path);
            default -> path.equals("mace") || isSpear(path);
        };
    }

    private static boolean isSpear(String path) {
        return path.equals("trident") || path.endsWith("_spear");
    }

    private static String pathOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /**
     * Clears the per-target state but keeps the weapon in hand: swapping back
     * and forth would reset the server's attack charge on every item change and
     * every brief target loss would restart the full-charge wait.
     */
    private void stop() {
        inRangeTicks = 0;
        diving = false;
        lastTarget = null;
    }

    /** Nothing to do right now; after a while the hotbar slot is handed back. */
    private void idle(Minecraft client) {
        stop();
        if (swapped && ++idleTicks > 100) {
            releaseWeapon(client);
        }
    }

    /** Restores the hotbar slot the module took over. */
    private void releaseWeapon(Minecraft client) {
        if (swapped && previousSlot >= 0 && client.player != null && swapBack.isEnabled()) {
            client.player.getInventory().setSelectedSlot(previousSlot);
        }
        swapped = false;
        previousSlot = -1;
        moduleSlot = -1;
        idleTicks = 0;
    }

    /**
     * Drives the player directly instead of through movement input, so the
     * dive/climb works the same with or without the Flight module enabled.
     * Gravity removes 0.08 and applies 0.98 friction each tick, hence the
     * compensation that makes {@code vertical} the real blocks per tick.
     */
    private static void steer(Player player, Vec3 aim, double horizontalSpeed, double verticalSpeed) {
        double dx = aim.x - player.getX();
        double dz = aim.z - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > 1.0e-4) {
            dx = dx / horizontal * horizontalSpeed;
            dz = dz / horizontal * horizontalSpeed;
        } else {
            dx = 0;
            dz = 0;
        }
        player.setDeltaMovement(dx, verticalSpeed + 0.08, dz);
        // Abilities flight would replace the velocity with input-driven flight,
        // so the dive/climb drives the player as a falling entity instead.
        player.getAbilities().flying = false;
    }

    private static void lookAt(Player player, Entity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        double dx = center.x - eye.x;
        double dy = center.y - eye.y;
        double dz = center.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
    }
}
