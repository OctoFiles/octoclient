package dev.octoclient.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.cosmetics.Aura;
import dev.octoclient.module.cosmetics.AuraDesigns;
import dev.octoclient.module.cosmetics.Accessory;
import dev.octoclient.module.cosmetics.AccessoryDesigns;
import dev.octoclient.module.cosmetics.Bandana;
import dev.octoclient.module.cosmetics.BandanaDesigns;
import dev.octoclient.module.cosmetics.Cape;
import dev.octoclient.module.cosmetics.CosmeticCatalog;
import dev.octoclient.module.cosmetics.CosmeticPreviewState;
import dev.octoclient.module.cosmetics.Halo;
import dev.octoclient.module.cosmetics.Hat;
import dev.octoclient.module.cosmetics.HatDesigns;
import dev.octoclient.module.cosmetics.WingDesigns;
import dev.octoclient.module.cosmetics.Wings;
import dev.octoclient.network.CapeSettings;
import dev.octoclient.network.CapeSyncClient;
import dev.octoclient.network.CosmeticLoadout;
import dev.octoclient.network.CosmeticSyncClient;
import dev.octoclient.util.Anim;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side player cosmetics.
 *
 * <p>The layers draw client-side textures and geometry, and copy the vanilla
 * part poses (body for the cape, head for the hat) instead of inventing a
 * coordinate space. Only bundled assets are used.</p>
 *
 * <p>Local cosmetics are always rendered. Other players' cosmetics come from
 * the server relay or the shared API; their cape is then simulated locally
 * with the wearer's synced settings instead of the vanilla rigid pose.</p>
 */
public final class CosmeticLayers {
    private static final Identifier WHITE =
            Identifier.fromNamespaceAndPath("octoclient", "textures/misc/white.png");
    private CosmeticLayers() {
    }

    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (!(renderer instanceof AvatarRenderer)) {
                return;
            }
            @SuppressWarnings("unchecked")
            RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                    (RenderLayerParent<AvatarRenderState, PlayerModel>) renderer;
            helper.register(new CapeLayer(parent));
            helper.register(new BandanaLayer(parent));
            helper.register(new AccessoryLayer(parent));
            helper.register(new WingsLayer(parent));
            helper.register(new HatLayer(parent));
            helper.register(new HaloLayer(parent));
            helper.register(new AuraLayer(parent));
            helper.register(new CatalogLayer(parent));
        });
        // WaveyCapes advances cloth once per game tick, not once per render
        // frame. Rendering only reads the resulting points.
        ClientTickEvents.END_CLIENT_TICK.register(CapeLayer::tick);
    }

    private static boolean isLocalPlayer(AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && state.id == minecraft.player.getId();
    }

    /**
     * Cosmetics the rendered player is wearing: your own selection for yourself,
     * the relayed selection for everyone else.
     */
    private static CosmeticLoadout loadoutOf(AvatarRenderState state) {
        if (state instanceof CosmeticPreviewState preview && preview.octoclient$isCosmeticPreview()) {
            CosmeticLoadout loadout = preview.octoclient$getCosmeticPreviewLoadout();
            return loadout == null ? CosmeticLoadout.EMPTY : loadout;
        }
        if (isLocalPlayer(state)) {
            return CosmeticSyncClient.localLoadout();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return CosmeticLoadout.EMPTY;
        }
        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player)) {
            return CosmeticLoadout.EMPTY;
        }
        return CosmeticSyncClient.remoteLoadout(player.getUUID());
    }

    private static UUID playerId(AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(state.id);
        if (entity != null) {
            return entity.getUUID();
        }
        return isLocalPlayer(state) && minecraft.player != null ? minecraft.player.getUUID() : null;
    }

    /** Motion settings for whichever cape the rendered player is wearing. */
    static CapeSettings capeSettings(AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean localPlayer = isLocalPlayer(state);
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(state.id);
        Cape cape = localPlayer ? CapeLayer.selectedLocalCape()
                : (entity instanceof Player player ? CapeLayer.selectedRemoteCape(player) : null);
        UUID id = playerId(state);
        if (cape != null) {
            return CapeLayer.settingsFor(cape, id, localPlayer);
        }
        if (entity instanceof Player player) {
            return settingsForCatalog(player, localPlayer);
        }
        return null;
    }

    /** Catalog capes have no module settings, so they use the WaveyCapes defaults. */
    private static CapeSettings settingsForCatalog(Player player, boolean localPlayer) {
        CosmeticLoadout loadout = localPlayer ? CosmeticSyncClient.localLoadout()
                : CosmeticSyncClient.remoteLoadout(player.getUUID());
        String selection = loadout.catalogSelection();
        if (selection == null || !selection.contains("CAPE:")) {
            return null;
        }
        UUID id = player.getUUID();
        if (!localPlayer && CapeSyncClient.hasRemoteSettings(id)) {
            return CapeSyncClient.remoteSettings(id);
        }
        return CapeSettings.DEFAULT;
    }

    /**
     * Shared WaveyCapes cloth geometry. Both the dedicated cape layer and the
     * catalog renderer read the same per-player simulation, so a catalog cape
     * moves exactly like a module cape instead of using a weak local sine wave.
     */
    public static float[][] capeRows(AvatarRenderState state, CapeSettings settings) {
        float[] rowX = new float[CapeLayer.PART_COUNT + 1];
        float[] rowY = new float[CapeLayer.PART_COUNT + 1];
        float[] rowZ = new float[CapeLayer.PART_COUNT + 1];
        Minecraft minecraft = Minecraft.getInstance();
        UUID id = playerId(state);
        CapePhysics.Simulation simulation = id == null ? null
                : CapeLayer.simulationFor(id, settings.movement());
        boolean simulated = !settings.movement().equals("Classic") && simulation != null;
        boolean moving = CapeLayer.isMoving(state);
        boolean waves = settings.windMode().equals("Waves");
        float delta = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        if (simulated) {
            for (int i = 0; i <= CapeLayer.PART_COUNT; i++) {
                rowX[i] = (simulation.lerpZ(i, delta) - simulation.lerpZ(0, delta))
                        * CapeLayer.SEGMENT;
                rowY[i] = -(simulation.lerpY(i, delta) - simulation.lerpY(0, delta))
                        * CapeLayer.SEGMENT;
                rowZ[i] = -(simulation.lerpX(i, delta) - simulation.lerpX(0, delta))
                        * CapeLayer.SEGMENT;
            }
            for (int i = 0; i <= CapeLayer.PART_COUNT; i++) {
                float wave = waves ? CapeLayer.windSwing(i / (float) CapeLayer.PART_COUNT) : 0f;
                float angle = (float) Math.toRadians(6f + wave);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float y = rowY[i];
                float z = rowZ[i];
                rowY[i] = y * cos - z * sin;
                rowZ[i] = y * sin + z * cos;
            }
        } else {
            float side = (float) Math.toRadians(moving ? Anim.clamp01(state.capeLean2) / 2f : 0f);
            for (int i = 0; i <= CapeLayer.PART_COUNT; i++) {
                float t = i / (float) CapeLayer.PART_COUNT;
                float degrees = 6f + (moving
                        ? Anim.clamp01(state.capeLean) / 2f + Anim.clamp01(state.capeFlap)
                        : 0f);
                if (waves) {
                    degrees += CapeLayer.windSwing(t);
                }
                float angle = (float) Math.toRadians(degrees);
                rowX[i] = (float) Math.sin(side) * i * CapeLayer.SEGMENT;
                rowY[i] = (float) Math.cos(angle) * i * CapeLayer.SEGMENT;
                rowZ[i] = (float) Math.sin(angle) * i * CapeLayer.SEGMENT;
            }
        }

        // A standing player's simulation settles into a vertical line: the
        // constant 6-degree bend then rotates the lower rows towards the torso
        // and the cape disappears into the armour. Keep every row behind the
        // back surface, with the clearance growing towards the hem.
        for (int i = 1; i <= CapeLayer.PART_COUNT; i++) {
            float t = i / (float) CapeLayer.PART_COUNT;
            float minBack = (3f / 16f) * t;
            if (rowZ[i] < minBack) {
                rowZ[i] = minBack;
            }
        }

        // A visible travelling wave on top of the settled simulation. It runs
        // even while the player stands still, so the cloth never looks frozen.
        // The offset above keeps the negative half of the wave outside the body.
        float phase = System.currentTimeMillis() * 0.0032f;
        for (int i = 1; i <= CapeLayer.PART_COUNT; i++) {
            float t = i / (float) CapeLayer.PART_COUNT;
            float ripple = (float) Math.sin(phase - t * 3.0f);
            rowZ[i] += (0.06f + ripple * 0.05f) * t;
            rowY[i] += (ripple * 0.5f + 0.5f) * 0.06f * t;
            rowX[i] += 0.12f * t * (float) Math.sin(phase * 0.7f - t * 2.0f);
        }
        return new float[][] {rowX, rowY, rowZ};
    }

    /** Dev capture helper: proves whether the local cape is really simulated. */
    static String capeDebug() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return "capeSim=no-player";
        }
        UUID id = minecraft.player.getUUID();
        Cape cape = CapeLayer.selectedLocalCape();
        CosmeticLoadout loadout = CosmeticSyncClient.localLoadout();
        boolean catalogCape = loadout.catalogSelection().contains("CAPE:");
        CapeSettings settings = cape != null ? CapeLayer.settingsFor(cape, id, true)
                : (catalogCape ? CapeSettings.DEFAULT : null);
        boolean simulated = settings != null && CapeLayer.simulationFor(id, settings.movement()) != null;
        return "capeSim=" + simulated
                + " movement=" + (settings == null ? "none" : settings.movement())
                + " catalog=" + catalogCape;
    }

    private static final class BandanaLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private BandanaLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            CosmeticLoadout loadout = loadoutOf(state);
            String designName = loadout.bandana();
            if (designName.isEmpty()) {
                return;
            }
            BandanaDesigns.Design design = BandanaDesigns.byName(designName);
            if (design == null) {
                return;
            }
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            // Keep the bandana outside 3D hair/skin layers instead of letting
            // the back of the head cut through it. Lowered by 1 model pixel, so
            // the band does not ride above the hairline.
            pose.translate(0f, -1.8f / 16f, -0.7f / 16f);
            // WingModelRenderer's built-in Bedrock X mirror is required here:
            // Bedrock authors +X as the wearer's right, Java as -X. Cancelling
            // it kept the design on the wrong side of the head (falsch herum).
            // The Y flip puts the band on the crown; the Z direction below is
            // the half-turn that puts the knot at the back.
            WingModelRenderer.render(
                    design.texturePath(),
                    design.geometryResource(),
                    design.animationResource(),
                    design.texture(),
                    "armorHead",
                    24.016f,
                    -1f,
                    // The Bedrock bandana meshes model the knot/ribbon on +Z,
                    // which is already the back of the head in Java model
                    // space. Flipping Z as well left the knot on the forehead;
                    // keeping Z unflipped is the extra half-turn that moves it
                    // behind the head.
                    +1f,
                    false,
                    getParentModel().head,
                    pose,
                    collector,
                    light,
                    // Bandanas are headwear, not capes: freeze the imported
                    // idle clip so the knot and tails never wave like cloth.
                    0f);
            pose.popPose();
        }
    }

    /**
     * Cape drawn as the same 16-part sheet WaveyCapes renders. Every visible
     * wearer gets a local simulation: the local player's own settings, or the
     * remote wearer's synced settings from the server/API. The sheet is drawn
     * with shared corner positions and normals (Smooth) or separate strips
     * (Blocky), and its side/top/bottom edges are closed so no corners show.
     */
    private static final class CapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        /** WaveyCapes' PART_COUNT and CAPE_WIDTH / 2. */
        private static final int PART_COUNT = 16;
        private static final float SEGMENT = 1f / PART_COUNT;
        private static final float HALF_WIDTH = 5f / 16f;
        /** One WaveyCapes simulation per visible player, keyed by UUID. */
        private static final Map<UUID, CapePhysics.Simulation> simulations = new HashMap<>();
        private static final Map<UUID, String> simulationModes = new HashMap<>();

        private CapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean localPlayer = isLocalPlayer(state);
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(state.id);
            UUID playerId = entity != null ? entity.getUUID()
                    : (localPlayer && minecraft.player != null ? minecraft.player.getUUID() : null);
            Cape cape = localPlayer ? selectedLocalCape()
                    : (entity instanceof Player player ? selectedRemoteCape(player) : null);
            if (cape == null) {
                return;
            }
            CapeSettings settings = capeSettings(state);
            if (settings == null) {
                return;
            }
            boolean smooth = settings.style().equals("Smooth");
            float[][] rows = capeRows(state, settings);
            final float[] rowX = rows[0];
            final float[] rowY = rows[1];
            final float[] rowZ = rows[2];

            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            // WaveyCapes translates the cape 0.125 blocks behind the torso.
            pose.translate(0f, 0f, 0.125f);

            final float[] normalY = new float[PART_COUNT + 1];
            final float[] normalZ = new float[PART_COUNT + 1];
            for (int i = 0; i <= PART_COUNT; i++) {
                float dy;
                float dz;
                if (i == 0) {
                    dy = rowY[1] - rowY[0];
                    dz = rowZ[1] - rowZ[0];
                } else if (i == PART_COUNT) {
                    dy = rowY[PART_COUNT] - rowY[PART_COUNT - 1];
                    dz = rowZ[PART_COUNT] - rowZ[PART_COUNT - 1];
                } else {
                    dy = smooth ? rowY[i + 1] - rowY[i - 1] : rowY[i + 1] - rowY[i];
                    dz = smooth ? rowZ[i + 1] - rowZ[i - 1] : rowZ[i + 1] - rowZ[i];
                }
                float ny = -dz;
                float nz = dy;
                float length = Math.max(1.0e-4f, (float) Math.sqrt(ny * ny + nz * nz));
                normalY[i] = ny / length;
                normalZ[i] = nz / length;
            }

            // The cloth has real thickness. With translucent depth writes off,
            // submission order decides what is visible, so the faces closest to
            // the camera (outer face, then the edge caps) must come last.
            final float thickness = 1f / 16f;
            RenderType capeType = RenderTypes.entityTranslucent(cape.texture());
            collector.submitCustomGeometry(pose, capeType, (p, vc) -> {
                for (int i = 0; i < PART_COUNT; i++) {
                    float vTop = cape.textureV0() + (cape.textureV1() - cape.textureV0()) * i / PART_COUNT;
                    float vBottom = cape.textureV0()
                            + (cape.textureV1() - cape.textureV0()) * (i + 1) / PART_COUNT;
                    float u0 = cape.textureU0();
                    float u1 = cape.textureU1();

                    float topY = rowY[i];
                    float topZ = rowZ[i];
                    float bottomY = rowY[i + 1];
                    float bottomZ = rowZ[i + 1];
                    float topX = rowX[i];
                    float bottomX = rowX[i + 1];
                    float topNY = normalY[i];
                    float topNZ = normalZ[i];
                    float bottomNY = smooth ? normalY[i + 1] : normalY[i];
                    float bottomNZ = smooth ? normalZ[i + 1] : normalZ[i];

                    float innerTopY = topY - topNY * thickness;
                    float innerTopZ = topZ - topNZ * thickness;
                    float innerBottomY = bottomY - bottomNY * thickness;
                    float innerBottomZ = bottomZ - bottomNZ * thickness;

                    // Inner face first: it is the farthest from a rear camera.
                    sheetVertex(vc, p, light, -HALF_WIDTH + topX, innerTopY, innerTopZ, u0, vTop,
                            -topNY, -topNZ, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, HALF_WIDTH + topX, innerTopY, innerTopZ, u1, vTop,
                            -topNY, -topNZ, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, HALF_WIDTH + bottomX, innerBottomY, innerBottomZ,
                            u1, vBottom, -bottomNY, -bottomNZ, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, -HALF_WIDTH + bottomX, innerBottomY, innerBottomZ,
                            u0, vBottom, -bottomNY, -bottomNZ, 255, 255, 255, 255);

                    // Left and right edges close the slab so no seam shows.
                    // Their normals point along the width, not along the cloth,
                    // otherwise every side corner lights up like a top cap.
                    sheetVertex(vc, p, light, -HALF_WIDTH + topX, innerTopY, innerTopZ, u0, vTop,
                            -1f, 0f, 0f, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, -HALF_WIDTH + bottomX, innerBottomY, innerBottomZ,
                            u0, vBottom, -1f, 0f, 0f, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, -HALF_WIDTH + bottomX, bottomY, bottomZ, u0, vBottom,
                            -1f, 0f, 0f, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, -HALF_WIDTH + topX, topY, topZ, u0, vTop,
                            -1f, 0f, 0f, 255, 255, 255, 255);

                    sheetVertex(vc, p, light, HALF_WIDTH + topX, topY, topZ, u1, vTop,
                            1f, 0f, 0f, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, HALF_WIDTH + bottomX, bottomY, bottomZ, u1, vBottom,
                            1f, 0f, 0f, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, HALF_WIDTH + bottomX, innerBottomY, innerBottomZ,
                            u1, vBottom, 1f, 0f, 0f, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, HALF_WIDTH + topX, innerTopY, innerTopZ, u1, vTop,
                            1f, 0f, 0f, 255, 255, 255, 255);

                    // Outer face last so it can never be overpainted.
                    sheetVertex(vc, p, light, HALF_WIDTH + topX, topY, topZ, u1, vTop,
                            topNY, topNZ, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, -HALF_WIDTH + topX, topY, topZ, u0, vTop,
                            topNY, topNZ, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, -HALF_WIDTH + bottomX, bottomY, bottomZ, u0, vBottom,
                            bottomNY, bottomNZ, 255, 255, 255, 255);
                    sheetVertex(vc, p, light, HALF_WIDTH + bottomX, bottomY, bottomZ, u1, vBottom,
                            bottomNY, bottomNZ, 255, 255, 255, 255);
                }

                // Close the top and bottom edges of the slab.
                float topNY = normalY[0];
                float topNZ = normalZ[0];
                float bottomNY = normalY[PART_COUNT];
                float bottomNZ = normalZ[PART_COUNT];
                float u0 = cape.textureU0();
                float u1 = cape.textureU1();
                float vTop = cape.textureV0();
                float vBottom = cape.textureV1();
                float innerTopY = rowY[0] - topNY * thickness;
                float innerTopZ = rowZ[0] - topNZ * thickness;
                float innerBottomY = rowY[PART_COUNT] - bottomNY * thickness;
                float innerBottomZ = rowZ[PART_COUNT] - bottomNZ * thickness;

                // Cap normals follow the cloth tangent: up at the shoulders,
                // down at the hem, instead of the old hard-coded (0, 1, 0).
                float topLength = Math.max(1.0e-4f,
                        (float) Math.hypot(normalY[0], normalZ[0]));
                float topTY = -normalZ[0] / topLength;
                float topTZ = normalY[0] / topLength;
                float bottomLength = Math.max(1.0e-4f,
                        (float) Math.hypot(normalY[PART_COUNT], normalZ[PART_COUNT]));
                float bottomTY = -normalZ[PART_COUNT] / bottomLength;
                float bottomTZ = normalY[PART_COUNT] / bottomLength;

                sheetVertex(vc, p, light, -HALF_WIDTH + rowX[0], innerTopY, innerTopZ, u0, vTop,
                        0f, topTY, topTZ, 255, 255, 255, 255);
                sheetVertex(vc, p, light, HALF_WIDTH + rowX[0], innerTopY, innerTopZ, u1, vTop,
                        0f, topTY, topTZ, 255, 255, 255, 255);
                sheetVertex(vc, p, light, HALF_WIDTH + rowX[0], rowY[0], rowZ[0], u1, vTop,
                        0f, topTY, topTZ, 255, 255, 255, 255);
                sheetVertex(vc, p, light, -HALF_WIDTH + rowX[0], rowY[0], rowZ[0], u0, vTop,
                        0f, topTY, topTZ, 255, 255, 255, 255);

                sheetVertex(vc, p, light, HALF_WIDTH + rowX[PART_COUNT], rowY[PART_COUNT],
                        rowZ[PART_COUNT], u1, vBottom, 0f, -bottomTY, -bottomTZ, 255, 255, 255, 255);
                sheetVertex(vc, p, light, -HALF_WIDTH + rowX[PART_COUNT], rowY[PART_COUNT],
                        rowZ[PART_COUNT], u0, vBottom, 0f, -bottomTY, -bottomTZ, 255, 255, 255, 255);
                sheetVertex(vc, p, light, -HALF_WIDTH + rowX[PART_COUNT], innerBottomY,
                        innerBottomZ, u0, vBottom, 0f, -bottomTY, -bottomTZ, 255, 255, 255, 255);
                sheetVertex(vc, p, light, HALF_WIDTH + rowX[PART_COUNT], innerBottomY,
                        innerBottomZ, u1, vBottom, 0f, -bottomTY, -bottomTZ, 255, 255, 255, 255);
            });
            pose.popPose();
        }

        /** Advances one WaveyCapes simulation per visible player. */
        private static void tick(Minecraft client) {
            if (client.player == null || client.level == null) {
                simulations.clear();
                simulationModes.clear();
                return;
            }
            Set<UUID> active = new HashSet<>();
            for (Player player : client.level.players()) {
                if (!(player instanceof AbstractClientPlayer clientPlayer)) {
                    continue;
                }
                boolean localPlayer = player == client.player;
                Cape cape = localPlayer ? selectedLocalCape() : selectedRemoteCape(player);
                CapeSettings settings = cape == null
                        ? settingsForCatalog(player, localPlayer)
                        : settingsFor(cape, player.getUUID(), localPlayer);
                if (settings == null) {
                    continue;
                }
                String wanted = settings.movement();
                if (wanted.equals("Classic")) {
                    continue;
                }
                UUID id = player.getUUID();
                active.add(id);
                CapePhysics.Simulation simulation = simulations.get(id);
                if (simulation == null || !wanted.equals(simulationModes.get(id))) {
                    simulation = CapePhysics.create(wanted, PART_COUNT + 1);
                    simulations.put(id, simulation);
                    simulationModes.put(id, wanted);
                    // Settle a freshly created cloth before it is first drawn.
                    applyMovement(simulation, 1f, 1f, 0f);
                    for (int i = 0; i < 5; i++) {
                        simulation.simulate();
                    }
                }
                simulatePlayer(client, clientPlayer, simulation, settings);
            }
            simulations.keySet().retainAll(active);
            simulationModes.keySet().retainAll(active);
        }

        private static void simulatePlayer(Minecraft client, AbstractClientPlayer player,
                                           CapePhysics.Simulation simulation, CapeSettings settings) {
            float x = (float) player.getX();
            float y = (float) player.getY();
            float z = (float) player.getZ();
            float xo = (float) player.xo;
            float yo = (float) player.yo;
            float zo = (float) player.zo;
            float delta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double cloakX = player.avatarState().getInterpolatedCloakX(delta);
            double cloakZ = player.avatarState().getInterpolatedCloakZ(delta);
            float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * delta;
            boolean sneaking = player.isCrouching();

            float heightMultiplier = settings.heightMultiplier();
            float gravity = settings.gravity();
            if (player.isInWater()) {
                heightMultiplier *= 2f;
                gravity /= 10f;
            }
            simulation.gravity = gravity;

            // WaveyCapes CapeHolder.simulate(): the lag between the last cloak
            // render position and the body gives the forward/back impulse, while
            // the previous-tick position drives the fall and strafe impulses.
            double d = cloakX - x;
            double m = cloakZ - z;
            double yawRad = Math.toRadians(bodyYaw);
            float o = (float) Math.sin(yawRad);
            float p = (float) -Math.cos(yawRad);
            float fallHack = (float) Math.max(0f, Math.min(1f, (yo - y) * 10f));
            float crouch = sneaking && !simulation.sneaking ? 1f : 0f;
            float changeX = (float) (d * o + m * p) + fallHack + crouch * 3f;
            float changeY = (y - yo) * heightMultiplier + crouch;
            double strafeYawRad = Math.toRadians(-player.getYRot());
            float strafe = (float) (Math.cos(strafeYawRad) * (x - xo)
                    - Math.sin(strafeYawRad) * (z - zo));
            float changeZ = -strafe * 2f;

            if (settings.computeGravityVector()) {
                // WaveyCapes' optional gravity vector follows the wearer's body
                // pitch instead of always pointing world-down. Simulation x is
                // behind the torso and y is down, so looking down pulls the
                // cloth backwards and level play keeps the default (0,-1,0).
                float pitch = (float) Math.toRadians(player.getXRot());
                simulation.setGravityDirection(-(float) Math.sin(pitch), -(float) Math.cos(pitch), 0f);
            } else {
                float gx = 0f;
                float gy = -1f;
                if (player.isVisuallySwimming()) {
                    double rad = Math.toRadians(player.getXRot() + 90f);
                    float cos = (float) Math.cos(rad);
                    float sin = (float) Math.sin(rad);
                    float ngx = cos * gx - sin * gy;
                    float ngy = sin * gx + cos * gy;
                    gx = ngx;
                    gy = ngy;
                    float ncx = cos * changeX - sin * changeY;
                    float ncy = sin * changeX + cos * changeY;
                    changeX = ncx;
                    changeY = ncy;
                }
                simulation.setGravityDirection(gx, gy, 0f);
            }
            simulation.sneaking = sneaking;
            applyMovement(simulation, changeX, changeY, changeZ);
            simulation.simulate();
        }

        /** Uses the wearer's synced settings when available; local settings are the fallback. */
        private static CapeSettings settingsFor(Cape cape, UUID playerId, boolean localPlayer) {
            if (!localPlayer && playerId != null && CapeSyncClient.hasRemoteSettings(playerId)) {
                return CapeSyncClient.remoteSettings(playerId);
            }
            return CapeSettings.from(cape);
        }

        private static CapePhysics.Simulation simulationFor(UUID playerId, String movement) {
            CapePhysics.Simulation simulation = simulations.get(playerId);
            return simulation != null && movement.equals(simulationModes.get(playerId))
                    ? simulation : null;
        }

        private static void applyMovement(CapePhysics.Simulation simulation, float dx, float dy, float dz) {
            if (simulation instanceof CapePhysics.StickSimulation stick) {
                stick.applyMovement(dx, dy, dz);
            } else if (simulation instanceof CapePhysics.StickSimulation3d threeD) {
                threeD.applyMovement(dx, dy, dz);
            } else if (simulation instanceof CapePhysics.StickSimulationDungeons dungeons) {
                dungeons.applyMovement(dx, dy, dz);
            }
        }

        /** WaveyCapes' getNatrualWindSwing as a +/-3 degree travelling wave. */
        private static float windSwing(float relativePart) {
            long highlighted = (System.currentTimeMillis() / 3L) % 360L;
            return (float) (Math.sin(Math.toRadians(relativePart * 360f - highlighted)) * 3f);
        }

        private static Cape selectedLocalCape() {
            return ModuleManager.get().getByCategory(ModuleCategory.COSMETICS)
                    .stream()
                    .filter(Cape.class::isInstance)
                    .map(Cape.class::cast)
                    .filter(Cape::isEnabled)
                    .findFirst()
                    .orElse(null);
        }

        private static boolean isMoving(AvatarRenderState state) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return false;
            }
            var entity = minecraft.level.getEntity(state.id);
            return entity != null && CapeSyncClient.isEntityMoving(entity);
        }

        private static Cape selectedRemoteCape(Player player) {
            String fileName = CapeSyncClient.remoteCape(player.getUUID());
            if (fileName == null) {
                return null;
            }
            return ModuleManager.get().getByCategory(ModuleCategory.COSMETICS)
                    .stream()
                    .filter(Cape.class::isInstance)
                    .map(Cape.class::cast)
                    .filter(cape -> cape.fileName().equals(fileName))
                    .findFirst()
                    .orElse(null);
        }
    }

    /** Shallow crown sitting on the top of the head. */
    private static final class HatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private HatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            Hat hat = ModuleManager.get().byName("Hat", Hat.class).orElse(null);
            CosmeticLoadout loadout = loadoutOf(state);
            if (hat == null || !loadout.hat()) {
                return;
            }
            HatDesigns.Design design = HatDesigns.byName(loadout.hatDesign());
            if (design != null) {
                pose.pushPose();
                getParentModel().head.translateAndRotate(pose);
                // Clears the skull: the authored hats sat low enough that the
                // brim cut into the head.
                pose.translate(0f, -0.9f / 16f, 0f);
                WingModelRenderer.render(design, getParentModel().head, pose, collector, light,
                        state.ageInTicks / 20f);
                pose.popPose();
                return;
            }
            boolean localPlayer = isLocalPlayer(state);
            float s = localPlayer ? hat.size.getFloat() : 1f;
            int color = loadout.hatColor();

            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            // Head box is y -8..0 px, so the crown straddles the top of the skull.
            collector.submitCustomGeometry(pose, RenderTypes.entitySolid(WHITE), (p, vc) ->
                    box(vc, p, color, light,
                            -4.3f * s / 16f, -10.4f / 16f, -4.3f * s / 16f,
                            4.3f * s / 16f, -7.8f / 16f, 4.3f * s / 16f));
            pose.popPose();
        }
    }

    private static final class AccessoryLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private AccessoryLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            AccessoryDesigns.Design design = AccessoryDesigns.byName(loadoutOf(state).accessory());
            if (design == null) {
                return;
            }

            Identifier texture = design.texture();
            if (AccessoryDesigns.usesMinecraftSkin(design.name())) {
                Identifier playerSkin = MiniMeSkinManager.textureFor(loadoutOf(state).minimeSkinUser());
                if (playerSkin != null) {
                    texture = playerSkin;
                }
            }

            for (String attachmentBone : design.attachmentBones()) {
                ModelPart attachment = switch (attachmentBone) {
                    case "armorHead" -> getParentModel().head;
                    case "armorRightArm" -> getParentModel().rightArm;
                    case "armorLeftArm" -> getParentModel().leftArm;
                    default -> getParentModel().body;
                };
                pose.pushPose();
                attachment.translateAndRotate(pose);
                WingModelRenderer.render(
                        design, attachmentBone, texture, attachment,
                        pose, collector, light, state.ageInTicks / 20f);
                pose.popPose();
            }
        }
    }

    /**
     * Feather fans on the back: four blades per side, swept slightly back and
     * beating up and down on a smoothed sine. The beating is what sells them as
     * wings; without it they read as flat cardboard.
     */
    private static final class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private static float phase;
        private static long lastNanos;

        private static Wings selectedWings() {
            return ModuleManager.get().getByCategory(ModuleCategory.COSMETICS).stream()
                    .filter(Wings.class::isInstance)
                    .map(Wings.class::cast)
                    .filter(Wings::isEnabled)
                    .findFirst()
                    .orElse(null);
        }

        private WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            boolean localPlayer = isLocalPlayer(state);
            CosmeticLoadout loadout = loadoutOf(state);
            String designName = loadout.wings();
            if (designName.isEmpty()) {
                return;
            }
            // Imported meshes are authored around the vanilla body, so they already
            // sit on the back once the coordinates and per-design scale/offset are
            // applied (see WingModelRenderer). They need no local module, which is
            // what lets other players' wings render as well.
            WingDesigns.Design design = WingDesigns.byName(designName);
            if (design != null) {
                float designSpeed = 1f;
                if (localPlayer) {
                    Wings localWings = selectedWings();
                    if (localWings != null) {
                        designSpeed = localWings.speed.getFloat();
                    }
                }
                pose.pushPose();
                getParentModel().body.translateAndRotate(pose);
                // A small armor clearance prevents authored wing roots from
                // intersecting the torso and the 3D skin layer.
                pose.translate(0f, 0f, 0.55f / 16f);
                // Idle sway for every design: the ones without an own animation
                // file used to hang completely still, which read as a decal.
                float idle = state.ageInTicks;
                pose.mulPose(new org.joml.Quaternionf()
                        .rotateZ((float) Math.sin(idle * 0.08f) * 0.03f));
                // Only ever opens away from the back, never through it.
                pose.mulPose(new org.joml.Quaternionf()
                        .rotateX(0.02f + (float) Math.cos(idle * 0.055f) * 0.015f));
                WingModelRenderer.render(design, getParentModel().body, pose, collector, light,
                        state.ageInTicks / 20f * designSpeed);
                pose.popPose();
                return;
            }

            // Procedural fans are drawn from the wearer's own settings, so they are
            // only shown for yourself.
            Wings wings = localPlayer ? selectedWings() : null;
            if (wings == null) {
                return;
            }
            float span = wings.span.getFloat();
            float len = wings.length.getFloat();
            float amp = wings.flap.getFloat();
            float speed = wings.speed.getFloat();
            int color = wings.color.getValue();
            // Bee wings are the short striped variant.
            boolean bee = wings.isBeeStyle();
            float sizeMul = bee ? 0.68f : 1f;
            int blades = bee ? 3 : 5;

            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
            lastNanos = now;
            phase += dt * (1.6f + 4.6f * speed);
            float beat = (float) Math.sin(phase) * amp;

            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            // Root the fans just behind the shoulder blades, and far enough back
            // that the cape never eats them from a rear view.
            pose.translate(0f, 1.4f / 16f, 4.6f / 16f);

            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                // Sweep the fan backwards (negative Y turns +X towards +Z) and
                // tilt it with the beat. No mirroring: a mirrored space would
                // flip the droop direction and leave one wing pointing up, so
                // the blades themselves are built towards +/- X instead.
                pose.mulPose(Axis.YP.rotation(-side * 0.42f));
                pose.mulPose(Axis.ZP.rotation(side * (-0.05f + 0.16f * beat)));

                for (int i = 0; i < blades; i++) {
                    pose.pushPose();
                    // Fan the blades downwards; the outer ones are the longest.
                    pose.mulPose(Axis.ZP.rotation(side * 0.12f * i));
                    final float bladeLen = (5.0f + 2.6f * i) * len * span * sizeMul / 16f;
                    final float halfH = (2.2f - 0.18f * i) / 16f;
                    final float halfZ = (0.9f - 0.12f * i) / 16f;
                    final float root = -0.5f / 16f;
                    final float xa = Math.min(side * root, side * bladeLen);
                    final float xb = Math.max(side * root, side * bladeLen);
                    final int bladeColor = bee && i == blades - 1 ? shade(color, 0.25f) : color;
                    collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WHITE), (p, vc) ->
                            box(vc, p, bladeColor, light, xa, -halfH, -halfZ, xb, halfH, halfZ));
                    pose.popPose();
                }
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /**
     * Glowing ring floating above the head, with a few orbs circling it.
     *
     * <p>The ring is a beaded torus: small cubes placed on a circle and turned
     * with their angle, which keeps the silhouette round without a torus mesh.
     * The orbs orbit slightly faster and bob, so the halo reads as alive.</p>
     */
    private static final class HaloLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private static float spin;
        private static long lastNanos;

        private HaloLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            Halo halo = ModuleManager.get().byName("Halo", Halo.class).orElse(null);
            CosmeticLoadout loadout = loadoutOf(state);
            if (halo == null || !loadout.halo()) {
                return;
            }
            boolean localPlayer = isLocalPlayer(state);
            float size = localPlayer ? halo.size.getFloat() : 1f;
            int color = loadout.haloColor();
            int orbs = localPlayer ? halo.orbs.getInt() : 4;
            float haloSpeed = localPlayer ? halo.speed.getFloat() : 1f;

            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
            lastNanos = now;
            spin += dt * haloSpeed * 1.1f;

            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            // Head space is y -8 .. 0 (skull top at -8), so the ring floats above
            // the hat instead of cutting through it.
            float ringY = -10.6f / 16f;
            float radius = 5.0f * size / 16f;
            int beads = 20;
            float bead = 0.85f * size / 16f;

            for (int i = 0; i < beads; i++) {
                double angle = spin * 0.35 + i * (Math.PI * 2.0 / beads);
                final float bx = (float) Math.cos(angle) * radius;
                final float bz = (float) Math.sin(angle) * radius;
                final float h = bead * 0.55f;
                pose.pushPose();
                pose.translate(bx, ringY, bz);
                pose.mulPose(Axis.YP.rotation((float) -angle));
                collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WHITE), (p, vc) ->
                        box(vc, p, color, light, -bead, -h, -h * 0.5f, bead, h, h * 0.5f));
                pose.popPose();
            }

            for (int i = 0; i < orbs; i++) {
                double angle = spin + i * (Math.PI * 2.0 / Math.max(1, orbs));
                final float ox = (float) Math.cos(angle) * radius * 1.08f;
                final float oz = (float) Math.sin(angle) * radius * 1.08f;
                float bobY = halo.bob.isEnabled()
                        ? 0.9f / 16f * (float) Math.sin(spin * 2.2 + i * 1.4)
                        : 0f;
                final float oy = ringY + bobY;
                final float s = 0.65f / 16f * (0.85f + 0.15f * size);
                collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WHITE), (p, vc) ->
                        box(vc, p, color, light, ox - s, oy - s, oz - s, ox + s, oy + s, oz + s));
            }
            pose.popPose();
        }
    }

    /**
     * Swirling glow around the player.
     *
     * <p>Built from vertical strips arranged on a circle; each strip fades out
     * towards its top edge, which makes the band read as light instead of a
     * cylinder wall. The strips rotate with the configured speed.</p>
     */
    private static final class AuraLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private static float spin;
        private static long lastNanos;

        private AuraLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            CosmeticLoadout loadout = loadoutOf(state);
            if (!loadout.aura()) {
                return;
            }
            boolean localPlayer = isLocalPlayer(state);
            Aura aura = localPlayer ? ModuleManager.get().getModules().stream()
                    .filter(Aura.class::isInstance)
                    .map(Aura.class::cast)
                    .filter(Aura::isEnabled)
                    .findFirst()
                    .orElse(null) : null;
            float r = aura == null ? 1f : aura.width.getFloat();
            float h = aura == null ? 1f : aura.height.getFloat();
            int color = loadout.auraColor();
            String shape = loadout.auraShape();
            float auraSpeed = aura == null ? 1f : aura.speed.getFloat();

            AuraDesigns.Design design = AuraDesigns.byName(shape);
            if (design != null) {
                if (design.particleEffect()) {
                    renderEnchantAura(pose, collector, design, light,
                            state.ageInTicks / 20f * auraSpeed, r, h);
                    return;
                }
                pose.pushPose();
                getParentModel().body.translateAndRotate(pose);
                pose.scale(r * design.scale(), h * design.scale(), r * design.scale());
                WingModelRenderer.render(shape, design.geometryResource(), design.animationResource(),
                        design.texture(), "armorBody", 24.016f, -1f, 1f, true,
                        getParentModel().body, pose, collector, light,
                        state.ageInTicks / 20f * auraSpeed);
                pose.popPose();
                return;
            }

            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
            lastNanos = now;
            spin += dt * auraSpeed * 0.9f;

            // Entity space: y grows downwards, the feet sit at 24 px = 1.5 units.
            float footY = 1.5f;
            float radius = 0.55f * r;
            int strips = 28;

            if (shape.equals("Halo")) {
                auraRing(pose, collector, color, light, radius * 1.25f, footY - 0.03f, footY,
                        strips, spin, 0.85f);
            } else if (shape.equals("Column")) {
                auraRing(pose, collector, color, light, radius * 0.85f, footY - 1.45f * h, footY,
                        strips, spin, 0.45f);
            } else {
                auraRing(pose, collector, color, light, radius, footY - 0.42f * h, footY,
                        strips, spin, 1f);
            }
        }

        private static void renderEnchantAura(PoseStack pose, SubmitNodeCollector collector,
                                              AuraDesigns.Design design, int light, float time,
                                              float width, float height) {
            List<AuraSprite> sprites = new java.util.ArrayList<>(12);
            int frameOffset = (int) (time * 6f);
            for (int i = 0; i < 12; i++) {
                float angle = time * (0.8f + (i % 3) * 0.12f) + i * (float) (Math.PI * 2.0 / 12.0);
                float radius = 0.32f + (i % 4) * 0.12f;
                float y = -0.8f + (i % 6) * 0.42f + (float) Math.sin(time * 1.8f + i) * 0.08f;
                sprites.add(new AuraSprite(
                        (float) Math.cos(angle) * radius * width,
                        y * height,
                        (float) Math.sin(angle) * radius * width,
                        0.075f,
                        Math.floorMod(i * 5 + frameOffset, 26)));
            }

            pose.pushPose();
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(design.texture()),
                    (p, vertices) -> {
                        for (AuraSprite sprite : sprites) {
                            emitAuraSprite(vertices, p, sprite, light);
                        }
                    });
            pose.popPose();
        }

        private static void emitAuraSprite(VertexConsumer vertices, PoseStack.Pose pose,
                                           AuraSprite sprite, int light) {
            float half = sprite.size() * 0.5f;
            float radius = Math.max(1.0e-5f, (float) Math.sqrt(sprite.x() * sprite.x()
                    + sprite.z() * sprite.z()));
            float tangentX = -sprite.z() / radius;
            float tangentZ = sprite.x() / radius;
            float x0 = sprite.x() - tangentX * half;
            float z0 = sprite.z() - tangentZ * half;
            float x1 = sprite.x() + tangentX * half;
            float z1 = sprite.z() + tangentZ * half;
            float u0 = sprite.frame() / 26f;
            float u1 = (sprite.frame() + 1) / 26f;
            int alpha = 255;
            float normalX = sprite.x() / radius;
            float normalZ = sprite.z() / radius;
            auraSpriteVertex(vertices, pose, light, x0, sprite.y() + half, z0, u0, 0f, normalX, normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x1, sprite.y() + half, z1, u1, 0f, normalX, normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x1, sprite.y() - half, z1, u1, 1f, normalX, normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x0, sprite.y() - half, z0, u0, 1f, normalX, normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x0, sprite.y() - half, z0, u0, 1f, -normalX, -normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x1, sprite.y() - half, z1, u1, 1f, -normalX, -normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x1, sprite.y() + half, z1, u1, 0f, -normalX, -normalZ, alpha);
            auraSpriteVertex(vertices, pose, light, x0, sprite.y() + half, z0, u0, 0f, -normalX, -normalZ, alpha);
        }

        private static void auraSpriteVertex(VertexConsumer vertices, PoseStack.Pose pose, int light,
                                             float x, float y, float z, float u, float v,
                                             float normalX, float normalZ, int alpha) {
            vertices.addVertex(pose, x, y, z)
                    .setColor(255, 255, 255, alpha)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, normalX, 0f, normalZ);
        }

        private record AuraSprite(float x, float y, float z, float size, int frame) {
        }
    }

    /** One band of light: {@code strips} quads on a circle, fading towards the top. */
    private static void auraRing(PoseStack pose, SubmitNodeCollector collector, int argb, int light,
                                 float radius, float yTop, float yBottom, int strips, float spin,
                                 float strength) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float a = (((argb >>> 24) & 0xFF) / 255f) * strength;
        int base = Math.round(a * 135f);
        int top = Math.round(a * 12f);

        pose.pushPose();
        for (int i = 0; i < strips; i++) {
            double a0 = spin + i * (Math.PI * 2.0 / strips);
            double a1 = spin + (i + 1) * (Math.PI * 2.0 / strips);
            final float x0 = (float) Math.cos(a0) * radius;
            final float z0 = (float) Math.sin(a0) * radius;
            final float x1 = (float) Math.cos(a1) * radius;
            final float z1 = (float) Math.sin(a1) * radius;
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WHITE), (p, vc) ->
                    strip(vc, p, r, g, b, base, top, light, x0, yTop, z0, x1, yBottom, z1));
        }
        pose.popPose();
    }

    /** Camera-facing-ish vertical strip with a bottom-to-top alpha fade, both windings. */
    private static void strip(VertexConsumer vc, PoseStack.Pose pose, int r, int g, int b,
                              int bottomAlpha, int topAlpha, int light,
                              float x0, float yTop, float z0, float x1, float yBottom, float z1) {
        float nx = x0 + x1;
        float nz = z0 + z1;
        float len = Math.max(1.0e-5f, (float) Math.sqrt(nx * nx + nz * nz));
        nx /= len;
        nz /= len;
        vertex(vc, pose, r, g, b, topAlpha, light, x0, yTop, z0, nx, 0f, nz);
        vertex(vc, pose, r, g, b, topAlpha, light, x1, yTop, z1, nx, 0f, nz);
        vertex(vc, pose, r, g, b, bottomAlpha, light, x1, yBottom, z1, nx, 0f, nz);
        vertex(vc, pose, r, g, b, bottomAlpha, light, x0, yBottom, z0, nx, 0f, nz);
        // Back side.
        vertex(vc, pose, r, g, b, bottomAlpha, light, x0, yBottom, z0, -nx, 0f, -nz);
        vertex(vc, pose, r, g, b, bottomAlpha, light, x1, yBottom, z1, -nx, 0f, -nz);
        vertex(vc, pose, r, g, b, topAlpha, light, x1, yTop, z1, -nx, 0f, -nz);
        vertex(vc, pose, r, g, b, topAlpha, light, x0, yTop, z0, -nx, 0f, -nz);
    }

    /** Axis-aligned box in local units, every face emitted for both windings. */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, int argb, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        quad(vc, pose, argb, light, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0f, 0f, -1f);
        quad(vc, pose, argb, light, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0f, 0f, 1f);
        quad(vc, pose, argb, light, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1f, 0f, 0f);
        quad(vc, pose, argb, light, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1f, 0f, 0f);
        quad(vc, pose, argb, light, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f);
        quad(vc, pose, argb, light, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f);
    }

    /**
     * One cloth vertex: explicit texture coordinates, colour and a normal taken
     * from the row seam so neighbouring rows shade continuously. Entity render
     * types are no-cull, so one winding per face is enough.
     */
    private static void sheetVertex(VertexConsumer vc, PoseStack.Pose pose, int light,
                                    float x, float y, float z, float u, float v,
                                    float ny, float nz, int r, int g, int b, int a) {
        sheetVertex(vc, pose, light, x, y, z, u, v, 0f, ny, nz, r, g, b, a);
    }

    /** Cloth vertex with a full normal, used by the slab's side and end caps. */
    private static void sheetVertex(VertexConsumer vc, PoseStack.Pose pose, int light,
                                    float x, float y, float z, float u, float v,
                                    float nx, float ny, float nz, int r, int g, int b, int a) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /** Multiplies rgb by {@code factor}, keeping alpha. */
    private static int shade(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = clamp255(Math.round(((argb >> 16) & 0xFF) * factor));
        int g = clamp255(Math.round(((argb >> 8) & 0xFF) * factor));
        int b = clamp255(Math.round((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static final class CatalogLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private CatalogLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                           float limbSwing, float limbSwingAmount) {
            CosmeticLoadout loadout = loadoutOf(state);
            for (CosmeticCatalog.Entry entry : CosmeticCatalog.fromSelection(loadout.catalogSelection())) {
                CatalogCosmeticRenderer.render(entry, getParentModel(), pose, collector, light, state);
            }
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int argb, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int a = (argb >>> 24) & 0xFF;
        vertex(vc, pose, r, g, b, a, light, ax, ay, az, nx, ny, nz);
        vertex(vc, pose, r, g, b, a, light, bx, by, bz, nx, ny, nz);
        vertex(vc, pose, r, g, b, a, light, cx, cy, cz, nx, ny, nz);
        vertex(vc, pose, r, g, b, a, light, dx, dy, dz, nx, ny, nz);
        // Back side, so the thin cape is visible from both directions.
        vertex(vc, pose, r, g, b, a, light, dx, dy, dz, -nx, -ny, -nz);
        vertex(vc, pose, r, g, b, a, light, cx, cy, cz, -nx, -ny, -nz);
        vertex(vc, pose, r, g, b, a, light, bx, by, bz, -nx, -ny, -nz);
        vertex(vc, pose, r, g, b, a, light, ax, ay, az, -nx, -ny, -nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int r, int g, int b, int a, int light,
                               float x, float y, float z, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
