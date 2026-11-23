package dev.octoclient.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.octoclient.module.cosmetics.WingDesigns;
import dev.octoclient.module.cosmetics.AccessoryDesigns;
import dev.octoclient.module.cosmetics.HatDesigns;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders the bundled Bedrock geometry cosmetics (bandanas, wings, auras, and
 * accessories).
 *
 * <p>The geometry files are authored in Bedrock/Blockbench space: +Y up, the
 * player's feet at y=0 and the neck at y=24, with -Z towards the player's face.
 * The vanilla player model we attach to uses +Y down, y=0 at the neck and +Z
 * behind the player, so every vertex is remapped to player-local coordinates
 * and rebased against the attached model part before it is drawn. This keeps
 * head and arm cosmetics from being translated twice by the vanilla part pose.</p>
 *
 * <p>Two conventions are easy to get wrong and both were:</p>
 * <ul>
 *   <li>Bedrock Euler rotations are applied X, then Y, then Z, which is JOML's
 *       {@code rotateZYX}. Using {@code rotateXYZ} rotates the three-axis
 *       wing roots into the chest instead of onto the back.</li>
 *   <li>Bedrock texture V grows downward from the top of each face rectangle,
 *       while the side quads here are built bottom-first. The V components are
 *       therefore swapped per face (the horizontal "up" face is the one face
 *       that already matched).</li>
 * </ul>
 *
 * <p>Textures may be vertical animation strips with a {@code .mcmeta}
 * ({@code animation.frametime}, width/height). Only the current frame is
 * sampled, by squeezing the V range into that frame's slice of the image.</p>
 */
final class WingModelRenderer {
    private static final Map<String, Model> MODELS = new HashMap<>();
    private static final Map<Identifier, TextureAnimation> TEXTURE_ANIMATIONS = new HashMap<>();
    private static final Set<String> WARNED_ANIMATION_EXPRESSIONS = new java.util.HashSet<>();
    private static final TextureAnimation STILL = new TextureAnimation(1, 1f, 50L);

    private WingModelRenderer() {
    }

    static void render(WingDesigns.Design design, ModelPart attachment, PoseStack pose,
                       SubmitNodeCollector collector, int light, float animationTime) {
        render(design.texturePath(), design.geometryResource(), design.animationResource(),
                design.texture(), "armorBody", 24.016f, -1f, 1f, true,
                design.scale(), design.offsetX(), design.offsetY(), design.offsetZ(), null,
                attachment, pose, collector, light, animationTime);
    }

    static void render(AccessoryDesigns.Design design, String attachmentBone, ModelPart attachment,
                       PoseStack pose, SubmitNodeCollector collector, int light, float animationTime) {
        render(design, attachmentBone, design.texture(), attachment, pose, collector, light, animationTime);
    }

    static void render(AccessoryDesigns.Design design, String attachmentBone, Identifier texture,
                       ModelPart attachment,
                       PoseStack pose, SubmitNodeCollector collector, int light, float animationTime) {
        render(design.folder() + "#" + attachmentBone,
                design.geometryResource(), design.animationResource(), texture,
                attachmentBone, 24.016f, -1f, 1f, false,
                design.scale(), 0f, 0f, 0f, design.reversedSwordBone(),
                attachment, pose, collector, light, animationTime);
    }

    static void render(HatDesigns.Design design, ModelPart attachment,
                       PoseStack pose, SubmitNodeCollector collector,
                       int light, float animationTime) {
        render(design.modelPath(), design.geometryResource(), design.animationResource(), design.texture(),
                "armorHead", 24.016f, -1f, 1f, false, design.scale(),
                design.offsetX(), design.offsetY(), design.offsetZ(), null, attachment,
                pose, collector, light, animationTime);
    }

    static void render(String key, String geometryResource, String animationResource, Identifier texture,
                       String bodyBone, float yOrigin, float yDirection, float zDirection, boolean emissive,
                       ModelPart attachment, PoseStack pose, SubmitNodeCollector collector,
                       int light, float animationTime) {
        render(key, geometryResource, animationResource, texture, bodyBone, yOrigin, yDirection, zDirection,
                emissive, 1f, 0f, 0f, 0f, null, attachment, pose, collector, light, animationTime);
    }

    private static void render(String key, String geometryResource, String animationResource, Identifier texture,
                               String bodyBone, float yOrigin, float yDirection, float zDirection,
                               boolean emissive, float scale, float offsetX, float offsetY, float offsetZ,
                               String reversedSwordBone, ModelPart attachment,
                               PoseStack pose, SubmitNodeCollector collector, int light, float animationTime) {
        Model model = MODELS.computeIfAbsent(key,
                ignored -> load(key, geometryResource, animationResource, bodyBone));
        Map<String, Bone> byName = new HashMap<>();
        model.bones().forEach(bone -> byName.put(bone.name(), bone));
        Map<String, Matrix4f> transforms = new HashMap<>();
        List<Quad> quads = new ArrayList<>();
        for (Bone bone : model.bones()) {
            if (!isWingBone(bone, byName, bodyBone)) {
                continue;
            }
            Matrix4f transform = transformFor(
                    bone, byName, transforms, model.animations(), animationTime, reversedSwordBone);
            for (Plane plane : bone.planes()) {
                Matrix4f planeTransform = new Matrix4f(transform)
                        .translate(plane.pivotX(), plane.pivotY(), plane.pivotZ())
                        .rotateZYX(plane.rotationZ(), plane.rotationY(), plane.rotationX())
                        .translate(-plane.pivotX(), -plane.pivotY(), -plane.pivotZ());
                quads.add(createQuad(plane, planeTransform, yOrigin, yDirection, zDirection,
                        attachment == null ? 0f : attachment.x,
                        attachment == null ? 0f : attachment.y,
                        attachment == null ? 0f : attachment.z));
            }
        }
        // Cutout (depth-writing, alpha-tested) instead of translucent: the
        // cosmetic models stack several flat layers on top of each other, and a
        // translucent pass never writes depth. The result was that whichever
        // pane happened to be submitted last painted over the nearer ones - the
        // back of a bandana showed the front band's artwork, and layered wings
        // came out as a muddy, see-through smear.
        // No-cull cutout: the imported Bedrock sheets are open (single-faced
        // panes, one-sided cards), so with back-face culling whole parts vanished
        // as soon as the player was seen from the other side or from the side.
        // Cutout keeps the alpha test and depth writes that stop the layers
        // painting over each other.
        var renderType = RenderTypes.entityCutout(texture);
        TextureAnimation animation = TEXTURE_ANIMATIONS.computeIfAbsent(texture,
                WingModelRenderer::readTextureAnimation);
        int frame = animation.frame();
        float vOffset = frame * animation.vSlice();
        float vScale = animation.vSlice();
        pose.pushPose();
        applyCosmeticScale(pose, byName.get(bodyBone), scale);
        if (offsetX != 0f || offsetY != 0f || offsetZ != 0f) {
            // NoRisk applies the cosmetic offset in pixels after the scale, in
            // Bedrock space, so mirror X/Y and multiply by the same scale.
            // Every design also drops a little: the authored roots sat at the
            // shoulder blades, which reads too high on the vanilla body.
            pose.translate(-offsetX * scale / 16f, -offsetY * scale / 16f + 1.4f / 16f,
                    offsetZ * scale / 16f);
        }
        collector.submitCustomGeometry(pose, renderType, (p, vertices) -> {
            for (Quad quad : quads) {
                emit(vertices, p, light, quad, vOffset, vScale);
            }
        });
        pose.popPose();
    }

    /**
     * NoRisk multiplies the cosmetic scale onto the body-part bone, which
     * scales the model around that bone's pivot. The pivot is authored in
     * Bedrock space, so it is remapped the same way as the vertices.
     */
    private static void applyCosmeticScale(PoseStack pose, Bone body, float scale) {
        if (scale == 1f) {
            return;
        }
        if (body == null) {
            pose.scale(scale, scale, scale);
            return;
        }
        float pivotX = -body.pivotX() / 16f;
        float pivotY = (24.016f - body.pivotY()) / 16f;
        float pivotZ = body.pivotZ() / 16f;
        pose.translate(pivotX, pivotY, pivotZ);
        pose.scale(scale, scale, scale);
        pose.translate(-pivotX, -pivotY, -pivotZ);
    }

    private static Model load(String name, String geometryResource, String animationResource, String bodyBone) {
        try (InputStream stream = WingModelRenderer.class.getResourceAsStream(geometryResource)) {
            if (stream == null) {
                throw new IOException("Missing cosmetic model " + geometryResource);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            float textureWidth = description.get("texture_width").getAsFloat();
            float textureHeight = description.get("texture_height").getAsFloat();
            List<Bone> bones = readBones(geometry.getAsJsonArray("bones"), textureWidth, textureHeight);
            Map<String, Bone> byName = new HashMap<>();
            bones.forEach(bone -> byName.put(bone.name(), bone));
            if (bones.stream().noneMatch(bone -> !bone.planes().isEmpty()
                    && isWingBone(bone, byName, bodyBone))) {
                throw new IOException("Cosmetic model has no renderable geometry: " + geometryResource);
            }
            return new Model(bones, readAnimations(animationResource));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to load cosmetic model " + name, exception);
        }
    }

    private static List<Bone> readBones(JsonArray array, float textureWidth, float textureHeight) {
        List<Bone> bones = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String name = json.get("name").getAsString();
            String parent = json.has("parent") ? json.get("parent").getAsString() : null;
            float[] pivot = vector(json, "pivot", new float[]{0f, 0f, 0f});
            float[] rotation = vector(json, "rotation", new float[]{0f, 0f, 0f});
            List<Plane> planes = new ArrayList<>();
            if (json.has("cubes")) {
                for (JsonElement cubeElement : json.getAsJsonArray("cubes")) {
                    JsonObject cube = cubeElement.getAsJsonObject();
                    float[] rawSize = vector(cube, "size", new float[]{0f, 0f, 0f});
                    float[] rawOrigin = vector(cube, "origin", new float[]{0f, 0f, 0f});
                    float[] size = rawSize;
                    float[] origin = rawOrigin;
                    int zeroAxis = zeroAxis(size);
                    // "inflate" grows a cube by the given amount on all six sides.
                    // Negative values shrink it, which the dragon/neon designs use
                    // to layer their detail panes.
                    float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0f;
                    if (inflate != 0f) {
                        origin = new float[]{
                                origin[0] - inflate, origin[1] - inflate, origin[2] - inflate};
                        size = new float[]{
                                size[0] + (zeroAxis == 1 ? 0f : 2f * inflate),
                                size[1] + (zeroAxis == 2 ? 0f : 2f * inflate),
                                size[2] + (zeroAxis == 3 ? 0f : 2f * inflate)};
                    }
                    // NoRisk rotates a cube around the centre of its box when the
                    // model does not author an explicit pivot (createCube's
                    // fallback), not around the minimum corner.
                    float[] cubePivot = vector(cube, "pivot", new float[]{
                            rawOrigin[0] + rawSize[0] / 2f,
                            rawOrigin[1] + rawSize[1] / 2f,
                            rawOrigin[2] + rawSize[2] / 2f});
                    float[] cubeRotation = vector(cube, "rotation", new float[]{0f, 0f, 0f});
                    JsonElement uvElement = cube.get("uv");
                    if (uvElement == null || !uvElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject uvFaces = uvElement.getAsJsonObject();
                    String[] faceNames = zeroAxis == 0 ? new String[]{
                            "north", "south", "east", "west", "up", "down"
                    } : paneFace(uvFaces, zeroAxis);
                    for (String faceName : faceNames) {
                        JsonObject face = uvFaces.getAsJsonObject(faceName);
                        if (face == null || !face.has("uv") || !face.has("uv_size")) {
                            continue;
                        }
                        float[] uv = vector(face, "uv", new float[]{0f, 0f});
                        float[] uvSize = vector(face, "uv_size", new float[]{0f, 0f});
                        planes.add(new Plane(origin, size, cubePivot, cubeRotation,
                                uv[0] / textureWidth, uv[1] / textureHeight,
                                uvSize[0] / textureWidth, uvSize[1] / textureHeight, faceName));
                    }
                }
            }
            bones.add(new Bone(name, parent, pivot, rotation, List.copyOf(planes)));
        }
        return bones;
    }

    private static int zeroAxis(float[] size) {
        for (int i = 0; i < size.length; i++) {
            if (Math.abs(size[i]) < 1.0e-4f) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * Face names for a zero-thickness pane. Bedrock wings are flat sheets that
     * author both sides with separate (usually mirrored) UVs, and NoRisk draws
     * every authored quad. Using only one side left the "holes" in the near
     * artwork showing straight through the wing, which is what made imported
     * wings look transparent. Both faces are emitted here; the cutout render
     * type culls the one that points away from the camera, so the two can share
     * a plane without z-fighting.
     */
    private static String[] paneFace(JsonObject uvFaces, int zeroAxis) {
        String first;
        String second;
        switch (zeroAxis) {
            case 1 -> { first = "east"; second = "west"; }
            case 2 -> { first = "up"; second = "down"; }
            default -> { first = "north"; second = "south"; }
        }
        boolean hasFirst = uvFaces.has(first);
        boolean hasSecond = uvFaces.has(second);
        if (hasFirst && hasSecond) {
            return new String[]{first, second};
        }
        return new String[]{hasFirst ? first : second};
    }

    private static boolean isWingBone(Bone bone, Map<String, Bone> byName, String bodyBone) {
        if ("armorBody".equalsIgnoreCase(bodyBone)) {
            String name = bone.name().toLowerCase(java.util.Locale.ROOT);
            // Some imported wing files contain a stray head/helmet helper bone.
            // Rendering it from the body anchor puts one wing through the head.
            if (name.contains("head") || name.contains("helmet") || name.contains("hat")) {
                return false;
            }
        }
        Bone current = bone;
        while (current != null) {
            if (current.name().equalsIgnoreCase(bodyBone)) {
                return true;
            }
            current = current.parent() == null ? null : byName.get(current.parent());
        }
        return false;
    }

    private static Matrix4f transformFor(Bone bone, Map<String, Bone> byName,
                                         Map<String, Matrix4f> cache,
                                         Map<String, BoneAnimation> animations, float animationTime,
                                         String reversedSwordBone) {
        Matrix4f cached = cache.get(bone.name());
        if (cached != null) {
            return cached;
        }
        Matrix4f parent = bone.parent() == null || !byName.containsKey(bone.parent())
                ? new Matrix4f()
                : new Matrix4f(transformFor(byName.get(bone.parent()), byName, cache, animations,
                        animationTime, reversedSwordBone));
        BoneAnimation animation = animations.get(bone.name().toLowerCase(java.util.Locale.ROOT));
        float[] animatedRotation = animation == null || animation.rotation() == null
                ? new float[3] : animation.rotation().at(animationTime);
        float[] animatedPosition = animation == null || animation.position() == null
                ? new float[3] : animation.position().at(animationTime);
        float[] animatedScale = animation == null || animation.scale() == null
                ? new float[]{1f, 1f, 1f} : animation.scale().at(animationTime);
        float rotationX = bone.rotationX() + (float) Math.toRadians(animatedRotation[0]);
        if (bone.name().equalsIgnoreCase(reversedSwordBone)) {
            rotationX = -rotationX;
        }
        Matrix4f transform = parent
                .translate(animatedPosition[0], animatedPosition[1], animatedPosition[2])
                .translate(bone.pivotX(), bone.pivotY(), bone.pivotZ())
                // The animated deltas are added as authored. Flipping them made
                // the flapping sweep forward through the torso instead of
                // behind the back (black onyx and friends ended up several
                // pixels in front of the chest). Z is additive either way.
                .rotateZYX(bone.rotationZ() + (float) Math.toRadians(animatedRotation[2]),
                        bone.rotationY() + (float) Math.toRadians(animatedRotation[1]),
                        rotationX)
                .scale(animatedScale[0], animatedScale[1], animatedScale[2])
                .translate(-bone.pivotX(), -bone.pivotY(), -bone.pivotZ());
        cache.put(bone.name(), transform);
        return transform;
    }

    private static Map<String, BoneAnimation> readAnimations(String animationResource) throws IOException {
        try (InputStream stream = WingModelRenderer.class.getResourceAsStream(animationResource)) {
            if (stream == null) {
                return Map.of();
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            JsonObject idle = animations.has("nrc.idle")
                    ? animations.getAsJsonObject("nrc.idle")
                    : animations.entrySet().stream()
                            .map(entry -> entry.getValue().getAsJsonObject())
                            .filter(animation -> animation.has("bones"))
                            .findFirst()
                            .orElse(null);
            if (idle == null || !idle.has("bones")) {
                return Map.of();
            }
            float animationLength = idle.has("animation_length")
                    ? idle.get("animation_length").getAsFloat() : 0f;
            Map<String, BoneAnimation> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : idle.getAsJsonObject("bones").entrySet()) {
                JsonObject tracks = entry.getValue().getAsJsonObject();
                result.put(entry.getKey().toLowerCase(java.util.Locale.ROOT), new BoneAnimation(
                        tracks.has("rotation") ? vectorTrack(tracks.get("rotation"), animationLength, 0f) : null,
                        tracks.has("position") ? vectorTrack(tracks.get("position"), animationLength, 0f) : null,
                        tracks.has("scale") ? vectorTrack(tracks.get("scale"), animationLength, 1f) : null));
            }
            return Map.copyOf(result);
        }
    }

    private static VectorTrack vectorTrack(JsonElement element, float animationLength, float defaultValue) {
        JsonElement vector = element;
        if (element.isJsonObject() && element.getAsJsonObject().has("vector")) {
            vector = element.getAsJsonObject().get("vector");
        }
        if (vector.isJsonArray()) {
            JsonArray components = vector.getAsJsonArray();
            ComponentTrack[] tracks = new ComponentTrack[3];
            for (int i = 0; i < tracks.length; i++) {
                tracks[i] = componentTrack(i < components.size() ? components.get(i) : null, defaultValue);
            }
            return new VectorTrack(tracks, animationLength);
        }
        if (vector.isJsonObject()) {
            JsonObject keyframes = vector.getAsJsonObject();
            ComponentTrack[] tracks = new ComponentTrack[3];
            for (int component = 0; component < 3; component++) {
                List<Keyframe> frames = new ArrayList<>();
                for (Map.Entry<String, JsonElement> frame : keyframes.entrySet()) {
                    JsonElement value = frame.getValue();
                    String lerpMode = "linear";
                    if (value.isJsonObject()) {
                        JsonObject frameObject = value.getAsJsonObject();
                        if (frameObject.has("lerp_mode")) {
                            lerpMode = frameObject.get("lerp_mode").getAsString();
                        }
                        value = frameObject.has("post") ? frameObject.get("post") : frameObject.get("pre");
                    }
                    if (value != null && value.isJsonArray() && value.getAsJsonArray().size() > component) {
                        frames.add(new Keyframe(Float.parseFloat(frame.getKey()),
                                componentTrack(value.getAsJsonArray().get(component), defaultValue), lerpMode));
                    }
                }
                frames.sort(java.util.Comparator.comparingDouble(Keyframe::time));
                tracks[component] = new ComponentTrack(defaultValue, null, List.copyOf(frames));
            }
            return new VectorTrack(tracks, animationLength);
        }
        return new VectorTrack(new ComponentTrack[]{new ComponentTrack(defaultValue, null, List.of()),
                new ComponentTrack(defaultValue, null, List.of()),
                new ComponentTrack(defaultValue, null, List.of())}, animationLength);
    }

    private static ComponentTrack componentTrack(JsonElement value, float defaultValue) {
        if (value == null || value.isJsonNull()) {
            return new ComponentTrack(defaultValue, null, List.of());
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new ComponentTrack(value.getAsFloat(), null, List.of());
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return new ComponentTrack(0f, value.getAsString(), List.of());
        }
        return new ComponentTrack(defaultValue, null, List.of());
    }

    private static float evaluate(String expression, float time) {
        try {
            return new ExpressionParser(expression, time).parse();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unsupported wing animation expression: " + expression, exception);
        }
    }

    private static Quad createQuad(Plane plane, Matrix4f transform,
                                   float yOrigin, float yDirection, float zDirection,
                                   float anchorX, float anchorY, float anchorZ) {
        float x = plane.origin()[0];
        float y = plane.origin()[1];
        float z = plane.origin()[2];
        float sx = plane.size()[0];
        float sy = plane.size()[1];
        float sz = plane.size()[2];
        // A sliver of separation keeps a zero-thickness pane from z-fighting
        // with a full cube that shares its plane (some models place a detail
        // sheet exactly on the body of a thicker slab).
        float nudge = zeroAxis(plane.size()) == 0 ? 0f : 0.15f;
        float ox = 0f;
        float oy = 0f;
        float oz = 0f;
        switch (plane.face()) {
            case "east" -> ox = nudge;
            case "west" -> ox = -nudge;
            case "up" -> oy = nudge;
            case "down" -> oy = -nudge;
            case "south" -> oz = nudge;
            default -> oz = -nudge;
        }
        float minX = x + ox;
        float maxX = x + sx + ox;
        float minY = y + oy;
        float maxY = y + sy + oy;
        float minZ = z + oz;
        float maxZ = z + sz + oz;
        Vector3f[] positions = switch (plane.face()) {
            case "east" -> new Vector3f[]{
                    point(transform, maxX, maxY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, minY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, minY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, maxY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ)
            };
            case "west" -> new Vector3f[]{
                    point(transform, minX, maxY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, minY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, minY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, maxY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ)
            };
            case "up" -> new Vector3f[]{
                    point(transform, minX, maxY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, maxY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, maxY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, maxY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ)
            };
            case "down" -> new Vector3f[]{
                    point(transform, minX, minY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, minY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, minY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, minY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ)
            };
            case "south" -> new Vector3f[]{
                    point(transform, maxX, maxY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, minY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, minY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, maxY, maxZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ)
            };
            default -> new Vector3f[]{
                    point(transform, minX, maxY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, minX, minY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, minY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ),
                    point(transform, maxX, maxY, minZ, yOrigin, yDirection, zDirection, anchorX, anchorY, anchorZ)
            };
        };
        float u0 = plane.u();
        float v0 = plane.v();
        float u1 = u0 + plane.uSize();
        float v1 = v0 + plane.vSize();
        // East/west face vertex order runs opposite to the texture's horizontal
        // direction when viewed from outside, unlike north/south and top/bottom.
        float[] us = plane.face().equals("east") || plane.face().equals("west")
                ? new float[]{u1, u1, u0, u0}
                : new float[]{u0, u0, u1, u1};
        // The horizontal "up" face is the only one whose V already runs the right
        // way once Y is flipped; every other face is built bottom edge first.
        float[] vs = plane.face().equals("up")
                ? new float[]{v1, v0, v0, v1}
                : new float[]{v0, v1, v1, v0};
        Vector3f a = positions[0];
        Vector3f b = positions[1];
        Vector3f c = positions[2];
        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
        Vector3f expectedNormal = faceNormal(plane.face());
        transform.transformDirection(expectedNormal);
        expectedNormal.set(-expectedNormal.x, expectedNormal.y * yDirection,
                expectedNormal.z * zDirection).normalize();
        if (normal.dot(expectedNormal) < 0f) {
            swap(positions, 1, 3);
            swap(us, 1, 3);
            swap(vs, 1, 3);
            normal.negate();
        }
        return new Quad(positions, us, vs, normal);
    }

    private static void swap(Vector3f[] values, int first, int second) {
        Vector3f value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private static void swap(float[] values, int first, int second) {
        float value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private static Vector3f point(Matrix4f transform, float x, float y, float z,
                                  float yOrigin, float yDirection, float zDirection,
                                  float anchorX, float anchorY, float anchorZ) {
        Vector3f point = transform.transformPosition(x, y, z, new Vector3f());
        // NoRisk's root transform is translate(0, 1.501, 0) followed by
        // scale(-1, -1, 1): Y is flipped because Bedrock counts up from the
        // feet while the vanilla model counts down from the neck, and X is
        // flipped because Bedrock puts the player's right at +X while Java
        // puts it at -X. Dropping the X negation mirrored every asymmetric
        // design (butterfly, flower and demon wings).
        // Geometry is authored in whole-player coordinates. The vanilla part
        // pose already translates to its pivot, so rebase vertices before they
        // inherit that pose (most importantly for arm-mounted accessories).
        return new Vector3f(
                (-point.x - anchorX) / 16f,
                ((point.y - yOrigin) * yDirection - anchorY) / 16f,
                (point.z * zDirection - anchorZ) / 16f);
    }

    private static void emit(VertexConsumer vertices, PoseStack.Pose pose, int light, Quad quad,
                             float vOffset, float vScale) {
        for (int i = 0; i < 4; i++) {
            vertex(vertices, pose, light, quad, i, quad.normal(), vOffset, vScale);
        }
    }

    private static Vector3f faceNormal(String face) {
        return switch (face) {
            case "east" -> new Vector3f(1f, 0f, 0f);
            case "west" -> new Vector3f(-1f, 0f, 0f);
            case "up" -> new Vector3f(0f, 1f, 0f);
            case "down" -> new Vector3f(0f, -1f, 0f);
            case "south" -> new Vector3f(0f, 0f, 1f);
            default -> new Vector3f(0f, 0f, -1f);
        };
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, int light,
                               Quad quad, int index, Vector3f normal,
                               float vOffset, float vScale) {
        Vector3f point = quad.positions()[index];
        vertices.addVertex(pose, point.x, point.y, point.z)
                .setColor(255, 255, 255, 255)
                .setUv(quad.us()[index], vOffset + quad.vs()[index] * vScale)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    // ------------------------------------------------------------------
    // Texture animation strips
    // ------------------------------------------------------------------

    /** One animation frame of a vertical strip texture, plus where it sits in V. */
    private record TextureAnimation(int frames, float vSlice, long frameMillis) {
        private int frame() {
            if (frames <= 1) {
                return 0;
            }
            long step = Math.max(1L, frameMillis);
            return (int) ((System.currentTimeMillis() / step) % frames);
        }
    }

    /**
     * Reads the PNG header and the neighbouring {@code .png.mcmeta} without
     * decoding the pixels, so a 384x5760 strip costs nothing at load time. Only
     * the frame count and the frame's share of the image height are needed: the
     * UVs then get squeezed into that slice.
     */
    private static TextureAnimation readTextureAnimation(Identifier texture) {
        String resource = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
        int width;
        int height;
        try (InputStream png = WingModelRenderer.class.getResourceAsStream(resource)) {
            if (png == null) {
                return STILL;
            }
            byte[] header = png.readNBytes(24);
            if (header.length < 24 || (header[0] & 0xFF) != 0x89 || header[1] != 'P') {
                return STILL;
            }
            width = bigEndianInt(header, 16);
            height = bigEndianInt(header, 20);
        } catch (IOException exception) {
            return STILL;
        }
        int frameWidth = 0;
        int frameHeight = 0;
        long frameMillis = 50L;
        try (InputStream meta = WingModelRenderer.class.getResourceAsStream(resource + ".mcmeta")) {
            if (meta != null) {
                JsonObject root = JsonParser.parseReader(
                        new InputStreamReader(meta, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("animation")) {
                    JsonObject animation = root.getAsJsonObject("animation");
                    if (animation.has("frametime")) {
                        // Minecraft animation frametime is measured in ticks.
                        frameMillis = Math.max(1L, animation.get("frametime").getAsLong()) * 50L;
                    }
                    if (animation.has("width")) {
                        frameWidth = animation.get("width").getAsInt();
                    }
                    if (animation.has("height")) {
                        frameHeight = animation.get("height").getAsInt();
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            return STILL;
        }
        if (frameHeight <= 0) {
            frameHeight = frameWidth > 0 ? frameWidth : width;
        }
        if (frameHeight <= 0 || width <= 0 || height <= 0) {
            return STILL;
        }
        int frames = height / frameHeight;
        if (frames <= 1 || height % frameHeight != 0) {
            return STILL;
        }
        return new TextureAnimation(frames, (float) frameHeight / height, frameMillis);
    }

    private static int bigEndianInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static float[] vector(JsonObject json, String name, float[] fallback) {
        if (!json.has(name) || !json.get(name).isJsonArray()) {
            return fallback;
        }
        JsonArray array = json.getAsJsonArray(name);
        float[] values = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    private record Bone(String name, String parent, float[] pivot, float[] rotation, List<Plane> planes) {
        private float pivotX() { return pivot[0]; }
        private float pivotY() { return pivot[1]; }
        private float pivotZ() { return pivot[2]; }
        private float rotationX() { return (float) Math.toRadians(rotation[0]); }
        private float rotationY() { return (float) Math.toRadians(rotation[1]); }
        private float rotationZ() { return (float) Math.toRadians(rotation[2]); }
    }

    private record Plane(float[] origin, float[] size, float[] pivot, float[] rotation,
                         float u, float v, float uSize, float vSize, String face) {
        private float pivotX() { return pivot[0]; }
        private float pivotY() { return pivot[1]; }
        private float pivotZ() { return pivot[2]; }
        private float rotationX() { return (float) Math.toRadians(rotation[0]); }
        private float rotationY() { return (float) Math.toRadians(rotation[1]); }
        private float rotationZ() { return (float) Math.toRadians(rotation[2]); }
    }

    private record Quad(Vector3f[] positions, float[] us, float[] vs, Vector3f normal) {
    }

    private record Model(List<Bone> bones, Map<String, BoneAnimation> animations) {
    }

    private record BoneAnimation(VectorTrack rotation, VectorTrack position, VectorTrack scale) {
    }

    private record VectorTrack(ComponentTrack[] components, float animationLength) {
        private float[] at(float time) {
            float sampleTime = animationLength > 0f ? time % animationLength : time;
            return new float[]{
                    components[0].at(sampleTime),
                    components[1].at(sampleTime),
                    components[2].at(sampleTime)
            };
        }
    }

    private record ComponentTrack(float value, String expression, List<Keyframe> keyframes) {
        private float at(float time) {
            if (expression != null) {
                return evaluate(expression, time);
            }
            if (keyframes.isEmpty()) {
                return value;
            }
            if (time <= keyframes.getFirst().time()) {
                return keyframes.getFirst().value().at(time);
            }
            for (int i = 1; i < keyframes.size(); i++) {
                Keyframe next = keyframes.get(i);
                if (time <= next.time()) {
                    Keyframe previous = keyframes.get(i - 1);
                    float amount = (time - previous.time()) / (next.time() - previous.time());
                    float from = previous.value().at(time);
                    float to = next.value().at(time);
                    if (previous.lerpMode().equalsIgnoreCase("catmullrom")) {
                        float before = keyframes.get(Math.max(0, i - 2)).value().at(time);
                        float after = keyframes.get(Math.min(keyframes.size() - 1, i + 1)).value().at(time);
                        float amount2 = amount * amount;
                        float amount3 = amount2 * amount;
                        return 0.5f * ((2f * from)
                                + (-before + to) * amount
                                + (2f * before - 5f * from + 4f * to - after) * amount2
                                + (-before + 3f * from - 3f * to + after) * amount3);
                    }
                    return from + (to - from) * amount;
                }
            }
            return keyframes.getLast().value().at(time);
        }
    }

    private record Keyframe(float time, ComponentTrack value, String lerpMode) {
    }

    private static final class ExpressionParser {
        private final String expression;
        private final float animationTime;
        private int position;

        private ExpressionParser(String expression, float animationTime) {
            this.expression = expression;
            this.animationTime = animationTime;
        }

        private float parse() {
            float value = expression();
            skipWhitespace();
            if (position != expression.length()) {
                throw new IllegalArgumentException("Unexpected token at " + position);
            }
            return value;
        }

        private float expression() {
            float value = term();
            while (true) {
                skipWhitespace();
                if (take('+')) {
                    value += term();
                } else if (take('-')) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        private float term() {
            float value = factor();
            while (true) {
                skipWhitespace();
                if (take('*')) {
                    value *= factor();
                } else if (take('/')) {
                    value /= factor();
                } else if (take('%')) {
                    value %= factor();
                } else {
                    return value;
                }
            }
        }

        private float factor() {
            skipWhitespace();
            if (take('-')) {
                return -factor();
            }
            if (take('+')) {
                return factor();
            }
            if (take('(')) {
                float value = expression();
                expect(')');
                return value;
            }
            if (position < expression.length()
                    && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                return number();
            }
            String identifier = identifier().toLowerCase(java.util.Locale.ROOT);
            if (identifier.equals("query.anim_time") || identifier.equals("query.time")) {
                return animationTime;
            }
            if (identifier.equals("nan")) {
                if (WARNED_ANIMATION_EXPRESSIONS.add(expression)) {
                    System.err.println("[OctoClient] NaN in cosmetic animation expression; treating it as zero: "
                            + expression);
                }
                return 0f;
            }
            if (identifier.equals("math.sin") || identifier.equals("math.cos")
                    || identifier.equals("math.abs")) {
                expect('(');
                float argument = expression();
                expect(')');
                if (identifier.equals("math.abs")) {
                    return Math.abs(argument);
                }
                // Molang's trigonometry works in degrees, like the rest of the
                // animation format; feeding the values to Math.sin raw made every
                // flap run roughly 57 times too fast.
                double radians = Math.toRadians(argument);
                return identifier.equals("math.sin") ? (float) Math.sin(radians) : (float) Math.cos(radians);
            }
            throw new IllegalArgumentException("Unknown identifier " + identifier);
        }

        private float number() {
            int start = position;
            while (position < expression.length()
                    && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                position++;
            }
            return Float.parseFloat(expression.substring(start, position));
        }

        private String identifier() {
            int start = position;
            while (position < expression.length()
                    && (Character.isLetterOrDigit(expression.charAt(position))
                    || expression.charAt(position) == '.' || expression.charAt(position) == '_')) {
                position++;
            }
            if (start == position) {
                throw new IllegalArgumentException("Expected value at " + position);
            }
            return expression.substring(start, position);
        }

        private boolean take(char token) {
            if (position < expression.length() && expression.charAt(position) == token) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char token) {
            skipWhitespace();
            if (!take(token)) {
                throw new IllegalArgumentException("Expected '" + token + "' at " + position);
            }
        }

        private void skipWhitespace() {
            while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) {
                position++;
            }
        }
    }
}
