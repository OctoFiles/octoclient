package dev.octoclient.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.octoclient.module.cosmetics.CosmeticCatalog;
import dev.octoclient.module.hud.ShieldStatusHud;
import dev.octoclient.network.CapeSettings;
import dev.octoclient.network.CapeSyncClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and renders catalog OBJ and MD5 meshes in the player's local model space.
 *
 * <p>Catalog capes, wings and bandanas use three different source formats, so
 * each gets its own placement path:</p>
 * <ul>
 *   <li>Model-less capes are a procedural cloth strip that uses the standard
 *       44x46 cape atlas (outer face on the left half, inner face on the
 *       right half).</li>
 *   <li>MD5 capes are skinned to {@code dynamic.md5anim}, which is the same
 *       cloth animation Wavey Capes plays. The MD5 mesh is authored with the
 *       attachment edge on +Z and the cloth hanging down -Z, so it is rotated
 *       into the player's +Y-down/+Z-back space and blended with its rest
 *       pose (a little when idle, fully while moving).</li>
 *   <li>Wings are one-sided half-meshes. The authored half is drawn once and
 *       mirrored across X so the pair is visible from both sides.</li>
 *   <li>Bandanas are authored Y-up with the face toward +Z, so they are
 *       rotated 180 degrees about X (Y and Z flipped) and centred from their
 *       own bounds. Both the old bandana and the v2 wrap then put the band on
 *       the upper head with the knot at the back.</li>
 * </ul>
 */
final class CatalogCosmeticRenderer {
    private static final String RESOURCE_ROOT = "/assets/octoclient/textures/cosmetics/";
    private static final Pattern JOINTS_BLOCK = Pattern.compile("(?s)\\bjoints\\s*\\{(.*?)\\}");
    private static final Pattern MESH_BLOCK = Pattern.compile("(?s)\\bmesh\\s*\\{(.*?)\\n\\s*\\}");
    private static final Pattern JOINT_LINE = Pattern.compile(
            "\"([^\"]+)\"\\s+(-?\\d+)\\s+\\(\\s*([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s*\\)"
                    + "\\s+\\(\\s*([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s*\\)");
    private static final Pattern VERT_LINE = Pattern.compile(
            "vert\\s+(\\d+)\\s+\\(\\s*([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s*\\)\\s+(\\d+)\\s+(\\d+)");
    private static final Pattern TRI_LINE = Pattern.compile("tri\\s+\\d+\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
    private static final Pattern WEIGHT_LINE = Pattern.compile(
            "weight\\s+(\\d+)\\s+(\\d+)\\s+([-+\\d.eE]+)\\s+\\(\\s*([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s*\\)");
    private static final Pattern HIERARCHY_BLOCK = Pattern.compile("(?s)\\bhierarchy\\s*\\{(.*?)\\}");
    private static final Pattern HIERARCHY_LINE = Pattern.compile("\"([^\"]+)\"\\s+(-?\\d+)\\s+(\\d+)\\s+(\\d+)");
    private static final Pattern BASEFRAME_BLOCK = Pattern.compile("(?s)\\bbaseframe\\s*\\{(.*?)\\}");
    private static final Pattern BASEFRAME_LINE = Pattern.compile(
            "\\(\\s*([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s*\\)"
                    + "\\s*\\(\\s*([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s+([-+\\d.eE]+)\\s*\\)");
    private static final Pattern FRAME_BLOCK = Pattern.compile("(?s)\\bframe\\s+\\d+\\s*\\{(.*?)\\}");
    private static final Pattern FRAME_RATE = Pattern.compile("\\bframeRate\\s+(\\d+)");
    private static final Pattern NUM_FRAMES = Pattern.compile("\\bnumFrames\\s+(\\d+)");

    /**
     * Head-local Y of a bandana's top edge. The head spans -8..0 px (up is
     * negative) and the eyes sit around -6..-5 px. The band is 1.52 px tall, so
     * -7.75 puts it just under the crown and clears the eyes: the old -5.25
     * value dropped its lower edge to about -3.7, right across the face.
     * Both OBJ bandana models have the band at their top and the knot/tails
     * below, so aligning the top edge keeps them consistent.
     */
    private static final float BANDANA_TOP_Y = -7.75f / 16f;
    /**
     * Radius the bandana band has to reach, in head pixels. The band is
     * authored just outside the 8 px skull (4.0 px) but inside the skin's
     * 9 px outer (hat/hair) layer, which ends at 4.5 px, and a worn armor
     * helmet reaches 5.0 px - both swallowed the band completely. Pushing the
     * band past 5.0 px keeps it visible under either, while the band's inner
     * face (3.375 px authored) still ends up right on the skull instead of
     * floating away from it.
     */
    private static final float BANDANA_BAND_RADIUS = 5.3f;
    /**
     * The band is the top of the bandana mesh; the knot and tails hang below.
     * Inflating the whole mesh keeps the two parts joined, and 0.9 px of extra
     * depth on the tails is invisible behind the head.
     */
    private static final float BANDANA_BAND_MIN_Y = 0f;
    /**
     * Wings hang just below the neck instead of mid-back. Player space is
     * Y-down, so the drop is positive; the pair was raised once more (2.7 to
     * 1.9 px) so the roots sit on the shoulder blades and the sheets rise above
     * the shoulders like the in-game wings instead of ending level with them.
     */
    private static final float WING_OFFSET_Y = 1.9f / 16f;
    /** Shared downward nudge applied to wings without a model (fallback panels). */
    private static final float WING_FALLBACK_DROP = 1.5f / 16f;
    /**
     * The Wichtiger wing sheets are much taller than the other catalog wings.
     * Their top edge reached the head pivot and visibly cut into the skull, so
     * this pair gets an extra drop while all other wings keep their fit.
     */
    private static final float WICHTIGER_WING_DROP = 4f / 16f;
    /**
     * Demon head meshes start almost at the head pivot. Lift the helmet and
     * horns so their lower rim sits on top of the skull instead of poking into
     * it; crowns and smaller hats keep the shared fit.
     */
    private static final float DEMON_HELMET_LIFT = 4f / 16f;
    private static final float DEMON_HORNS_LIFT = 2f / 16f;
    /**
     * On the body side of the cape sheet (4.2 px), so the cloth covers the wing
     * roots and only the spread beyond the cape's width reads from behind.
     */
    private static final float WING_OFFSET_Z = 2.9f / 16f;
    /**
     * Lateral shift between each wing root and the spine, in model pixels.
     * This is applied after the authored vertices are converted, so it lives in
     * the same unit as the meshes (px), not blocks; the old 1.1/16 value was
     * 0.07 px and did nothing at all. A negative pull draws the two halves one
     * pixel closer to the spine, which closes the gap down the middle of the
     * back that made the pair read as two separate sheets.
     */
    private static final float WING_SPREAD = -1f;
    /**
     * Rest-pose blend for MD5 capes: the cloth keeps a visible sway while the
     * player stands still, and follows the animation fully while moving.
     */
    private static final float CAPE_IDLE_BLEND = 0.55f;
    /**
     * MD5 clips are authored at 24 fps (wings: 24-30 fps). {@code speed} is a
     * playback rate in frames per second, so these must be in the same range as
     * the source frame rate; values around 1 frame per second froze the cloth.
     */
    private static final float CAPE_IDLE_SPEED = 9f;
    private static final float CAPE_MOVING_SPEED = 24f;
    private static final float WING_IDLE_SPEED = 6.5f;
    private static final float WING_MOVING_SPEED = 14f;
    /**
     * Catalog wings replay the idle clip of the Black Dark Wings pack
     * ({@code wings/dark_wings/variants/black_dark_wings/black_dark_wings.animation.json},
     * {@code nrc.idle}). That clip drives two chained bones around the vertical
     * axis at {@code query.anim_time * 100} and adds a small second harmonic
     * 0.25 degrees out of phase. Molang trigonometry works in degrees, exactly
     * like {@link WingModelRenderer}'s evaluator, so the 100 is 100 degrees per
     * second (~0.28 Hz), not radians: reading it as rad/s ran the flutter 57
     * times too fast and turned it into a stroboscopic jitter that never read
     * as a wing coming back to its rest pose. Their rest-pose offsets (-40/+40)
     * are applied as {@link #WING_BASE_SWEEP}: without them the catalog sheets
     * sit flat against the spine and the flutter swings them forward through
     * the torso.
     * <p>
     * The reference clip lets the outer bone do nearly all of the work
     * (18.4 degrees peak to peak inside, 34.4 outside). On the tall BastiGHG
     * sheets the outer edge is also the top of the wing, so that read as "only
     * the top swings". The base yaw now covers the whole sheet and the outer
     * bend only adds a gentle whip at the tip, so every part of the wing moves
     * together. The same flutter is applied to OBJ and MD5 catalog wings.
     */
    private static final float WING_FLUTTER_RATE = 100f;
    private static final float WING_INNER_PHASE = -50f;
    private static final float WING_OUTER_PHASE = -100f;
    private static final float WING_HARMONIC_PHASE = -0.25f;
    private static final float WING_INNER_SWING = 12f;
    private static final float WING_INNER_HARMONIC = 5f;
    private static final float WING_OUTER_SWING = 8f;
    private static final float WING_OUTER_HARMONIC = 0.2f;
    /** Span fraction where the whole-sheet yaw starts bending towards the tip. */
    private static final float WING_INNER_SPLIT = 0.4f;
    /**
     * Rest-pose yaw of the reference clip's inner bone (-40 on the left half,
     * +40 on the right). Applied uniformly around the spine hinge so every
     * catalog half sits angled back like the Black Dark Wings and the flutter
     * oscillates around that sweep instead of swinging through the player.
     */
    private static final float WING_BASE_SWEEP = 40f;
    private static final int CAPE_ROWS = 16;
    /**
     * Standard cape atlas: 44x46 template. The outer face is the left cell
     * (u 0..22, v 0..34), the inner face the right cell (u 22..44, v 0..34),
     * and the straps live in the bottom strip (v 34..46). The cells are sampled
     * edge to edge so the design is neither clipped nor shifted.
     */
    private static final float CAPE_OUTER_U0 = 0f;
    private static final float CAPE_OUTER_U1 = 22f / 44f;
    private static final float CAPE_INNER_U0 = 22f / 44f;
    private static final float CAPE_INNER_U1 = 1f;
    private static final float CAPE_V0 = 0f;
    private static final float CAPE_V1 = 34f / 46f;
    /** First-person shield footprint in model pixels, matching the vanilla plate closely. */
    private static final float FIRST_PERSON_SHIELD_WIDTH = 14f;
    private static final float FIRST_PERSON_SHIELD_HEIGHT = 22f;
    /** Dev-only: log submitted first-person shield geometry and its camera transform. */
    private static final boolean DEBUG_SHIELDS = Boolean.getBoolean("octoclient.debugShields");
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
    private static final Map<String, Mesh> MESHES = new HashMap<>();
    private static final Map<String, CapeUv> CAPE_UVS = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    private CatalogCosmeticRenderer() {
    }

    static void render(CosmeticCatalog.Entry entry, PlayerModel playerModel, PoseStack pose,
                       SubmitNodeCollector collector, int light, AvatarRenderState state) {
        Identifier texture = entry.texture();
        Mesh mesh = meshFor(entry);
        switch (entry.type()) {
            case "CAPE" -> {
                if (mesh != null && mesh.animated()) {
                    renderMd5Cape(entry, mesh, playerModel, pose, collector, light, texture, state);
                } else {
                    renderWaveyCape(entry, playerModel, pose, collector, light, texture, state);
                }
            }
            case "WINGS" -> {
                if (DevCapture.hideWings()) {
                    return;
                }
                if (mesh != null) {
                    renderWings(entry, mesh, playerModel, pose, collector, light, texture, state);
                } else {
                    renderFallback(entry, playerModel, pose, collector, light, texture, state);
                }
            }
            case "BOOTS" -> {
                if (mesh == null) {
                    renderFallback(entry, playerModel, pose, collector, light, texture, state);
                    return;
                }
                renderMesh(entry, mesh, playerModel.rightLeg, pose, collector, light, texture, state);
                renderMesh(entry, mesh, playerModel.leftLeg, pose, collector, light, texture, state);
            }
            case "SHIELD" -> {
                // Draw the catalog shield directly on the arm in third person, so it
                // sits on the forearm and follows arm animation. Always show the
                // equipped shield cosmetic, even without a held shield item.
                if (mesh == null) {
                    return;
                }
                boolean left = true;
                Entity entity = Minecraft.getInstance().level == null ? null
                        : Minecraft.getInstance().level.getEntity(state.id);
                if (entity instanceof Player player) {
                    boolean off = player.getOffhandItem().is(Items.SHIELD);
                    boolean main = player.getMainHandItem().is(Items.SHIELD);
                    if (main && !off) {
                        left = false;
                    }
                }
                ModelPart arm = left ? playerModel.leftArm : playerModel.rightArm;
                renderShieldOnArm(entry, mesh, arm, pose, collector, light, texture, left);
            }
            default -> {
                if (mesh == null) {
                    renderFallback(entry, playerModel, pose, collector, light, texture, state);
                } else {
                    renderMesh(entry, mesh, attachment(entry.type(), playerModel), pose, collector, light, texture, state);
                }
            }
        }
    }

    /** True for the round catalogue shield meshes that need first-person pose tweaks. */
    public static boolean isRoundShield(CosmeticCatalog.Entry entry) {
        return entry != null && entry.modelPaths().stream().anyMatch(
                p -> p != null && (p.contains("roundshield") || p.contains("shield_heart")));
    }

    /**
     * Submits a catalog shield in the pose after the vanilla item transform has
     * been applied. Shield OBJ assets use several source scales, so the mesh is
     * normalised to a vanilla-sized footprint while keeping its own aspect and
     * centred on the item origin, matching the hand/item pose in first and
     * third person.
     */
    static boolean renderItemShield(CosmeticCatalog.Entry entry, PoseStack pose,
                                    SubmitNodeCollector collector, int light, boolean firstPerson) {
        Mesh mesh = meshFor(entry);
        if (mesh == null) {
            return false;
        }
        Bounds bounds = mesh.bounds();
        float width = Math.max(1f, bounds.maxX() - bounds.minX());
        float height = Math.max(1f, bounds.maxY() - bounds.minY());
        boolean roundShield = isRoundShield(entry);
        // Round first-person shields are square meshes; the 14-pixel width limit
        // shrinks them far below the vanilla shield's 22-pixel height and pushes
        // nearly the whole silhouette below the viewport. Size those by height
        // instead, while rectangular catalog shields keep the bounded fit.
        float fit = firstPerson && roundShield
                ? FIRST_PERSON_SHIELD_HEIGHT / height
                : Math.min(FIRST_PERSON_SHIELD_WIDTH / width, FIRST_PERSON_SHIELD_HEIGHT / height);
        float scale = entry.scale() * fit;
        float centerX = (bounds.minX() + bounds.maxX()) * 0.5f;
        float centerY = (bounds.minY() + bounds.maxY()) * 0.5f;
        float centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5f;
        int color = ShieldStatusHud.tintShieldBase(0xFFFFFFFF);

        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.translate(-centerX, -centerY, -centerZ);
        // Third-person held shields sit a bit low in the item pose; nudge up in
        // item space before rotation so the offset stays vertical relative to hand.
        float liftY = firstPerson ? 0f : -14f;
        pose.translate(entry.translationX(), entry.translationY() + liftY, entry.translationZ());
        if (!firstPerson) {
            // Align third-person held shield orientation in the item pose.
            pose.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(90.0)));
        }
        if (roundShield) {
            // Round/heart shield models have their design on the opposite face.
            pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180.0)));
        } else if (!firstPerson) {
            // Some non-round third-person shield meshes present the opposite face.
            pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180.0)));
        }
        if (DEBUG_SHIELDS) {
            System.out.printf("[OctoClient] item shield entry=%s bounds=%.2f..%.2f x %.2f..%.2f x %.2f..%.2f fit=%.4f scale=%.5f round=%s first=%s%n",
                    entry.key(), bounds.minX(), bounds.maxX(), bounds.minY(), bounds.maxY(),
                    bounds.minZ(), bounds.maxZ(), fit, scale, roundShield, firstPerson);
        }
        Identifier texture = entry.texture();
        boolean[] logged = {false};
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, vertices) -> {
            if (DEBUG_SHIELDS && !logged[0]) {
                logged[0] = true;
                float minX = Float.POSITIVE_INFINITY;
                float minY = Float.POSITIVE_INFINITY;
                float minZ = Float.POSITIVE_INFINITY;
                float maxX = Float.NEGATIVE_INFINITY;
                float maxY = Float.NEGATIVE_INFINITY;
                float maxZ = Float.NEGATIVE_INFINITY;
                for (Triangle triangle : mesh.frameZero()) {
                    for (Vertex vertex : List.of(triangle.first(), triangle.second(), triangle.third())) {
                        Vector3f point = matrix.pose().transformPosition(
                                new Vector3f(vertex.x(), vertex.y(), vertex.z()), new Vector3f());
                        minX = Math.min(minX, point.x);
                        minY = Math.min(minY, point.y);
                        minZ = Math.min(minZ, point.z);
                        maxX = Math.max(maxX, point.x);
                        maxY = Math.max(maxY, point.y);
                        maxZ = Math.max(maxZ, point.z);
                    }
                }
                System.out.printf("[OctoClient] item shield geometry triangles=%d "
                                + "cameraBounds=%.3f..%.3f x %.3f..%.3f x %.3f..%.3f%n",
                        mesh.frameZero().size(), minX, maxX, minY, maxY, minZ, maxZ);
            }
            for (Triangle triangle : mesh.frameZero()) {
                // A 180 degree yaw shows the authored back face. Mirror U there so
                // the design is not flipped when viewed from the front.
                emitTriangle(vertices, matrix, light, triangle, entry.flipUVs(), color,
                        roundShield && firstPerson);
            }
        });
        pose.popPose();
        return true;
    }

    private static boolean isMoving(AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(state.id);
        return entity != null && CapeSyncClient.isEntityMoving(entity);
    }

    // ------------------------------------------------------------------
    // Static OBJ / generic MD5 meshes (bandanas, backpacks, tails, hats)
    // ------------------------------------------------------------------

    private static void renderShieldOnArm(CosmeticCatalog.Entry entry, Mesh mesh, ModelPart arm, PoseStack pose,
                                          SubmitNodeCollector collector, int light, Identifier texture, boolean leftArm) {
        Bounds bounds = mesh.bounds();
        float width = Math.max(1f, bounds.maxX() - bounds.minX());
        float height = Math.max(1f, bounds.maxY() - bounds.minY());
        float fit = Math.min(FIRST_PERSON_SHIELD_WIDTH / width, FIRST_PERSON_SHIELD_HEIGHT / height);
        float scale = entry.scale() * fit;
        float centerX = (bounds.minX() + bounds.maxX()) * 0.5f;
        float centerY = (bounds.minY() + bounds.maxY()) * 0.5f;
        float centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5f;
        int color = ShieldStatusHud.tintShieldBase(0xFFFFFFFF);

        pose.pushPose();
        arm.translateAndRotate(pose);
        // Place on the forearm, on the outer side of the arm.
        float side = leftArm ? 1f : -1f;
        pose.translate(side * (4f / 16f), 7f / 16f, 0f);
        // Face the shield outward from the arm.
        boolean roundShield = entry.modelPaths().stream().anyMatch(
                p -> p != null && (p.contains("roundshield") || p.contains("shield_heart")));
        pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(leftArm ? 90.0 : -90.0)));
        pose.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(roundShield ? 270.0 : 90.0)));
        pose.scale(scale, scale, scale);
        pose.translate(-centerX, -centerY, -centerZ);
        pose.translate(entry.translationX(), entry.translationY(), entry.translationZ());
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, vertices) -> {
            for (Triangle triangle : mesh.frameZero()) {
                // The arm pose shows the shield from its opposite side, so both
                // faces need U mirrored; otherwise the catalog design reads
                // spiegelverkehrt compared with the held shield.
                emitTriangle(vertices, matrix, light, triangle, entry.flipUVs(), color, true, true);
            }
        });
        pose.popPose();
    }

    private static void renderMesh(CosmeticCatalog.Entry entry, Mesh mesh, ModelPart part, PoseStack pose,
                                   SubmitNodeCollector collector, int light, Identifier texture,
                                   AvatarRenderState state) {
        boolean bandana = entry.type().equals("BANDANA");
        float scale = entry.scale();
        float offsetY = 0f;
        if (bandana) {
            // Bandana OBJ assets are authored Y-up with the face toward +Z and
            // the knot/tails on -Z. Player model space is Y-down with the face
            // toward -Z, so both axes are flipped (a 180 degree rotation about
            // X). The band is the top of the mesh, so aligning the top edge
            // under the crown puts the band on the upper head and the knot at
            // the back, like the Bedrock bandana geometry, instead of hanging
            // the band at the neck.
            Bounds bounds = mesh.bounds();
            offsetY = BANDANA_TOP_Y + bounds.maxY() * scale - entry.translationY();
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        // Catalog hats sit a good half pixel into the skull; lift the whole
        // group so the brim clears the head instead of cutting through it.
        float liftY = entry.type().equals("HAT") ? -1.1f / 16f : 0f;
        if (entry.type().equals("HAT")) {
            for (String path : entry.modelPaths()) {
                if (path == null) {
                    continue;
                }
                if (path.endsWith("/demon_helmet.obj")) {
                    liftY -= DEMON_HELMET_LIFT;
                } else if (path.endsWith("/sasukey_demon_horns.obj")) {
                    liftY -= DEMON_HORNS_LIFT;
                }
            }
        }
        pose.translate(entry.translationX(), entry.translationY() + offsetY + liftY, entry.translationZ());
        // Bandanas are authored Y-up, face toward +Z, knot at -Z. Java model
        // space is Y-down with the back of the head at +Z, so a real 180 degree
        // turn about X flips both Y and Z. Flipping only Y left the knot on the
        // forehead, which is why every catalog bandana read as "falsch herum".
        // Bandanas stay rigid: the cape cloth simulation is only for capes.
        pose.scale(scale, -scale, -scale);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, vertices) -> {
            for (Triangle triangle : mesh.frameZero()) {
                emitTriangle(vertices, matrix, light, triangle, entry.flipUVs());
            }
        });
        pose.popPose();
    }

    private static ModelPart attachment(String type, PlayerModel model) {
        return switch (type) {
            case "HAT", "BANDANA", "EARS", "NOSE", "MOUTH", "EYES", "MASK",
                    "INSIDER_NAMETAG_ICON", "BLC_NAMETAG_ICON", "INSIDER_NAMETAG_BORDER" -> model.head;
            case "BOW", "SWORD", "SHIELD", "ARM_BAND" -> model.rightArm;
            default -> model.body;
        };
    }

    // ------------------------------------------------------------------
    // Wings
    // ------------------------------------------------------------------

    private static void renderWings(CosmeticCatalog.Entry entry, Mesh mesh, PlayerModel model, PoseStack pose,
                                    SubmitNodeCollector collector, int light, Identifier texture,
                                    AvatarRenderState state) {
        boolean moving = isMoving(state);
        float speed = moving ? WING_MOVING_SPEED : WING_IDLE_SPEED;
        boolean md5 = mesh.md5();
        // MD5 wings play a baked clip. Keep the fractional frame position and
        // blend the two neighbouring frames, otherwise a 24 fps clip sampled
        // at a handful of frames per second visibly steps instead of moving.
        List<Triangle> frameFirst;
        List<Triangle> frameSecond;
        float frameBlend;
        if (md5 && mesh.animated()) {
            float position = animationPosition(mesh, state.ageInTicks / 20f * speed);
            int base = (int) Math.floor(position);
            frameFirst = mesh.frame(base);
            frameSecond = mesh.frame(base + 1);
            frameBlend = position - base;
        } else {
            frameFirst = mesh.frameZero();
            frameSecond = frameFirst;
            frameBlend = 0f;
        }
        // OBJ wings are a single static pose and MD5 wings carry a baked cloth
        // clip. Both get the same Black Dark Wings flutter on top: the whole
        // sheet sweeps fore/aft around the vertical axis at the spine, with a
        // soft bend at the tip. On MD5 wings the clip keeps its own flap, so
        // the authored motion stays and every catalog wing reads the same.
        WingSwing swing = WingSwing.at(mesh, state.ageInTicks / 20f);
        pose.pushPose();
        model.body.translateAndRotate(pose);
        // The catalog roots sit high on the shoulder blades and the meshes are
        // authored small; drop every pair and grow it by the same factor.
        boolean wichtigerWings = entry.modelPaths().stream()
                .anyMatch(path -> path != null && path.endsWith("/wichtiger.obj"));
        float headClearance = wichtigerWings ? WICHTIGER_WING_DROP : 0f;
        pose.translate(entry.translationX(),
                entry.translationY() + WING_OFFSET_Y + 1.1f / 16f + headClearance,
                entry.translationZ() + WING_OFFSET_Z);
        float wingScale = entry.scale() * 1.25f;
        pose.scale(wingScale, wingScale, wingScale);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, vertices) -> {
            // One scratch buffer per wing, reused for every triangle, so the
            // per-frame swing does not allocate a Vertex/Vector3f per corner.
            float[] scratch = new float[9];
            int count = frameFirst.size();
            for (int i = 0; i < count; i++) {
                Triangle triangle = frameFirst.get(i);
                // Both frames of one clip share topology, so the same index is
                // the same corner in the next frame.
                Triangle next = frameSecond.size() == count ? frameSecond.get(i) : triangle;
                // The source assets contain a single half-wing. Draw the
                // authored half and its mirror image so both wings exist and
                // read correctly from either side.
                emitWingTriangle(vertices, matrix, light, triangle, next, frameBlend,
                        entry.flipUVs(), false, md5, swing, scratch);
                emitWingTriangle(vertices, matrix, light, triangle, next, frameBlend,
                        entry.flipUVs(), true, md5, swing, scratch);
            }
        });
        pose.popPose();
    }

    private static void emitWingTriangle(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                         Triangle triangle, Triangle next, float blend, boolean flipUVs,
                                         boolean mirror, boolean md5, WingSwing swing, float[] scratch) {
        wingPoint(scratch, 0, triangle.first(), next.first(), blend, md5, swing, mirror);
        wingPoint(scratch, 3, triangle.second(), next.second(), blend, md5, swing, mirror);
        wingPoint(scratch, 6, triangle.third(), next.third(), blend, md5, swing, mirror);
        if (mirror) {
            // Mirroring across the player's X axis reverses the winding, so
            // swap the last two corners to keep the reflected half facing out.
            swapPoints(scratch, 3, 6);
        }
        float ax = scratch[0];
        float ay = scratch[1];
        float az = scratch[2];
        float bx = scratch[3];
        float by = scratch[4];
        float bz = scratch[5];
        float cx = scratch[6];
        float cy = scratch[7];
        float cz = scratch[8];
        float abx = bx - ax;
        float aby = by - ay;
        float abz = bz - az;
        float acx = cx - ax;
        float acy = cy - ay;
        float acz = cz - az;
        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;
        float lengthSquared = nx * nx + ny * ny + nz * nz;
        if (lengthSquared < 1.0e-8f) {
            nx = 0f;
            ny = 1f;
            nz = 0f;
        } else {
            float length = (float) Math.sqrt(lengthSquared);
            nx /= length;
            ny /= length;
            nz /= length;
        }
        Vertex secondUv = mirror ? triangle.third() : triangle.second();
        Vertex thirdUv = mirror ? triangle.second() : triangle.third();
        // Draw both sides so round/thin wing meshes are visible from either
        // direction instead of appearing backwards from one side.
        vertex(consumer, pose, light, ax, ay, az, triangle.first().u(), triangle.first().v(),
                nx, ny, nz, flipUVs);
        vertex(consumer, pose, light, bx, by, bz, secondUv.u(), secondUv.v(),
                nx, ny, nz, flipUVs);
        vertex(consumer, pose, light, cx, cy, cz, thirdUv.u(), thirdUv.v(),
                nx, ny, nz, flipUVs);
        vertex(consumer, pose, light, cx, cy, cz, thirdUv.u(), thirdUv.v(),
                nx, ny, nz, flipUVs);

        vertex(consumer, pose, light, ax, ay, az, triangle.first().u(), triangle.first().v(),
                -nx, -ny, -nz, flipUVs);
        vertex(consumer, pose, light, cx, cy, cz, thirdUv.u(), thirdUv.v(),
                -nx, -ny, -nz, flipUVs);
        vertex(consumer, pose, light, bx, by, bz, secondUv.u(), secondUv.v(),
                -nx, -ny, -nz, flipUVs);
        vertex(consumer, pose, light, bx, by, bz, secondUv.u(), secondUv.v(),
                -nx, -ny, -nz, flipUVs);
    }

    /**
     * Converts one authored wing corner to player space, applies the shared
     * flutter and mirrors/shifts it for the requested half. Everything stays
     * in primitives so a crowd of animated wings does not churn the heap.
     */
    private static void wingPoint(float[] out, int offset, Vertex vertex, Vertex next, float blend,
                                  boolean md5, WingSwing swing, boolean mirror) {
        // Blend the two neighbouring animation frames before any conversion;
        // the mapping to player space is linear, so order does not matter.
        float x = vertex.x() + (next.x() - vertex.x()) * blend;
        float y = vertex.y() + (next.y() - vertex.y()) * blend;
        float z = vertex.z() + (next.z() - vertex.z()) * blend;
        // MD5 assets use the Doom 3 convention (Z up, Y forward), so a wing's
        // vertical axis is Z and its thin depth axis is Y. OBJ assets are
        // authored Y up with Z as the thickness axis.
        float px = x;
        float py = md5 ? -z : -y;
        float pz = md5 ? y : z;
        if (swing != null) {
            float span = swing.tipX() - swing.rootX();
            float frac = Math.max(0f, Math.min(1f, (px - swing.rootX()) / span));
            // The reference rest pose is a constant yaw on the inner bone. A
            // rotation about the vertical line through rootX leaves that hinge
            // itself fixed, so the base can be uniform: it angles the whole
            // sheet back without letting the root slide into the torso.
            // The flutter is ramped on top, from zero at the hinge to its full
            // value at the crease, then the outer bend; smoothstep keeps the
            // slope continuous where the two segments meet.
            float flutter;
            if (frac <= WING_INNER_SPLIT) {
                float t = frac / WING_INNER_SPLIT;
                flutter = swing.inner() * (t * t * (3f - 2f * t));
            } else {
                float t = (frac - WING_INNER_SPLIT) / (1f - WING_INNER_SPLIT);
                flutter = swing.inner() + swing.outer() * (t * t * (3f - 2f * t));
            }
            float yaw = swing.base() + flutter;
            float cos = (float) Math.cos(yaw);
            float sin = (float) Math.sin(yaw);
            float dx = px - swing.rootX();
            px = swing.rootX() + dx * cos + pz * sin;
            pz = -dx * sin + pz * cos;
        }
        if (mirror) {
            px = -px;
        }
        // Draw both halves towards the spine by WING_SPREAD so the pair reads
        // as one set of wings instead of two sheets with a gap between them.
        if (Math.abs(px) > 1.0e-5f) {
            px += Math.signum(px) * WING_SPREAD;
        }
        out[offset] = px;
        out[offset + 1] = py;
        out[offset + 2] = pz;
    }

    private static void swapPoints(float[] points, int first, int second) {
        for (int i = 0; i < 3; i++) {
            float value = points[first + i];
            points[first + i] = points[second + i];
            points[second + i] = value;
        }
    }

    // ------------------------------------------------------------------
    // MD5 capes (Wavey-Capes-style cloth animation)
    // ------------------------------------------------------------------

    private static void renderMd5Cape(CosmeticCatalog.Entry entry, Mesh mesh, PlayerModel model, PoseStack pose,
                                      SubmitNodeCollector collector, int light, Identifier texture,
                                      AvatarRenderState state) {
        boolean moving = isMoving(state);
        float speed = moving ? CAPE_MOVING_SPEED : CAPE_IDLE_SPEED;
        float blend = moving ? 1f : CAPE_IDLE_BLEND;
        List<Triangle> rest = mesh.frameZero();
        // Wings already interpolate neighbouring MD5 frames. Doing the same for
        // capes removes the visible stepping that made catalog capes look as if
        // they were not animated at all at low frame rates.
        float position = animationPosition(mesh, state.ageInTicks / 20f * speed);
        int base = (int) Math.floor(position);
        List<Triangle> animated = mesh.frame(base);
        List<Triangle> next = mesh.frame(base + 1);
        float frameBlend = position - base;
        CapePivot pivot = mesh.capePivot();
        pose.pushPose();
        model.body.translateAndRotate(pose);
        pose.scale(entry.scale(), entry.scale(), entry.scale());
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, vertices) -> {
            int count = Math.min(rest.size(), animated.size());
            for (int i = 0; i < count; i++) {
                Triangle a = rest.get(i);
                Triangle b = animated.get(i);
                Triangle c = next.size() == count ? next.get(i) : b;
                emitCapeTriangle(vertices, matrix, light,
                        blend(a.first(), blend(b.first(), c.first(), frameBlend), blend),
                        blend(a.second(), blend(b.second(), c.second(), frameBlend), blend),
                        blend(a.third(), blend(b.third(), c.third(), frameBlend), blend),
                        pivot);
            }
        });
        pose.popPose();
    }

    private static void emitCapeTriangle(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                         Vertex first, Vertex second, Vertex third, CapePivot pivot) {
        Vertex a = capeVertex(first, pivot);
        Vertex b = capeVertex(second, pivot);
        Vertex c = capeVertex(third, pivot);
        Vector3f normal = normal(a, b, c);
        vertex(consumer, pose, light, a, normal, false);
        vertex(consumer, pose, light, b, normal, false);
        vertex(consumer, pose, light, c, normal, false);
        vertex(consumer, pose, light, c, normal, false);
    }

    /**
     * Maps an MD5 cape vertex from cloth space (Y is the swing direction, Z
     * runs down the cloth) into player space: the attachment edge ends up at
     * the neck, the cloth hangs down (+Y) and swings behind the body (+Z).
     */
    private static Vertex capeVertex(Vertex vertex, CapePivot pivot) {
        return new Vertex(new Vector3f(
                vertex.x(),
                pivot.pivotZ() - vertex.z(),
                vertex.y() - pivot.pivotY() + 3f), vertex.u(), vertex.v());
    }

    private static Vertex blend(Vertex rest, Vertex animated, float amount) {
        if (amount >= 1f) {
            return animated;
        }
        return new Vertex(new Vector3f(
                rest.x() + (animated.x() - rest.x()) * amount,
                rest.y() + (animated.y() - rest.y()) * amount,
                rest.z() + (animated.z() - rest.z()) * amount),
                animated.u(), animated.v());
    }

    // ------------------------------------------------------------------
    // Model-less capes (procedural wavey strip)
    // ------------------------------------------------------------------

    private static void renderWaveyCape(CosmeticCatalog.Entry entry, PlayerModel model, PoseStack pose,
                                        SubmitNodeCollector collector, int light, Identifier texture,
                                        AvatarRenderState state) {
        CapeSettings settings = CosmeticLayers.capeSettings(state);
        float[][] rows = CosmeticLayers.capeRows(state,
                settings == null ? CapeSettings.DEFAULT : settings);
        float[] rowX = rows[0];
        float[] rowY = rows[1];
        float[] rowZ = rows[2];
        float[] normalY = new float[CAPE_ROWS + 1];
        float[] normalZ = new float[CAPE_ROWS + 1];

        for (int i = 0; i <= CAPE_ROWS; i++) {
            int before = Math.max(0, i - 1);
            int after = Math.min(CAPE_ROWS, i + 1);
            float ny = -(rowZ[after] - rowZ[before]);
            float nz = rowY[after] - rowY[before];
            float length = Math.max(1.0e-4f, (float) Math.sqrt(ny * ny + nz * nz));
            normalY[i] = ny / length;
            normalZ[i] = nz / length;
        }

        CapeUv uv = capeUv(entry);
        pose.pushPose();
        model.body.translateAndRotate(pose);
        // The torso back surface is two pixels behind the model origin. Anchor
        // the sheet there so it lies on the back instead of floating away; the
        // old 4.2 px offset plus the wind bend lifted it off the shoulders.
        pose.translate(0f, 0f, 2f / 16f);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, consumer) -> {
            final float left = -5f / 16f;
            final float right = 5f / 16f;
            for (int i = 0; i < CAPE_ROWS; i++) {
                float t0 = (float) i / CAPE_ROWS;
                float t1 = (float) (i + 1) / CAPE_ROWS;
                float vTop = uv.v0() + (uv.v1() - uv.v0()) * t0;
                float vBottom = uv.v0() + (uv.v1() - uv.v0()) * t1;

                // U runs opposite to X so the artwork reads from behind. The
                // winding is left->right here to match the +Z cloth normal;
                // the earlier right->left quad had the normal and the triangle
                // facing opposite ways, which showed up as lit corners.
                capeVertex(consumer, matrix, light, left + rowX[i], rowY[i], rowZ[i],
                        uv.u1(), vTop, normalY[i], normalZ[i]);
                capeVertex(consumer, matrix, light, right + rowX[i], rowY[i], rowZ[i],
                        uv.u0(), vTop, normalY[i], normalZ[i]);
                capeVertex(consumer, matrix, light, right + rowX[i + 1], rowY[i + 1], rowZ[i + 1],
                        uv.u0(), vBottom, normalY[i + 1], normalZ[i + 1]);
                capeVertex(consumer, matrix, light, left + rowX[i + 1], rowY[i + 1], rowZ[i + 1],
                        uv.u1(), vBottom, normalY[i + 1], normalZ[i + 1]);

                // Two-sided flat cloth: no thickness slab, no side strips and
                // no caps. Those were the little hard "Dinger" at the edges.
                capeVertex(consumer, matrix, light, right + rowX[i], rowY[i], rowZ[i],
                        uv.u0(), vTop, -normalY[i], -normalZ[i]);
                capeVertex(consumer, matrix, light, left + rowX[i], rowY[i], rowZ[i],
                        uv.u1(), vTop, -normalY[i], -normalZ[i]);
                capeVertex(consumer, matrix, light, left + rowX[i + 1], rowY[i + 1], rowZ[i + 1],
                        uv.u1(), vBottom, -normalY[i + 1], -normalZ[i + 1]);
                capeVertex(consumer, matrix, light, right + rowX[i + 1], rowY[i + 1], rowZ[i + 1],
                        uv.u0(), vBottom, -normalY[i + 1], -normalZ[i + 1]);
            }
        });
        pose.popPose();
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static void capeVertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                   float x, float y, float z, float u, float v, float ny, float nz) {
        capeVertex(consumer, pose, light, x, y, z, u, v, 0f, ny, nz);
    }

    private static void capeVertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                   float x, float y, float z, float u, float v,
                                   float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static CapeUv capeUv(CosmeticCatalog.Entry entry) {
        return CAPE_UVS.computeIfAbsent(entry.texturePath(), ignored -> readCapeUv(entry));
    }

    private static CapeUv readCapeUv(CosmeticCatalog.Entry entry) {
        String resource = RESOURCE_ROOT + entry.texturePath();
        try (InputStream stream = CatalogCosmeticRenderer.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing cape texture " + resource);
            }
            NativeImage image = NativeImage.read(stream);
            try {
                int width = image.getWidth();
                int height = image.getHeight();
                if (width <= 0 || height <= 0) {
                    throw new IOException("Invalid cape texture dimensions " + resource);
                }
                float aspect = (float) width / height;
                // The standard cape atlas is 44x46 (aspect ~0.956): the outer face
                // on the left half, the inner face on the right half. Wider sheets
                // are one continuous design.
                if (aspect >= 0.90f && aspect <= 1.02f) {
                    int cellW = Math.max(2, width / 2);
                    int cellH = Math.max(2, Math.round(height * CAPE_V1));
                    int[] outer = artworkRect(image, 0, 0, cellW, cellH);
                    int[] inner = artworkRect(image, cellW, 0, cellW, cellH);
                    return new CapeUv(
                            inset(outer[0], width), inset(outer[2], width),
                            inset(inner[0], width), inset(inner[2], width),
                            inset(outer[1], height), inset(outer[3], height));
                }
                int[] full = artworkRect(image, 0, 0, width, height);
                return new CapeUv(inset(full[0], width), inset(full[2], width),
                        inset(full[0], width), inset(full[2], width),
                        inset(full[1], height), inset(full[3], height));
            } finally {
                image.close();
            }
        } catch (IOException | RuntimeException exception) {
            warn(entry, exception);
            return new CapeUv(CAPE_OUTER_U0, CAPE_OUTER_U1, CAPE_INNER_U0, CAPE_INNER_U1,
                    CAPE_V0, CAPE_V1);
        }
    }

    private static float inset(int pixel, int size) {
        return pixel / (float) size;
    }

    /**
     * Bounding box of the artwork inside one cape cell, found by peeling the
     * uniform frame these sheets are painted with. Template capes used to show
     * that frame as a wide border along the cloth, and a fixed texel inset could
     * never fit every design.
     */
    private static int[] artworkRect(NativeImage image, int x0, int y0, int w, int h) {
        int left = x0;
        int right = x0 + w;
        int top = y0;
        int bottom = y0 + h;
        if (w < 8 || h < 8) {
            return new int[]{left, top, right, bottom};
        }
        int frame = image.getPixel(x0, y0);
        int minWidth = Math.max(4, w / 2);
        int minHeight = Math.max(4, h / 2);
        boolean peeled = true;
        while (peeled) {
            peeled = false;
            if (right - left > minWidth && lineIsFrame(image, left, top, bottom, frame, true)) {
                left++;
                peeled = true;
            }
            if (right - left > minWidth && lineIsFrame(image, right - 1, top, bottom, frame, true)) {
                right--;
                peeled = true;
            }
            if (bottom - top > minHeight && lineIsFrame(image, top, left, right, frame, false)) {
                top++;
                peeled = true;
            }
            if (bottom - top > minHeight && lineIsFrame(image, bottom - 1, left, right, frame, false)) {
                bottom--;
                peeled = true;
            }
        }
        return new int[]{left, top, right, bottom};
    }

    private static boolean lineIsFrame(NativeImage image, int fixed, int from, int to, int frame, boolean column) {
        int samples = Math.min(48, to - from);
        if (samples <= 0) {
            return false;
        }
        int step = Math.max(1, (to - from) / samples);
        int matching = 0;
        int total = 0;
        for (int i = from; i < to; i += step) {
            int color = column ? image.getPixel(fixed, i) : image.getPixel(i, fixed);
            total++;
            if (color == 0 || sameColor(color, frame)) {
                matching++;
            }
        }
        return total > 0 && matching * 10 >= total * 9;
    }

    private static boolean sameColor(int a, int b) {
        if (a == b) {
            return true;
        }
        for (int shift = 0; shift < 32; shift += 8) {
            if (Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)) > 6) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasPngSignature(byte[] header) {
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (header[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Fallback planes for catalog entries without a usable model
    // ------------------------------------------------------------------

    private static void renderFallback(CosmeticCatalog.Entry entry, PlayerModel model, PoseStack pose,
                                       SubmitNodeCollector collector, int light, Identifier texture,
                                       AvatarRenderState state) {
        String type = entry.type();
        if (type.equals("WINGS")) {
            float seconds = state.ageInTicks / 20f;
            // Same rest sweep as the meshes, mirrored per half so both panels
            // angle back instead of flapping forward through the torso.
            float yaw = (float) Math.toRadians(WING_BASE_SWEEP)
                    + wingInnerYaw(seconds) + wingOuterYaw(seconds);
            pose.pushPose();
            model.body.translateAndRotate(pose);
            // Fallback panels are positioned in body space, not through
            // WING_OFFSET_Y, so apply the same 0.5 px correction here (+Y is down).
            pose.translate(0f, WING_FALLBACK_DROP, 0f);
            // Each panel hinges at its inner edge; the right one mirrors the
            // angle so the pair sweeps together instead of banking sideways.
            swingPlane(pose, collector, light, texture, -0.80f, -0.50f, -0.08f, 0.12f, 0.12f, yaw, -0.08f);
            swingPlane(pose, collector, light, texture, 0.08f, -0.50f, 0.80f, 0.12f, 0.12f, -yaw, 0.08f);
            pose.popPose();
            return;
        }

        pose.pushPose();
        if (type.equals("INSIDER_NAMETAG_ICON") || type.equals("BLC_NAMETAG_ICON")
                || type.equals("INSIDER_NAMETAG_BORDER") || type.equals("SPRAY")
                || type.equals("PROJECTILE") || type.equals("AURA") || type.equals("PLAYER_PET")) {
            model.head.translateAndRotate(pose);
            pose.translate(0f, -0.78f, -0.05f);
        } else {
            attachment(type, model).translateAndRotate(pose);
        }
        drawPlane(pose, collector, light, texture, -0.20f, -0.20f, 0.20f, 0.20f, -0.28f);
        pose.popPose();
    }

    private static void swingPlane(PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture,
                                   float x0, float y0, float x1, float y1, float z,
                                   float angle, float rootX) {
        pose.pushPose();
        pose.translate(rootX, (y0 + y1) / 2f, z);
        pose.mulPose(new Quaternionf().rotateY(angle));
        pose.translate(-rootX, -(y0 + y1) / 2f, -z);
        drawPlane(pose, collector, light, texture, x0, y0, x1, y1, z);
        pose.popPose();
    }

    private static void drawPlane(PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture,
                                  float x0, float y0, float x1, float y1, float z) {
        Vertex topLeft = new Vertex(new Vector3f(x0, y0, z), 0f, 1f);
        Vertex topRight = new Vertex(new Vector3f(x1, y0, z), 1f, 1f);
        Vertex bottomRight = new Vertex(new Vector3f(x1, y1, z), 1f, 0f);
        Vertex bottomLeft = new Vertex(new Vector3f(x0, y1, z), 0f, 0f);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, consumer) -> {
            Vector3f normal = new Vector3f(0f, 0f, 1f);
            vertex(consumer, matrix, light, topLeft, normal, false);
            vertex(consumer, matrix, light, topRight, normal, false);
            vertex(consumer, matrix, light, bottomRight, normal, false);
            vertex(consumer, matrix, light, bottomLeft, normal, false);
            Vector3f backNormal = new Vector3f(0f, 0f, -1f);
            vertex(consumer, matrix, light, bottomLeft, backNormal, false);
            vertex(consumer, matrix, light, bottomRight, backNormal, false);
            vertex(consumer, matrix, light, topRight, backNormal, false);
            vertex(consumer, matrix, light, topLeft, backNormal, false);
        });
    }

    // ------------------------------------------------------------------
    // Model loading
    // ------------------------------------------------------------------

    private static Mesh meshFor(CosmeticCatalog.Entry entry) {
        if (entry.modelPaths().isEmpty()) {
            return null;
        }
        String key = entry.modelPaths() + "|" + entry.animationPaths() + "|"
                + entry.scale() + "|" + entry.flipUVs()
                + (entry.type().equals("BANDANA") ? "|bandana" : "");
        Mesh cached = MESHES.get(key);
        if (cached != null) {
            return cached;
        }
        List<Triangle> objTriangles = new ArrayList<>();
        List<Md5Model> md5Models = new ArrayList<>();
        Md5Animation animation = null;
        try {
            for (String resource : entry.modelPaths()) {
                try (InputStream stream = CatalogCosmeticRenderer.class
                        .getResourceAsStream(RESOURCE_ROOT + resource)) {
                    if (stream == null) {
                        throw new IOException("Missing model resource " + RESOURCE_ROOT + resource);
                    }
                    String lower = resource.toLowerCase(java.util.Locale.ROOT);
                    if (lower.endsWith(".md5mesh")) {
                        md5Models.add(parseMd5Model(stream));
                    } else if (lower.endsWith(".obj")) {
                        objTriangles.addAll(parseObj(stream));
                    } else {
                        throw new IllegalArgumentException("Unsupported model format: " + resource);
                    }
                }
            }
            if (!entry.animationPaths().isEmpty()) {
                String resource = entry.animationPaths().getFirst();
                try (InputStream stream = CatalogCosmeticRenderer.class
                        .getResourceAsStream(RESOURCE_ROOT + resource)) {
                    if (stream == null) {
                        throw new IOException("Missing animation resource " + RESOURCE_ROOT + resource);
                    }
                    animation = parseMd5Animation(stream);
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            warn(entry, exception);
            return null;
        }
        Mesh loaded;
        if (!md5Models.isEmpty()) {
            List<Triangle> frameZero = poseMd5Models(md5Models, animation, 0);
            loaded = new Mesh(md5Models, animation, List.copyOf(frameZero),
                    Bounds.of(frameZero), CapePivot.of(frameZero));
        } else if (!objTriangles.isEmpty()) {
            if (entry.type().equals("BANDANA")) {
                objTriangles = inflateBandanaBand(objTriangles);
            }
            loaded = new Mesh(List.of(), null, List.copyOf(objTriangles),
                    Bounds.of(objTriangles), null);
        } else {
            warn(entry, new IllegalArgumentException("Model contains no triangles"));
            return null;
        }
        MESHES.put(key, loaded);
        return loaded;
    }

    /**
     * Widens a bandana so its band reaches {@link #BANDANA_BAND_RADIUS}. The
     * band is the top of the mesh ({@link #BANDANA_BAND_MIN_Y}); scaling the
     * whole model by one factor keeps band, knot and tails joined, and the
     * factor is derived from the authored radius so models that are already
     * wide enough (bandana_v2) only get the little they need.
     */
    private static List<Triangle> inflateBandanaBand(List<Triangle> triangles) {
        float radius = 0f;
        for (Triangle triangle : triangles) {
            for (Vertex vertex : List.of(triangle.first(), triangle.second(), triangle.third())) {
                if (vertex.y() > BANDANA_BAND_MIN_Y) {
                    radius = Math.max(radius, Math.max(Math.abs(vertex.x()), Math.abs(vertex.z())));
                }
            }
        }
        if (radius <= 0f || radius >= BANDANA_BAND_RADIUS) {
            return triangles;
        }
        float factor = BANDANA_BAND_RADIUS / radius;
        List<Triangle> result = new ArrayList<>(triangles.size());
        for (Triangle triangle : triangles) {
            result.add(new Triangle(
                    scaleHorizontal(triangle.first(), factor),
                    scaleHorizontal(triangle.second(), factor),
                    scaleHorizontal(triangle.third(), factor)));
        }
        return result;
    }

    private static Vertex scaleHorizontal(Vertex vertex, float factor) {
        return new Vertex(new Vector3f(vertex.x() * factor, vertex.y(), vertex.z() * factor),
                vertex.u(), vertex.v());
    }

    private static List<Triangle> poseMd5Models(List<Md5Model> models, Md5Animation animation, int frame) {
        List<Triangle> result = new ArrayList<>();
        for (Md5Model model : models) {
            List<Joint> joints = animation == null ? model.bindJoints() : poseJoints(animation, frame);
            result.addAll(poseTriangles(model, joints));
        }
        return result;
    }

    private static int animationFrame(Mesh mesh, float frameTime) {
        return (int) Math.floor(animationPosition(mesh, frameTime));
    }

    /**
     * Fractional position inside the clip. Keeping the remainder instead of
     * flooring it lets the renderer blend two neighbouring frames, so the
     * baked clip plays smoothly at any playback speed.
     */
    private static float animationPosition(Mesh mesh, float frameTime) {
        int frameCount = mesh.animation().frames().size();
        float position = frameTime % frameCount;
        if (position < 0f) {
            position += frameCount;
        }
        return position;
    }

    private static List<Triangle> parseObj(InputStream stream) throws IOException {
        List<Vector3f> positions = new ArrayList<>();
        List<Uv> uvs = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                switch (parts[0]) {
                    case "v" -> {
                        if (parts.length >= 4) {
                            positions.add(new Vector3f(parseFloat(parts[1]), parseFloat(parts[2]),
                                    parseFloat(parts[3])));
                        }
                    }
                    case "vt" -> {
                        if (parts.length >= 3) {
                            uvs.add(new Uv(parseFloat(parts[1]), parseFloat(parts[2])));
                        }
                    }
                    case "f" -> {
                        if (parts.length < 4) {
                            continue;
                        }
                        Vertex first = objVertex(parts[1], positions, uvs);
                        Vertex previous = objVertex(parts[2], positions, uvs);
                        for (int i = 3; i < parts.length; i++) {
                            Vertex current = objVertex(parts[i], positions, uvs);
                            triangles.add(new Triangle(first, previous, current));
                            previous = current;
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return triangles;
    }

    private static Vertex objVertex(String reference, List<Vector3f> positions, List<Uv> uvs) {
        String[] parts = reference.split("/", -1);
        Vector3f position = positions.get(resolveIndex(parts[0], positions.size()));
        Uv uv = parts.length > 1 && !parts[1].isEmpty()
                ? uvs.get(resolveIndex(parts[1], uvs.size()))
                : new Uv(0.5f, 0.5f);
        return new Vertex(new Vector3f(position), uv.u(), uv.v());
    }

    private static int resolveIndex(String raw, int size) {
        int index = Integer.parseInt(raw);
        int resolved = index > 0 ? index - 1 : size + index;
        if (resolved < 0 || resolved >= size) {
            throw new IllegalArgumentException("OBJ index is outside its vertex array");
        }
        return resolved;
    }

    private static Md5Model parseMd5Model(InputStream stream) throws IOException {
        String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        List<Joint> bindJoints = parseMd5Joints(text);
        List<Md5MeshPart> parts = new ArrayList<>();
        Matcher meshMatcher = MESH_BLOCK.matcher(text);
        while (meshMatcher.find()) {
            String block = meshMatcher.group(1);
            int vertexCount = count(block, "numverts");
            int weightCount = count(block, "numweights");
            VertexDefinition[] definitions = new VertexDefinition[vertexCount];
            Weight[] weights = new Weight[weightCount];
            List<int[]> indices = new ArrayList<>();
            for (String line : block.split("\\R")) {
                Matcher vert = VERT_LINE.matcher(line.trim());
                Matcher tri = TRI_LINE.matcher(line.trim());
                Matcher weight = WEIGHT_LINE.matcher(line.trim());
                if (vert.matches()) {
                    definitions[Integer.parseInt(vert.group(1))] = new VertexDefinition(
                            parseFloat(vert.group(2)), parseFloat(vert.group(3)),
                            Integer.parseInt(vert.group(4)), Integer.parseInt(vert.group(5)));
                } else if (tri.matches()) {
                    indices.add(new int[]{
                            Integer.parseInt(tri.group(1)),
                            Integer.parseInt(tri.group(2)),
                            Integer.parseInt(tri.group(3))});
                } else if (weight.matches()) {
                    weights[Integer.parseInt(weight.group(1))] = new Weight(
                            Integer.parseInt(weight.group(2)),
                            parseFloat(weight.group(3)),
                            new Vector3f(parseFloat(weight.group(4)),
                                    parseFloat(weight.group(5)), parseFloat(weight.group(6))));
                }
            }

            List<Md5Vertex> vertices = new ArrayList<>(vertexCount);
            for (int i = 0; i < vertexCount; i++) {
                VertexDefinition definition = definitions[i];
                if (definition == null) {
                    throw new IllegalArgumentException("MD5 mesh has a missing vertex");
                }
                List<Weight> vertexWeights = new ArrayList<>(definition.weightCount());
                for (int w = 0; w < definition.weightCount(); w++) {
                    Weight weight = weights[definition.firstWeight() + w];
                    if (weight == null || weight.joint() < 0 || weight.joint() >= bindJoints.size()) {
                        throw new IllegalArgumentException("MD5 mesh has an invalid vertex weight");
                    }
                    vertexWeights.add(weight);
                }
                vertices.add(new Md5Vertex(definition.u(), definition.v(), List.copyOf(vertexWeights)));
            }
            parts.add(new Md5MeshPart(List.copyOf(vertices), List.copyOf(indices)));
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("MD5 mesh has no meshes");
        }
        return new Md5Model(List.copyOf(bindJoints), List.copyOf(parts));
    }

    private static List<Joint> parseMd5Joints(String text) {
        Matcher jointsMatcher = JOINTS_BLOCK.matcher(text);
        if (!jointsMatcher.find()) {
            throw new IllegalArgumentException("MD5 mesh has no joints block");
        }
        List<Joint> joints = new ArrayList<>();
        Matcher jointMatcher = JOINT_LINE.matcher(jointsMatcher.group(1));
        while (jointMatcher.find()) {
            int parent = Integer.parseInt(jointMatcher.group(2));
            Vector3f position = new Vector3f(
                    parseFloat(jointMatcher.group(3)),
                    parseFloat(jointMatcher.group(4)),
                    parseFloat(jointMatcher.group(5)));
            float x = parseFloat(jointMatcher.group(6));
            float y = parseFloat(jointMatcher.group(7));
            float z = parseFloat(jointMatcher.group(8));
            float w = -(float) Math.sqrt(Math.max(0f, 1f - x * x - y * y - z * z));
            Quaternionf rotation = new Quaternionf(x, y, z, w).normalize();
            if (parent >= 0) {
                if (parent >= joints.size()) {
                    throw new IllegalArgumentException("MD5 joint parent is out of order");
                }
                Joint parentJoint = joints.get(parent);
                position.rotate(parentJoint.rotation()).add(parentJoint.position());
                rotation = new Quaternionf(parentJoint.rotation()).mul(rotation).normalize();
            }
            joints.add(new Joint(position, rotation));
        }
        if (joints.isEmpty()) {
            throw new IllegalArgumentException("MD5 mesh has no joints");
        }
        return joints;
    }

    private static Md5Animation parseMd5Animation(InputStream stream) throws IOException {
        String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        Matcher hierarchyMatcher = HIERARCHY_BLOCK.matcher(text);
        if (!hierarchyMatcher.find()) {
            throw new IllegalArgumentException("MD5 animation has no hierarchy block");
        }
        List<AnimJoint> hierarchy = new ArrayList<>();
        Matcher lineMatcher = HIERARCHY_LINE.matcher(hierarchyMatcher.group(1));
        while (lineMatcher.find()) {
            hierarchy.add(new AnimJoint(Integer.parseInt(lineMatcher.group(2)),
                    Integer.parseInt(lineMatcher.group(3)), Integer.parseInt(lineMatcher.group(4))));
        }
        Matcher baseMatcher = BASEFRAME_BLOCK.matcher(text);
        if (!baseMatcher.find()) {
            throw new IllegalArgumentException("MD5 animation has no baseframe block");
        }
        List<BaseJoint> baseFrame = new ArrayList<>();
        Matcher baseLine = BASEFRAME_LINE.matcher(baseMatcher.group(1));
        while (baseLine.find()) {
            baseFrame.add(new BaseJoint(
                    new Vector3f(parseFloat(baseLine.group(1)), parseFloat(baseLine.group(2)),
                            parseFloat(baseLine.group(3))),
                    parseFloat(baseLine.group(4)), parseFloat(baseLine.group(5)),
                    parseFloat(baseLine.group(6))));
        }
        if (hierarchy.isEmpty() || baseFrame.size() != hierarchy.size()) {
            throw new IllegalArgumentException("MD5 animation hierarchy does not match its baseframe");
        }
        List<float[]> frames = new ArrayList<>();
        Matcher frameMatcher = FRAME_BLOCK.matcher(text);
        while (frameMatcher.find()) {
            String[] values = frameMatcher.group(1).trim().split("\\s+");
            float[] frame = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                frame[i] = parseFloat(values[i]);
            }
            frames.add(frame);
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("MD5 animation has no frames");
        }
        int frameCount = frames.size();
        Matcher numFrames = NUM_FRAMES.matcher(text);
        if (numFrames.find()) {
            int declared = Integer.parseInt(numFrames.group(1));
            if (declared > 0 && declared <= frames.size()) {
                frames = frames.subList(0, declared);
                frameCount = declared;
            }
        }
        int frameRate = 24;
        Matcher rateMatcher = FRAME_RATE.matcher(text);
        if (rateMatcher.find()) {
            frameRate = Math.max(1, Integer.parseInt(rateMatcher.group(1)));
        }
        return new Md5Animation(List.copyOf(hierarchy), List.copyOf(baseFrame),
                List.copyOf(frames), frameCount, frameRate);
    }

    private static List<Joint> poseJoints(Md5Animation animation, int frameIndex) {
        int frameCount = animation.frameCount();
        int index = ((frameIndex % frameCount) + frameCount) % frameCount;
        float[] values = animation.frames().get(index);
        List<Joint> joints = new ArrayList<>(animation.hierarchy().size());
        for (int i = 0; i < animation.hierarchy().size(); i++) {
            AnimJoint hierarchy = animation.hierarchy().get(i);
            BaseJoint base = animation.baseFrame().get(i);
            float px = base.position().x;
            float py = base.position().y;
            float pz = base.position().z;
            float qx = base.qx();
            float qy = base.qy();
            float qz = base.qz();
            int component = hierarchy.start();
            if ((hierarchy.flags() & 1) != 0) {
                px = component(values, component++);
            }
            if ((hierarchy.flags() & 2) != 0) {
                py = component(values, component++);
            }
            if ((hierarchy.flags() & 4) != 0) {
                pz = component(values, component++);
            }
            if ((hierarchy.flags() & 8) != 0) {
                qx = component(values, component++);
            }
            if ((hierarchy.flags() & 16) != 0) {
                qy = component(values, component++);
            }
            if ((hierarchy.flags() & 32) != 0) {
                qz = component(values, component++);
            }
            float qw = -(float) Math.sqrt(Math.max(0f, 1f - qx * qx - qy * qy - qz * qz));
            Quaternionf rotation = new Quaternionf(qx, qy, qz, qw).normalize();
            Vector3f position = new Vector3f(px, py, pz);
            if (hierarchy.parent() >= 0) {
                if (hierarchy.parent() >= joints.size()) {
                    throw new IllegalArgumentException("MD5 animation hierarchy is out of order");
                }
                Joint parent = joints.get(hierarchy.parent());
                position.rotate(parent.rotation()).add(parent.position());
                rotation = new Quaternionf(parent.rotation()).mul(rotation).normalize();
            }
            joints.add(new Joint(position, rotation));
        }
        return joints;
    }

    private static float component(float[] values, int index) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("MD5 animation frame is too short");
        }
        return values[index];
    }

    private static List<Triangle> poseTriangles(Md5Model model, List<Joint> joints) {
        List<Triangle> result = new ArrayList<>();
        for (Md5MeshPart part : model.parts()) {
            Vertex[] vertices = new Vertex[part.vertices().size()];
            for (int i = 0; i < vertices.length; i++) {
                Md5Vertex vertex = part.vertices().get(i);
                Vector3f position = new Vector3f();
                for (Weight weight : vertex.weights()) {
                    if (weight.joint() < 0 || weight.joint() >= joints.size()) {
                        throw new IllegalArgumentException("MD5 vertex references an invalid joint");
                    }
                    Joint joint = joints.get(weight.joint());
                    position.add(new Vector3f(weight.position())
                            .rotate(joint.rotation())
                            .add(joint.position())
                            .mul(weight.bias()));
                }
                vertices[i] = new Vertex(position, vertex.u(), vertex.v());
            }
            for (int[] triangle : part.triangles()) {
                if (triangle[0] >= vertices.length || triangle[1] >= vertices.length
                        || triangle[2] >= vertices.length) {
                    throw new IllegalArgumentException("MD5 mesh has an invalid triangle index");
                }
                result.add(new Triangle(vertices[triangle[0]], vertices[triangle[1]], vertices[triangle[2]]));
            }
        }
        return result;
    }

    private static int count(String block, String name) {
        Matcher matcher = Pattern.compile("\\b" + name + "\\s+(\\d+)").matcher(block);
        if (!matcher.find()) {
            throw new IllegalArgumentException("MD5 mesh is missing " + name);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static float parseFloat(String value) {
        float parsed = Float.parseFloat(value);
        if (!Float.isFinite(parsed)) {
            throw new IllegalArgumentException("Model contains a non-finite number");
        }
        return parsed;
    }

    private static Vector3f normal(Vertex first, Vertex second, Vertex third) {
        Vector3f a = new Vector3f(second.x() - first.x(), second.y() - first.y(), second.z() - first.z());
        Vector3f b = new Vector3f(third.x() - first.x(), third.y() - first.y(), third.z() - first.z());
        Vector3f normal = a.cross(b);
        if (normal.lengthSquared() < 1.0e-8f) {
            return new Vector3f(0f, 1f, 0f);
        }
        return normal.normalize();
    }

    private static void emitTriangle(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                     Triangle triangle, boolean flipUVs) {
        emitTriangle(consumer, pose, light, triangle, flipUVs, 0xFFFFFFFF);
    }

    private static void emitTriangle(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                     Triangle triangle, boolean flipUVs, int color) {
        emitTriangle(consumer, pose, light, triangle, flipUVs, color, false);
    }

    private static void emitTriangle(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                     Triangle triangle, boolean flipUVs, int color, boolean mirrorBackU) {
        emitTriangle(consumer, pose, light, triangle, flipUVs, color, false, mirrorBackU);
    }

    private static void emitTriangle(VertexConsumer consumer, PoseStack.Pose pose, int light,
                                     Triangle triangle, boolean flipUVs, int color,
                                     boolean mirrorFrontU, boolean mirrorBackU) {
        Vector3f normal = normal(triangle.first(), triangle.second(), triangle.third());
        vertex(consumer, pose, light, triangle.first(), normal, flipUVs, mirrorFrontU, color);
        vertex(consumer, pose, light, triangle.second(), normal, flipUVs, mirrorFrontU, color);
        vertex(consumer, pose, light, triangle.third(), normal, flipUVs, mirrorFrontU, color);
        vertex(consumer, pose, light, triangle.third(), normal, flipUVs, mirrorFrontU, color);

        Vector3f back = new Vector3f(-normal.x(), -normal.y(), -normal.z());
        vertex(consumer, pose, light, triangle.first(), back, flipUVs, mirrorBackU, color);
        vertex(consumer, pose, light, triangle.third(), back, flipUVs, mirrorBackU, color);
        vertex(consumer, pose, light, triangle.second(), back, flipUVs, mirrorBackU, color);
        vertex(consumer, pose, light, triangle.second(), back, flipUVs, mirrorBackU, color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               Vertex vertex, Vector3f normal, boolean flipUVs) {
        vertex(consumer, pose, light, vertex, normal, flipUVs, 0xFFFFFFFF);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               Vertex vertex, Vector3f normal, boolean flipUVs, int color) {
        vertex(consumer, pose, light, vertex, normal, flipUVs, false, color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               Vertex vertex, Vector3f normal, boolean flipUVs, boolean mirrorU, int color) {
        float u = mirrorU ? 1f - vertex.u() : vertex.u();
        vertex(consumer, pose, light, vertex.x(), vertex.y(), vertex.z(), u, vertex.v(),
                normal.x, normal.y, normal.z, flipUVs, color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, boolean flipUVs) {
        vertex(consumer, pose, light, x, y, z, u, v, nx, ny, nz, flipUVs, 0xFFFFFFFF);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, boolean flipUVs, int color) {
        float flippedV = flipUVs ? 1f - v : v;
        consumer.addVertex(pose, x, y, z)
                .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .setUv(u, flippedV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static void warn(CosmeticCatalog.Entry entry, Exception exception) {
        if (WARNED.add(entry.key())) {
            System.err.println("[OctoClient] Could not load cosmetic model " + entry.key() + ": "
                    + exception.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    private record Uv(float u, float v) {
    }

    private record Vertex(Vector3f position, float u, float v) {
        private float x() {
            return position.x;
        }

        private float y() {
            return position.y;
        }

        private float z() {
            return position.z;
        }
    }

    private record Triangle(Vertex first, Vertex second, Vertex third) {
    }

    private record CapeUv(float u0, float u1, float innerU0, float innerU1, float v0, float v1) {
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        private static Bounds of(List<Triangle> triangles) {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            float maxZ = -Float.MAX_VALUE;
            for (Triangle triangle : triangles) {
                for (Vertex vertex : List.of(triangle.first(), triangle.second(), triangle.third())) {
                    minX = Math.min(minX, vertex.x());
                    minY = Math.min(minY, vertex.y());
                    minZ = Math.min(minZ, vertex.z());
                    maxX = Math.max(maxX, vertex.x());
                    maxY = Math.max(maxY, vertex.y());
                    maxZ = Math.max(maxZ, vertex.z());
                }
            }
            if (minX > maxX) {
                return new Bounds(0f, 0f, 0f, 0f, 0f, 0f);
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    /**
     * One frame of the shared wing flutter for catalog wings. {@code base} is
     * the constant rest yaw that angles the sheet back; {@code inner} and
     * {@code outer} are the oscillating radians around the vertical axis,
     * growing from the pinned {@code rootX} edge (the attachment at the spine)
     * towards {@code tipX}.
     */
    private record WingSwing(float base, float inner, float outer, float rootX, float tipX) {
        private static WingSwing at(Mesh mesh, float seconds) {
            Bounds bounds = mesh.bounds();
            // The authored half attaches at whichever X edge sits closest to
            // the spine; that edge is the hinge and stays fixed.
            boolean minNearer = Math.abs(bounds.minX()) <= Math.abs(bounds.maxX());
            float rootX = minNearer ? bounds.minX() : bounds.maxX();
            float tipX = minNearer ? bounds.maxX() : bounds.minX();
            if (Math.abs(tipX - rootX) < 1.0e-3f) {
                return null;
            }
            // A left-authored half (tip left of the hinge) needs +40 to sweep
            // back, a right-authored one -40. A positive yaw moves the sheet
            // towards +Z, which is behind the player, so both halves angle
            // away from the body instead of into it.
            float base = (float) (-Math.signum(tipX - rootX) * Math.toRadians(WING_BASE_SWEEP));
            return new WingSwing(base, wingInnerYaw(seconds), wingOuterYaw(seconds), rootX, tipX);
        }
    }

    /** Yaw of the inner wing bone in radians, from {@code nrc.idle}. */
    private static float wingInnerYaw(float seconds) {
        // Molang's sin/cos take degrees, so the whole phase runs in degrees and
        // only the final bone rotation is converted to radians.
        double t = seconds * WING_FLUTTER_RATE;
        double angle = WING_INNER_SWING * Math.sin(Math.toRadians(t + WING_INNER_PHASE))
                + WING_INNER_HARMONIC * Math.sin(Math.toRadians(t + WING_INNER_PHASE + WING_HARMONIC_PHASE));
        return (float) Math.toRadians(angle);
    }

    /** Yaw of the outer wing bone in radians, from {@code nrc.idle}. */
    private static float wingOuterYaw(float seconds) {
        double t = seconds * WING_FLUTTER_RATE;
        double angle = WING_OUTER_SWING * Math.sin(Math.toRadians(t + WING_OUTER_PHASE))
                + WING_OUTER_HARMONIC * Math.sin(Math.toRadians(t + WING_OUTER_PHASE + WING_HARMONIC_PHASE));
        return (float) Math.toRadians(angle);
    }

    /** Attachment edge of an MD5 cape in its authored cloth space. */
    private record CapePivot(float pivotY, float pivotZ) {
        private static CapePivot of(List<Triangle> triangles) {
            float maxZ = -Float.MAX_VALUE;
            for (Triangle triangle : triangles) {
                maxZ = Math.max(maxZ, Math.max(triangle.first().z(),
                        Math.max(triangle.second().z(), triangle.third().z())));
            }
            float sumY = 0f;
            int count = 0;
            for (Triangle triangle : triangles) {
                for (Vertex vertex : List.of(triangle.first(), triangle.second(), triangle.third())) {
                    if (vertex.z() >= maxZ - 0.5f) {
                        sumY += vertex.y();
                        count++;
                    }
                }
            }
            return new CapePivot(count == 0 ? 0f : sumY / count, maxZ);
        }
    }

    private record VertexDefinition(float u, float v, int firstWeight, int weightCount) {
    }

    private record Weight(int joint, float bias, Vector3f position) {
    }

    private record Joint(Vector3f position, Quaternionf rotation) {
    }

    private record Md5Vertex(float u, float v, List<Weight> weights) {
    }

    private record Md5MeshPart(List<Md5Vertex> vertices, List<int[]> triangles) {
    }

    private record Md5Model(List<Joint> bindJoints, List<Md5MeshPart> parts) {
    }

    private record AnimJoint(int parent, int flags, int start) {
    }

    private record BaseJoint(Vector3f position, float qx, float qy, float qz) {
    }

    private record Md5Animation(List<AnimJoint> hierarchy, List<BaseJoint> baseFrame,
                                List<float[]> frames, int frameCount, int frameRate) {
    }

    /**
     * A parsed model plus its rest pose. Animated meshes cache the skinned
     * triangles per animation frame so a crowd of players sharing a cosmetic
     * does not re-skin the same mesh every render pass.
     */
    private static final class Mesh {
        private final List<Md5Model> md5Models;
        private final Md5Animation animation;
        private final List<Triangle> frameZero;
        private final Bounds bounds;
        private final CapePivot capePivot;
        private final Map<Integer, List<Triangle>> frameCache = new HashMap<>();

        private Mesh(List<Md5Model> md5Models, Md5Animation animation, List<Triangle> frameZero,
                     Bounds bounds, CapePivot capePivot) {
            this.md5Models = md5Models;
            this.animation = animation;
            this.frameZero = frameZero;
            this.bounds = bounds;
            this.capePivot = capePivot;
        }

        private boolean md5() {
            return !md5Models.isEmpty();
        }

        private boolean animated() {
            return !md5Models.isEmpty() && animation != null;
        }

        private List<Triangle> frameZero() {
            return frameZero;
        }

        private Bounds bounds() {
            return bounds;
        }

        private CapePivot capePivot() {
            return capePivot;
        }

        private Md5Animation animation() {
            return animation;
        }

        private List<Triangle> frame(int frameIndex) {
            if (md5Models.isEmpty() || animation == null) {
                return frameZero;
            }
            int index = ((frameIndex % animation.frameCount()) + animation.frameCount())
                    % animation.frameCount();
            return frameCache.computeIfAbsent(index, ignored -> {
                List<Triangle> posed = new ArrayList<>();
                for (Md5Model model : md5Models) {
                    posed.addAll(poseTriangles(model, poseJoints(animation, index)));
                }
                return List.copyOf(posed);
            });
        }
    }
}
