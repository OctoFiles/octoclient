package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * World-space overlays for the crystal, anchor and multi-target modules.
 *
 * <p>The analysis modules are mostly automated, so their result would be
 * invisible. This class draws what each enabled module is looking at: valid
 * crystal and anchor spots, damage estimates, ranges, the required rotation,
 * surround gaps, trap blocks, predicted positions and totem state.</p>
 *
 * <p>It only reads settings; the modules keep their own behavior. Overlays are
 * driven by the module's enabled state, so nothing is drawn for a disabled
 * module.</p>
 */
public final class AuraViz {
    private static final float LINE = 1.5f;
    private static final int CRYSTAL = 0xFF6FE8FF;
    private static final int BASE = 0xFF55FF55;
    private static final int ANCHOR = 0xFFFFAA33;
    private static final int CHARGE = 0xFFFFE055;
    private static final int TARGET = 0xFFFF3355;
    private static final int GAP = 0xFFFF5555;
    private static final int TRAP = 0xFFB266FF;
    private static final int SELF = 0xFFFF9A3C;

    /** Last seen totem state per entity id, for the totem-loss marker. */
    private static final Map<Integer, Boolean> TOTEM_STATE = new HashMap<>();

    private AuraViz() {
    }

    /** Called once per frame from the level-render hook. */
    public static void submit(Minecraft client) {
        if (client.level == null || client.player == null || client.gui.screen() != null) {
            return;
        }
        ModuleManager modules = ModuleManager.get();
        // One overlay must not hide the others, and a world change mid-frame can
        // invalidate a position, so every module is isolated.
        safely(() -> crystalAura(client, modules));
        safely(() -> autoCrystal(client, modules));
        safely(() -> crystalPlace(client, modules));
        safely(() -> crystalBreak(client, modules));
        safely(() -> crystalOptimizer(client, modules));
        safely(() -> crystalPrediction(client, modules));
        safely(() -> crystalDamage(client, modules));
        safely(() -> crystalTarget(client, modules));
        safely(() -> crystalRange(client, modules));
        safely(() -> crystalRotation(client, modules));
        safely(() -> crystalSurround(client, modules));
        safely(() -> crystalAntiTotem(client, modules));
        safely(() -> crystalTrap(client, modules));
        safely(() -> crystalSwitch(client, modules));
        safely(() -> crystalMacro(client, modules));
        safely(() -> anchorAura(client, modules));
        safely(() -> autoAnchor(client, modules));
        safely(() -> anchorPlace(client, modules));
        safely(() -> anchorCharge(client, modules));
        safely(() -> anchorExplode(client, modules));
        safely(() -> anchorPrediction(client, modules));
        safely(() -> anchorDamage(client, modules));
        safely(() -> anchorTarget(client, modules));
        safely(() -> anchorSwitch(client, modules));
        safely(() -> anchorMacro(client, modules));
        safely(() -> multiAura(client, modules));
    }

    private static void safely(Runnable overlay) {
        try {
            overlay.run();
        } catch (RuntimeException ignored) {
            // Never let a gizmo break the render loop.
        }
    }

    /* ------------------------------------------------------------------ */
    /* Crystal modules                                                    */
    /* ------------------------------------------------------------------ */

    private static void crystalAura(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalAura");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "place-range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        double minDamage = num(module, "min-damage", 6);
        double maxSelf = num(module, "max-self-damage", 8);
        outlineTarget(target);
        if (flag(module, "place", true)) {
            double range = CrystalRange.placeRange(num(module, "place-range", 4.5));
            BlockPos base = CrystalUtil.bestBase(client, target, range, minDamage, maxSelf, predict);
            if (base != null) {
                drawBase(target, base, predict);
            }
        }
        if (flag(module, "break", true)) {
            double range = CrystalRange.breakRange(num(module, "break-range", 4.5));
            EndCrystal crystal = CrystalUtil.bestCrystal(client, target, range, minDamage, maxSelf, predict);
            if (crystal != null) {
                drawCrystal(target, crystal, predict);
            }
        }
    }

    private static void autoCrystal(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AutoCrystal");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "place-range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        outlineTarget(target);
        if (flag(module, "place", true)) {
            BlockPos base = CrystalUtil.bestBase(client, target,
                    CrystalRange.placeRange(num(module, "place-range", 4.5)),
                    num(module, "min-damage", 6), num(module, "max-self-damage", 8), predict);
            if (base != null) {
                drawBase(target, base, predict);
            }
        }
        if (flag(module, "break", true)) {
            EndCrystal crystal = CrystalUtil.bestCrystal(client, target,
                    CrystalRange.breakRange(num(module, "break-range", 4.5)),
                    num(module, "min-damage", 6), num(module, "max-self-damage", 8), predict);
            if (crystal != null) {
                drawCrystal(target, crystal, predict);
            }
        }
    }

    private static void crystalPlace(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalPlace");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "place-range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        outlineTarget(target);
        for (BlockPos base : bases(client, target,
                CrystalRange.placeRange(num(module, "place-range", 4.5)),
                num(module, "min-damage", 6), num(module, "max-self-damage", 8), predict, 8)) {
            drawBase(target, base, predict);
        }
    }

    private static void crystalBreak(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalBreak");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "break-range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        outlineTarget(target);
        for (EndCrystal crystal : CrystalUtil.crystals(client,
                CrystalRange.breakRange(num(module, "break-range", 4.5)))) {
            drawCrystal(target, crystal, predict);
        }
    }

    private static void crystalOptimizer(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalOptimizer");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        double minDamage = num(module, "min-damage", 6);
        double maxSelf = num(module, "max-self-damage", 8);
        outlineTarget(target);
        List<Candidate> ranked = new ArrayList<>();
        for (BlockPos base : bases(client, target, num(module, "range", 4.5),
                minDamage, maxSelf, predict, 8)) {
            ranked.add(new Candidate(Vec3.atCenterOf(base.above()),
                    CrystalUtil.damageTo(target, Vec3.atCenterOf(base.above()), predict),
                    CrystalUtil.selfDamage(client, Vec3.atCenterOf(base.above()))));
        }
        for (EndCrystal crystal : CrystalUtil.crystals(client, num(module, "range", 4.5))) {
            ranked.add(new Candidate(crystal.position(),
                    CrystalUtil.damageTo(target, crystal.position(), predict),
                    CrystalUtil.selfDamage(client, crystal.position())));
        }
        ranked.removeIf(candidate -> candidate.damage() < minDamage || candidate.self() > maxSelf);
        ranked.sort(Comparator.comparingDouble(Candidate::damage).reversed());
        int rank = 0;
        for (Candidate candidate : ranked) {
            if (rank >= 3) {
                break;
            }
            rank++;
            Gizmos.cuboid(new AABB(candidate.pos().x - 0.5, candidate.pos().y - 0.5,
                    candidate.pos().z - 0.5, candidate.pos().x + 0.5, candidate.pos().y + 0.5,
                    candidate.pos().z + 0.5), GizmoStyle.strokeAndFill(CRYSTAL, LINE, fill(CRYSTAL, 40)));
            label(candidate.pos().add(0, 0.9, 0),
                    "#" + rank + " " + fmt(candidate.damage()) + " (-" + fmt(candidate.self()) + ")",
                    rank == 1 ? BASE : CRYSTAL);
        }
    }

    private static void crystalPrediction(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalPrediction");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int ticks = (int) num(module, "predict-ticks", 4);
        Vec3 predicted = AuraUtil.predicted(target, ticks);
        Vec3 offset = predicted.subtract(target.position());
        AABB ghost = target.getBoundingBox().move(offset);
        Gizmos.cuboid(ghost, GizmoStyle.strokeAndFill(TRAP, LINE, fill(TRAP, 35)));
        Gizmos.line(target.getBoundingBox().getCenter(), ghost.getCenter(), TRAP, LINE);
        label(ghost.getCenter().add(0, 1.0, 0), "target +" + ticks + "t", TRAP);
        BlockPos predictedBase = nearestBase(client, BlockPos.containing(predicted),
                num(module, "range", 4.5));
        if (predictedBase != null) {
            Gizmos.cuboid(predictedBase.above(), GizmoStyle.stroke(CRYSTAL, LINE));
            label(Vec3.atCenterOf(predictedBase.above()).add(0, 0.9, 0), "predicted", CRYSTAL);
        }
    }

    private static void crystalDamage(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalDamage");
        if (module == null) {
            return;
        }
        double range = num(module, "range", 4.5);
        List<LivingEntity> targets = AuraUtil.targets(client, range,
                mode(module, "targets", "Players"), flag(module, "ignore-friends", true),
                flag(module, "ignore-creative", true));
        if (targets.isEmpty()) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        for (LivingEntity target : targets) {
            double bestCrystal = 0;
            for (EndCrystal crystal : CrystalUtil.crystals(client, range)) {
                bestCrystal = Math.max(bestCrystal, CrystalUtil.damageTo(target, crystal.position(), predict));
            }
            double bestBase = 0;
            BlockPos base = CrystalUtil.bestBase(client, target, range, 0, 100, predict);
            if (base != null) {
                bestBase = CrystalUtil.damageTo(target, Vec3.atCenterOf(base.above()), predict);
            }
            if (bestCrystal <= 0 && bestBase <= 0) {
                continue;
            }
            Gizmos.cuboid(target.getBoundingBox().inflate(0.08), GizmoStyle.stroke(TARGET, LINE));
            label(target.getBoundingBox().getCenter().add(0, 1.2, 0),
                    "C " + fmt(bestCrystal) + " | B " + fmt(bestBase), bestCrystal > 0 ? CRYSTAL : BASE);
        }
        label(client.player.position().add(0, 2.3, 0),
                "self " + fmt(selfCrystalDamage(client, range)), SELF);
    }

    private static void crystalTarget(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalTarget");
        if (module == null) {
            return;
        }
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, num(module, "range", 8),
                    mode(module, "targets", "Players"), flag(module, "ignore-friends", true),
                    flag(module, "ignore-creative", true));
        }
        if (target == null) {
            return;
        }
        Gizmos.cuboid(target.getBoundingBox().inflate(0.1), GizmoStyle.stroke(TARGET, 2.0f));
        label(target.getBoundingBox().getCenter().add(0, 1.5, 0),
                "TARGET " + fmt(target.getHealth()), TARGET);
    }

    private static void crystalRange(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalRange");
        if (module == null) {
            return;
        }
        Vec3 eye = client.player.getEyePosition();
        double place = CrystalRange.placeRange(4.5);
        double breakRange = CrystalRange.breakRange(4.5);
        Gizmos.circle(client.player.position(), (float) place, GizmoStyle.stroke(BASE, 1.0f));
        Gizmos.circle(client.player.position(), (float) breakRange, GizmoStyle.stroke(CRYSTAL, 1.0f));
        List<EndCrystal> crystals = CrystalUtil.crystals(client, Math.max(place, breakRange) + 4);
        if (!crystals.isEmpty()) {
            EndCrystal crystal = crystals.getFirst();
            Gizmos.line(eye, crystal.position(), CRYSTAL, LINE);
            label(crystal.position().add(0, 0.7, 0),
                    "crystal " + fmt(client.player.distanceTo(crystal)), CRYSTAL);
        }
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, breakRange + 4, "Players", true, true);
        }
        if (target != null) {
            Vec3 center = target.getBoundingBox().getCenter();
            Gizmos.line(eye, center, TARGET, LINE);
            label(center.add(0, 1.1, 0), "target " + fmt(client.player.distanceTo(target)), TARGET);
        }
        label(client.player.position().add(0, 2.1, 0),
                "place " + fmt(place) + " / break " + fmt(breakRange), BASE);
    }

    private static void crystalRotation(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalRotation");
        if (module == null) {
            return;
        }
        if (flag(module, "only-aura", false)
                && !ModuleManager.get().byName("CrystalAura").map(Module::isEnabled).orElse(false)) {
            return;
        }
        double range = num(module, "range", 6);
        Vec3 eye = client.player.getEyePosition();
        List<EndCrystal> crystals = CrystalUtil.crystals(client, range);
        Vec3 aim = null;
        if (!crystals.isEmpty()) {
            aim = crystals.getFirst().position().add(0, 0.5, 0);
        } else {
            LivingEntity target = CrystalTarget.lockedTarget(client);
            if (target == null) {
                target = AuraUtil.target(client, range, "Players", true, true);
            }
            if (target != null) {
                BlockPos base = CrystalUtil.bestBase(client, target, range, 0, 100, 0);
                aim = base != null ? Vec3.atCenterOf(base.above())
                        : target.getBoundingBox().getCenter();
            }
        }
        if (aim == null) {
            return;
        }
        Gizmos.arrow(eye, aim, CHARGE, 2.0f);
        label(aim.add(0, 0.8, 0), "aim", CHARGE);
    }

    private static void crystalSurround(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalSurround");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 5));
        if (target == null || client.level == null) {
            return;
        }
        outlineTarget(target);
        BlockPos feet = target.blockPosition();
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        int gaps = 0;
        int covered = 0;
        for (int[] offset : offsets) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos pos = feet.offset(offset[0], dy, offset[1]);
                BlockState state = client.level.getBlockState(pos);
                if (!state.isAir() && !state.canBeReplaced()) {
                    if (CrystalUtil.isBase(state)) {
                        Gizmos.cuboid(pos, GizmoStyle.stroke(BASE, 1.0f));
                        covered++;
                    }
                    break;
                }
                if (dy == 1) {
                    Gizmos.cuboid(pos, GizmoStyle.strokeAndFill(GAP, 1.0f, fill(GAP, 35)));
                    gaps++;
                }
            }
        }
        label(Vec3.atCenterOf(feet).add(0, 2.4, 0),
                "gaps " + gaps + " | covered " + covered, gaps > 0 ? GAP : BASE);
    }

    private static void crystalAntiTotem(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalAntiTotem");
        if (module == null) {
            return;
        }
        double range = num(module, "range", 6);
        for (LivingEntity target : AuraUtil.targets(client, range,
                mode(module, "targets", "Players"), flag(module, "ignore-friends", true),
                flag(module, "ignore-creative", true))) {
            boolean totem = hasTotem(target);
            Boolean before = TOTEM_STATE.put(target.getId(), totem);
            Vec3 above = target.getBoundingBox().getCenter().add(0, 1.4, 0);
            if (Boolean.TRUE.equals(before) && !totem) {
                label(above, "TOTEM POPPED", TARGET);
            } else if (!totem) {
                label(above, "no totem", SELF);
            } else {
                label(above, "totem", BASE);
            }
        }
    }

    private static void crystalTrap(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalTrap");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 5));
        if (target == null || client.level == null) {
            return;
        }
        outlineTarget(target);
        BlockPos feet = target.blockPosition();
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(feet.getX() - 2, feet.getY() + 2, feet.getZ() - 2,
                feet.getX() + 2, feet.getY() + 4, feet.getZ() + 2)) {
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                continue;
            }
            Gizmos.cuboid(pos, GizmoStyle.stroke(TRAP, 1.0f));
            if (++shown >= 12) {
                break;
            }
        }
        AnchorUtil.Spot spot = AnchorUtil.trapSpot(client, target, num(module, "range", 5));
        if (spot != null) {
            Gizmos.cuboid(spot.pos(), GizmoStyle.strokeAndFill(TRAP, LINE, fill(TRAP, 40)));
            label(Vec3.atCenterOf(spot.pos()).add(0, 0.8, 0), "trap spot", TRAP);
        }
    }

    private static void crystalSwitch(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalSwitch");
        if (module == null) {
            return;
        }
        String mode = mode(module, "mode", "Auto");
        double range = num(module, "range", 5);
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, 8, "Players", true, true);
        }
        if (target == null && flag(module, "target-only", true)) {
            return;
        }
        if (target != null) {
            outlineTarget(target);
        }
        String wanted;
        BlockPos base = null;
        if (mode.equals("Crystal")) {
            wanted = "crystal";
            base = target != null
                    ? CrystalUtil.bestBase(client, target, range, 0, 20, 0) : null;
        } else if (mode.equals("Obsidian")) {
            wanted = "obsidian";
        } else {
            base = target != null
                    ? CrystalUtil.bestBase(client, target, range, 0, 20, 0) : null;
            wanted = base != null ? "crystal" : "obsidian";
        }
        if (base != null) {
            Gizmos.cuboid(base, GizmoStyle.stroke(BASE, LINE));
            Gizmos.cuboid(base.above(), GizmoStyle.strokeAndFill(CRYSTAL, LINE, fill(CRYSTAL, 30)));
        }
        label(client.player.position().add(0, 2.0, 0), "switch: " + wanted, BASE);
    }

    private static void crystalMacro(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "CrystalMacro");
        if (module == null || client.level == null) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockPos base = hit.getBlockPos();
        if (!CrystalUtil.isBase(client.level.getBlockState(base))) {
            return;
        }
        Gizmos.cuboid(base, GizmoStyle.stroke(BASE, LINE));
        AABB area = new AABB(base.getX(), base.getY() + 1, base.getZ(),
                base.getX() + 1, base.getY() + 2, base.getZ() + 1);
        List<EndCrystal> crystals = client.level.getEntitiesOfClass(EndCrystal.class, area);
        if (!crystals.isEmpty()) {
            EndCrystal crystal = crystals.getFirst();
            Gizmos.cuboid(crystal.getBoundingBox(), GizmoStyle.stroke(TARGET, LINE));
            label(crystal.position().add(0, 1.2, 0),
                    "macro: break (-" + fmt(CrystalUtil.selfDamage(client, crystal.position())) + ")",
                    TARGET);
        } else {
            Gizmos.cuboid(base.above(), GizmoStyle.strokeAndFill(CRYSTAL, LINE, fill(CRYSTAL, 35)));
            label(Vec3.atCenterOf(base.above()).add(0, 0.9, 0), "macro: place", CRYSTAL);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Anchor modules                                                     */
    /* ------------------------------------------------------------------ */

    private static void anchorAura(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorAura");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        double minDamage = num(module, "min-damage", 6);
        double maxSelf = num(module, "max-self-damage", 8);
        double range = num(module, "range", 4.5);
        outlineTarget(target);
        if (flag(module, "place", true)) {
            AnchorUtil.Spot spot = AnchorUtil.findSpot(client, target, range, minDamage, maxSelf, predict);
            if (spot != null) {
                drawAnchorSpot(target, spot.pos(), predict);
            }
        }
        if (flag(module, "charge", true)) {
            BlockPos uncharged = AnchorUtil.bestUncharged(client, target, range);
            if (uncharged != null) {
                Gizmos.cuboid(uncharged, GizmoStyle.stroke(CHARGE, LINE));
                label(Vec3.atCenterOf(uncharged).add(0, 0.9, 0), "glowstone", CHARGE);
            }
        }
        if (flag(module, "explode", true)) {
            BlockPos charged = AnchorUtil.bestCharged(client, target, range, minDamage, maxSelf, predict);
            if (charged != null) {
                Gizmos.cuboid(charged, GizmoStyle.strokeAndFill(TARGET, LINE, fill(TARGET, 45)));
                label(Vec3.atCenterOf(charged).add(0, 0.9, 0),
                        "ARMED " + fmt(AnchorUtil.damageTo(target, charged, predict)), TARGET);
            }
        }
    }

    private static void autoAnchor(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AutoAnchor");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        double range = num(module, "range", 4.5);
        outlineTarget(target);
        AnchorUtil.Spot spot = AnchorUtil.findSpot(client, target, range,
                num(module, "min-damage", 6), num(module, "max-self-damage", 8), predict);
        if (spot != null) {
            drawAnchorSpot(target, spot.pos(), predict);
        }
        if (flag(module, "charge", true)) {
            BlockPos uncharged = AnchorUtil.bestUncharged(client, target, range);
            if (uncharged != null) {
                Gizmos.cuboid(uncharged, GizmoStyle.stroke(CHARGE, LINE));
            }
        }
        if (flag(module, "explode", true)) {
            BlockPos charged = AnchorUtil.bestCharged(client, target, range,
                    num(module, "min-damage", 6), num(module, "max-self-damage", 8), predict);
            if (charged != null) {
                Gizmos.cuboid(charged, GizmoStyle.strokeAndFill(TARGET, LINE, fill(TARGET, 45)));
            }
        }
    }

    private static void anchorPlace(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorPlace");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        outlineTarget(target);
        for (BlockPos spot : anchorSpots(client, target, num(module, "range", 4.5),
                num(module, "min-damage", 6), num(module, "max-self-damage", 8), predict, 8)) {
            drawAnchorSpot(target, spot, predict);
        }
    }

    private static void anchorCharge(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorCharge");
        if (module == null || client.level == null) {
            return;
        }
        double range = num(module, "range", 4.5);
        LivingEntity target = null;
        if (flag(module, "target-only", false)) {
            target = targetIn(client, module, num(module, "target-range", 8));
            if (target == null) {
                return;
            }
            outlineTarget(target);
        }
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                client.player.blockPosition().offset(-(int) range, -3, -(int) range),
                client.player.blockPosition().offset((int) range, 3, (int) range))) {
            int charge = AnchorUtil.charge(client.level, pos);
            if (charge < 0 || charge >= 4) {
                continue;
            }
            if (!AnchorUtil.inRange(client, pos, range)) {
                continue;
            }
            if (target != null && target.distanceToSqr(Vec3.atCenterOf(pos)) > range * range * 4) {
                continue;
            }
            Gizmos.cuboid(pos, GizmoStyle.strokeAndFill(CHARGE, LINE, fill(CHARGE, 30)));
            label(Vec3.atCenterOf(pos).add(0, 0.9, 0),
                    "charge " + charge + "/4 (+" + (4 - charge) + ")", CHARGE);
            if (++shown >= 10) {
                break;
            }
        }
    }

    private static void anchorExplode(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorExplode");
        if (module == null || client.level == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        outlineTarget(target);
        BlockPos center = target.blockPosition();
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 4, center.getY() - 3, center.getZ() - 4,
                center.getX() + 4, center.getY() + 3, center.getZ() + 4)) {
            int charge = AnchorUtil.charge(client.level, pos);
            if (charge <= 0 || !AnchorUtil.inRange(client, pos, num(module, "range", 4.5))) {
                continue;
            }
            double damage = AnchorUtil.damageTo(target, pos, predict);
            if (damage < num(module, "min-damage", 6)) {
                continue;
            }
            Gizmos.cuboid(pos, GizmoStyle.strokeAndFill(TARGET, LINE, fill(TARGET, 40)));
            label(Vec3.atCenterOf(pos).add(0, 0.9, 0), "ARMED " + fmt(damage), TARGET);
            if (++shown >= 8) {
                break;
            }
        }
    }

    private static void anchorPrediction(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorPrediction");
        if (module == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 4);
        double minDamage = num(module, "min-damage", 6);
        outlineTarget(target);
        Vec3 predicted = AuraUtil.predicted(target, predict);
        AABB ghost = target.getBoundingBox()
                .move(predicted.subtract(target.position()));
        Gizmos.cuboid(ghost, GizmoStyle.stroke(TRAP, LINE));
        label(ghost.getCenter().add(0, 1.0, 0), "target +" + predict + "t", TRAP);

        AnchorUtil.Spot spot = AnchorUtil.findSpot(client, target, num(module, "range", 4.5),
                minDamage, num(module, "max-self-damage", 8), predict);
        if (spot == null) {
            return;
        }
        Vec3 blast = Vec3.atCenterOf(spot.pos());
        double damage = AnchorUtil.damageTo(target, spot.pos(), predict);
        double radius = blastRadius(AnchorUtil.POWER, Math.max(1.0, minDamage));
        Gizmos.cuboid(spot.pos(), GizmoStyle.strokeAndFill(ANCHOR, LINE, fill(ANCHOR, 40)));
        Gizmos.circle(blast, (float) radius, GizmoStyle.stroke(ANCHOR, 1.0f));
        Gizmos.line(blast, predicted, ANCHOR, 1.0f);
        label(blast.add(0, 0.9, 0), fmt(damage) + " dmg", ANCHOR);
        label(blast.add(radius, 0.4, 0), ">=" + fmt(minDamage), ANCHOR);
    }

    private static void anchorDamage(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorDamage");
        if (module == null || client.level == null) {
            return;
        }
        LivingEntity target = targetIn(client, module, num(module, "range", 4.5));
        if (target == null) {
            return;
        }
        int predict = (int) num(module, "predict-ticks", 0);
        outlineTarget(target);
        double range = num(module, "range", 4.5);
        BlockPos center = target.blockPosition();
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 4, center.getY() - 3, center.getZ() - 4,
                center.getX() + 4, center.getY() + 3, center.getZ() + 4)) {
            int charge = AnchorUtil.charge(client.level, pos);
            if (charge < 0 || !AnchorUtil.inRange(client, pos, range)) {
                continue;
            }
            double damage = AnchorUtil.damageTo(target, pos, predict);
            if (damage <= 0) {
                continue;
            }
            Gizmos.cuboid(pos, GizmoStyle.stroke(charge > 0 ? TARGET : ANCHOR, 1.0f));
            label(Vec3.atCenterOf(pos).add(0, 0.9, 0),
                    (charge > 0 ? "armed " : "cold ") + fmt(damage), charge > 0 ? TARGET : ANCHOR);
            if (++shown >= 8) {
                break;
            }
        }
        label(client.player.position().add(0, 2.3, 0),
                "self " + fmt(selfAnchorDamage(client, target, predict)), SELF);
    }

    private static void anchorTarget(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorTarget");
        if (module == null) {
            return;
        }
        LivingEntity target = AnchorTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, num(module, "range", 8),
                    mode(module, "targets", "Players"), flag(module, "ignore-friends", true),
                    flag(module, "ignore-creative", true));
        }
        if (target == null) {
            return;
        }
        Gizmos.cuboid(target.getBoundingBox().inflate(0.1), GizmoStyle.stroke(TARGET, 2.0f));
        label(target.getBoundingBox().getCenter().add(0, 1.5, 0),
                "TARGET " + fmt(target.getHealth()), TARGET);
    }

    private static void anchorSwitch(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorSwitch");
        if (module == null) {
            return;
        }
        String mode = mode(module, "mode", "Auto");
        double range = num(module, "range", 5);
        LivingEntity target = AnchorTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, 8, "Players", true, true);
        }
        if (target == null && flag(module, "target-only", true)) {
            return;
        }
        if (target != null) {
            outlineTarget(target);
        }
        String wanted;
        BlockPos charged = null;
        BlockPos uncharged = null;
        if (mode.equals("Anchor")) {
            wanted = "anchor";
        } else if (mode.equals("Glowstone")) {
            wanted = "glowstone";
        } else if (mode.equals("Detonator")) {
            wanted = "detonator";
        } else {
            charged = target != null
                    ? AnchorUtil.bestCharged(client, target, range, 0, 20, 0) : null;
            uncharged = target != null
                    ? AnchorUtil.bestUncharged(client, target, range) : null;
            wanted = charged != null ? "detonator" : uncharged != null ? "glowstone" : "anchor";
        }
        if (charged != null) {
            Gizmos.cuboid(charged, GizmoStyle.strokeAndFill(TARGET, LINE, fill(TARGET, 40)));
            label(Vec3.atCenterOf(charged).add(0, 0.9, 0), "switch: detonator", TARGET);
        }
        if (uncharged != null) {
            Gizmos.cuboid(uncharged, GizmoStyle.strokeAndFill(CHARGE, LINE, fill(CHARGE, 30)));
            label(Vec3.atCenterOf(uncharged).add(0, 0.9, 0), "switch: glowstone", CHARGE);
        }
        if (charged == null && uncharged == null) {
            label(client.player.position().add(0, 2.0, 0), "switch: " + wanted, ANCHOR);
        }
    }

    private static void anchorMacro(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "AnchorMacro");
        if (module == null || client.level == null) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockPos clicked = hit.getBlockPos();
        Level level = client.level;
        if (AnchorUtil.isAnchor(level, clicked)) {
            int charge = AnchorUtil.charge(level, clicked);
            Gizmos.cuboid(clicked, GizmoStyle.stroke(ANCHOR, LINE));
            if (charge > 0) {
                Gizmos.cuboid(clicked, GizmoStyle.strokeAndFill(TARGET, LINE, fill(TARGET, 35)));
                label(Vec3.atCenterOf(clicked).add(0, 0.9, 0),
                        "macro: detonate (-" + fmt(AnchorUtil.selfDamage(client, clicked)) + ")",
                        TARGET);
            } else {
                label(Vec3.atCenterOf(clicked).add(0, 0.9, 0),
                        "macro: charge " + charge + "/4", CHARGE);
            }
            return;
        }
        BlockPos placePos = clicked.relative(hit.getDirection());
        if (AuraUtil.replaceable(level, placePos) && AuraUtil.solid(level, clicked)) {
            Gizmos.cuboid(placePos, GizmoStyle.strokeAndFill(ANCHOR, LINE, fill(ANCHOR, 35)));
            label(Vec3.atCenterOf(placePos).add(0, 0.9, 0), "macro: place anchor", ANCHOR);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Multi-target module                                                */
    /* ------------------------------------------------------------------ */

    private static void multiAura(Minecraft client, ModuleManager modules) {
        Module module = enabled(modules, "MultiAura");
        if (module == null) {
            return;
        }
        int max = (int) num(module, "max-targets", 3);
        List<LivingEntity> targets = AuraUtil.targets(client, num(module, "range", 4.5),
                mode(module, "targets", "Players"), flag(module, "ignore-friends", true),
                flag(module, "ignore-creative", true));
        int index = 0;
        for (LivingEntity target : targets) {
            if (index >= max) {
                break;
            }
            index++;
            Gizmos.cuboid(target.getBoundingBox().inflate(0.08), GizmoStyle.stroke(TARGET, LINE));
            label(target.getBoundingBox().getCenter().add(0, 1.2, 0),
                    "#" + index + " " + fmt(target.getHealth()), TARGET);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Drawing helpers                                                    */
    /* ------------------------------------------------------------------ */

    private static void drawBase(LivingEntity target, BlockPos base, int predict) {
        BlockPos above = base.above();
        double damage = CrystalUtil.damageTo(target, Vec3.atCenterOf(above), predict);
        Gizmos.cuboid(above, GizmoStyle.strokeAndFill(CRYSTAL, LINE, fill(CRYSTAL, 40)));
        label(Vec3.atCenterOf(above).add(0, 0.9, 0), fmt(damage), CRYSTAL);
    }

    private static void drawCrystal(LivingEntity target, EndCrystal crystal, int predict) {
        double damage = CrystalUtil.damageTo(target, crystal.position(), predict);
        Gizmos.cuboid(crystal.getBoundingBox(), GizmoStyle.stroke(CRYSTAL, LINE));
        label(crystal.position().add(0, 1.3, 0), fmt(damage), CRYSTAL);
    }

    private static void drawAnchorSpot(LivingEntity target, BlockPos pos, int predict) {
        double damage = AnchorUtil.damageTo(target, pos, predict);
        Gizmos.cuboid(pos, GizmoStyle.strokeAndFill(ANCHOR, LINE, fill(ANCHOR, 40)));
        label(Vec3.atCenterOf(pos).add(0, 0.9, 0), fmt(damage), ANCHOR);
    }

    private static void outlineTarget(LivingEntity target) {
        Gizmos.cuboid(target.getBoundingBox().inflate(0.08), GizmoStyle.stroke(TARGET, LINE));
    }

    private static void label(Vec3 pos, String text, int color) {
        Gizmos.billboardText(text, pos, TextGizmo.Style.forColorAndCentered(color).withScale(0.8f));
    }

    private static int fill(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /* ------------------------------------------------------------------ */
    /* Candidates                                                         */
    /* ------------------------------------------------------------------ */

    private record Candidate(Vec3 pos, double damage, double self) {
    }

    private static List<BlockPos> bases(Minecraft client, LivingEntity target, double range,
                                        double minDamage, double maxSelf, int predict, int limit) {
        List<BlockPos> found = new ArrayList<>();
        Level level = client.level;
        if (level == null) {
            return found;
        }
        BlockPos center = target.blockPosition();
        for (BlockPos base : BlockPos.betweenClosed(center.getX() - 3, center.getY() - 2, center.getZ() - 3,
                center.getX() + 3, center.getY() + 2, center.getZ() + 3)) {
            if (!CrystalUtil.isBase(level.getBlockState(base))) {
                continue;
            }
            BlockPos above = base.above();
            if (!AuraUtil.replaceable(level, above) || !CrystalUtil.inRange(client, base, range)) {
                continue;
            }
            Vec3 blast = Vec3.atCenterOf(above);
            if (CrystalUtil.damageTo(target, blast, predict) < minDamage) {
                continue;
            }
            if (CrystalUtil.selfDamage(client, blast) > maxSelf) {
                continue;
            }
            found.add(base.immutable());
        }
        found.sort(Comparator.comparingDouble(
                (BlockPos b) -> -CrystalUtil.damageTo(target, Vec3.atCenterOf(b.above()), predict)));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    private static List<BlockPos> anchorSpots(Minecraft client, LivingEntity target, double range,
                                              double minDamage, double maxSelf, int predict, int limit) {
        List<BlockPos> found = new ArrayList<>();
        Level level = client.level;
        if (level == null) {
            return found;
        }
        BlockPos center = target.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 2, center.getY() - 1, center.getZ() - 2,
                center.getX() + 2, center.getY() + 1, center.getZ() + 2)) {
            if (!AuraUtil.replaceable(level, pos) || !AnchorUtil.inRange(client, pos, range)) {
                continue;
            }
            if (AnchorUtil.supportFace(level, pos) == null) {
                continue;
            }
            if (AnchorUtil.damageTo(target, pos, predict) < minDamage) {
                continue;
            }
            if (AnchorUtil.selfDamage(client, pos) > maxSelf) {
                continue;
            }
            found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(
                (BlockPos p) -> -AnchorUtil.damageTo(target, p, predict)));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    /** Nearest crystal base to an arbitrary position, for the prediction ghost. */
    private static BlockPos nearestBase(Minecraft client, BlockPos around, double range) {
        Level level = client.level;
        if (level == null) {
            return null;
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos base : BlockPos.betweenClosed(around.getX() - 3, around.getY() - 2, around.getZ() - 3,
                around.getX() + 3, around.getY() + 2, around.getZ() + 3)) {
            if (!CrystalUtil.isBase(level.getBlockState(base))
                    || !AuraUtil.replaceable(level, base.above())
                    || !CrystalUtil.inRange(client, base, range)) {
                continue;
            }
            double distance = base.distSqr(around);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = base.immutable();
            }
        }
        return best;
    }

    /* ------------------------------------------------------------------ */
    /* Settings access                                                    */
    /* ------------------------------------------------------------------ */

    private static Module enabled(ModuleManager modules, String name) {
        Module module = modules.byName(name).orElse(null);
        return module != null && module.isEnabled() ? module : null;
    }

    private static LivingEntity targetIn(Minecraft client, Module module, double range) {
        LivingEntity locked = CrystalTarget.lockedTarget(client);
        if (locked == null) {
            locked = AnchorTarget.lockedTarget(client);
        }
        if (locked != null && client.player != null
                && client.player.distanceTo(locked) <= range + 4) {
            return locked;
        }
        return AuraUtil.target(client, range, mode(module, "targets", "Players"),
                flag(module, "ignore-friends", true), flag(module, "ignore-creative", true));
    }

    private static boolean flag(Module module, String name, boolean fallback) {
        for (Setting<?> setting : module.getSettings()) {
            if (setting.getName().equals(name) && setting instanceof BooleanSetting bool) {
                return bool.isEnabled();
            }
        }
        return fallback;
    }

    private static double num(Module module, String name, double fallback) {
        for (Setting<?> setting : module.getSettings()) {
            if (setting.getName().equals(name) && setting instanceof NumberSetting number) {
                return number.getValue();
            }
        }
        return fallback;
    }

    private static String mode(Module module, String name, String fallback) {
        for (Setting<?> setting : module.getSettings()) {
            if (setting.getName().equals(name) && setting instanceof ModeSetting mode) {
                return mode.getValue();
            }
        }
        return fallback;
    }

    /* ------------------------------------------------------------------ */
    /* Damage helpers                                                     */
    /* ------------------------------------------------------------------ */

    private static boolean hasTotem(LivingEntity entity) {
        return entity.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || entity.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private static double selfCrystalDamage(Minecraft client, double range) {
        double worst = 0;
        for (EndCrystal crystal : CrystalUtil.crystals(client, range)) {
            worst = Math.max(worst, CrystalUtil.selfDamage(client, crystal.position()));
        }
        return worst;
    }

    private static double selfAnchorDamage(Minecraft client, LivingEntity target, int predict) {
        BlockPos charged = AnchorUtil.bestCharged(client, target, 8, 0, 100, predict);
        return charged == null ? 0 : AnchorUtil.selfDamage(client, charged);
    }

    /**
     * Radius in blocks where an explosion still deals {@code damage}.
     * Inverts the vanilla falloff used by {@link AuraUtil#explosionDamage}.
     */
    private static double blastRadius(double power, double damage) {
        double k = Math.max(0, (damage - 1.0) / (7.0 * power));
        double impact = (-1.0 + Math.sqrt(1.0 + 8.0 * k)) / 2.0;
        return Math.max(0, (1.0 - impact) * power * 2.0);
    }
}
