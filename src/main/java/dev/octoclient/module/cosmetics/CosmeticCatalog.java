package dev.octoclient.module.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The bundled Badlion cosmetic catalog and its resource mappings. */
public final class CosmeticCatalog {
    private static final String RESOURCE = "/assets/octoclient/cosmetics/catalog.json";
    private static final String MODEL_LAYOUT_RESOURCE = "/assets/octoclient/cosmetics/reference/models.json";
    private static final List<Entry> ENTRIES;
    private static final Map<String, Entry> BY_KEY;

    static {
        List<Entry> entries = new ArrayList<>();
        Map<String, Entry> byKey = new HashMap<>();
        Map<String, ModelLayout> modelLayouts = readModelLayouts();
        try (InputStream stream = CosmeticCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled cosmetic catalog: " + RESOURCE);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonArray items = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("items");
                if (items == null) {
                    throw new IllegalStateException("Cosmetic catalog has no items array: " + RESOURCE);
                }
                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    List<String> modelPaths = readPaths(item, "modelPaths");
                    List<String> animationPaths = readPaths(item, "animationPaths");
                    String type = item.get("type").getAsString();
                    Entry entry = new Entry(
                            type,
                            item.get("id").getAsInt(),
                            item.get("name").getAsString(),
                            item.get("hasModel").getAsBoolean(),
                            item.get("texture").getAsString(),
                            modelPaths,
                            animationPaths,
                            item.get("scale").getAsFloat(),
                            item.get("flipUVs").getAsBoolean(),
                            item.get("translationX").getAsFloat(),
                            item.get("translationY").getAsFloat(),
                            item.get("translationZ").getAsFloat());
                    entry = applyModelLayout(entry, modelLayouts.get(layoutKey(type, modelName(entry))));
                    if (byKey.putIfAbsent(entry.key(), entry) != null) {
                        throw new IllegalStateException("Duplicate cosmetic catalog key: " + entry.key());
                    }
                    entries.add(entry);
                }
            }
        } catch (IOException | IllegalStateException exception) {
            throw new ExceptionInInitializerError(
                    new IllegalStateException("Could not load bundled cosmetic catalog", exception));
        }
        // Many wing arts share one display name ("Flame", "Angel", ...). Each
        // duplicate gets an extra name derived from its own texture so every
        // wings art can be told apart in the list.
        Map<String, Integer> nameCounts = new HashMap<>();
        for (Entry entry : entries) {
            nameCounts.merge(entry.type() + "|" + entry.name(), 1, Integer::sum);
        }
        Map<String, Integer> seen = new HashMap<>();
        List<Entry> named = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            String key = entry.type() + "|" + entry.name();
            if (nameCounts.getOrDefault(key, 0) > 1 || "WINGS".equals(entry.type())) {
                seen.merge(key, 1, Integer::sum);
                named.add(entry.renamed(entry.name() + " " + variantLabel(entry)));
            } else {
                named.add(entry);
            }
        }
        entries = named;

        entries.sort(Comparator.comparing(Entry::type).thenComparingInt(Entry::id));
        ENTRIES = List.copyOf(entries);
        BY_KEY = Map.copyOf(byKey);
    }

    private CosmeticCatalog() {
    }

    /** Short, readable tag taken from a cosmetic's own texture file name. */
    private static String variantLabel(Entry entry) {
        String path = entry.texturePath();
        String file = path.substring(path.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        if (dot > 0) {
            file = file.substring(0, dot);
        }
        String label = file.replace('_', ' ').trim();
        String lower = label.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("wing ")) {
            label = label.substring(5).trim();
        } else if (lower.startsWith("wings ")) {
            label = label.substring(6).trim();
        }
        if (label.isEmpty() || label.equalsIgnoreCase("wing") || label.equalsIgnoreCase("wings")                    || label.equalsIgnoreCase(entry.name())) {
            return String.valueOf(entry.id());
        }
        return label;
    }

    private static List<String> readPaths(JsonObject item, String name) {
        JsonArray array = item.getAsJsonArray(name);
        if (array == null) {
            return List.of();
        }
        List<String> paths = new ArrayList<>(array.size());
        for (JsonElement path : array) {
            paths.add(path.getAsString());
        }
        return List.copyOf(paths);
    }

    /**
     * The catalog is intentionally generic, while its OBJ/MD5 sources are not:
     * a backpack and a wing can use different origins, handedness and scale.
     * Keep the author-provided transform beside the assets and apply it per
     * model, rather than forcing every back cosmetic through the 1/16 default.
     */
    private static Map<String, ModelLayout> readModelLayouts() {
        Map<String, ModelLayout> layouts = new HashMap<>();
        try (InputStream stream = CosmeticCatalog.class.getResourceAsStream(MODEL_LAYOUT_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled cosmetic model layouts: " + MODEL_LAYOUT_RESOURCE);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject types = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("modelData");
                for (Map.Entry<String, JsonElement> typeEntry : types.entrySet()) {
                    for (Map.Entry<String, JsonElement> modelEntry : typeEntry.getValue().getAsJsonObject().entrySet()) {
                        JsonObject model = modelEntry.getValue().getAsJsonObject();
                        JsonObject translation = model.has("translation")
                                ? model.getAsJsonObject("translation") : null;
                        float scale = model.has("scale") ? model.get("scale").getAsFloat() : Float.NaN;
                        layouts.put(layoutKey(typeEntry.getKey(), modelEntry.getKey()), new ModelLayout(
                                scale,
                                translation == null ? 0f : translation.get("x").getAsFloat(),
                                translation == null ? 0f : translation.get("y").getAsFloat(),
                                translation == null ? 0f : translation.get("z").getAsFloat(),
                                model.has("flipUVs") && model.get("flipUVs").getAsBoolean()));
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw new ExceptionInInitializerError(
                    new IllegalStateException("Could not load cosmetic model layouts", exception));
        }
        return Map.copyOf(layouts);
    }

    private static Entry applyModelLayout(Entry entry, ModelLayout layout) {
        if (layout == null || !usesCompatibleSourceLayout(entry.type())) {
            return entry;
        }
        // A zero scale in the source data means "use the renderer default".
        float scale = Float.isFinite(layout.scale()) && layout.scale() != 0f ? layout.scale() : entry.scale();
        return new Entry(entry.type(), entry.id(), entry.name(), entry.hasModel(), entry.texturePath(),
                entry.modelPaths(), entry.animationPaths(), scale, layout.flipUVs(), layout.x(), layout.y(), layout.z());
    }

    /**
     * The original client uses a separate, animated renderer for wings. Its
     * layout file contains renderer directives such as scale {@code -2}, not
     * world-space OBJ scales; applying those values in the catalog renderer
     * makes the wing mesh explode. Backpacks and tails are static OBJ/MD5
     * attachments and share this renderer's coordinate convention.
     */
    private static boolean usesCompatibleSourceLayout(String type) {
        return type.equals("BACK") || type.equals("TAIL");
    }

    private static String modelName(Entry entry) {
        if (entry.modelPaths().isEmpty()) {
            return "";
        }
        String path = entry.modelPaths().getFirst();
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return path.substring(slash + 1, dot < slash ? path.length() : dot);
    }

    private static String layoutKey(String type, String modelName) {
        return type + ":" + modelName;
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Entry byKey(String type, int id) {
        return BY_KEY.get(type + ":" + id);
    }

    public static List<Entry> fromSelection(String selection) {
        if (selection == null || selection.isEmpty()) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        Set<String> selectedTypes = new HashSet<>();
        for (String token : selection.split(",", -1)) {
            Entry entry = BY_KEY.get(token);
            if (entry == null || !selectedTypes.add(entry.type())) {
                return List.of();
            }
            entries.add(entry);
        }
        return List.copyOf(entries);
    }

    public static String encodeSelection(Collection<Entry> entries) {
        Map<String, Entry> byType = new java.util.TreeMap<>();
        for (Entry entry : entries) {
            if (byType.putIfAbsent(entry.type(), entry) != null) {
                throw new IllegalArgumentException("More than one selected cosmetic for " + entry.type());
            }
        }
        return byType.values().stream().map(Entry::key).collect(java.util.stream.Collectors.joining(","));
    }

    public static boolean isValidSelection(String selection) {
        if (selection == null || selection.length() > 1024) {
            return false;
        }
        return selection.isEmpty() || !fromSelection(selection).isEmpty();
    }

    public record Entry(String type, int id, String name, boolean hasModel, String texturePath,
                        List<String> modelPaths, List<String> animationPaths, float scale, boolean flipUVs,
                        float translationX, float translationY, float translationZ) {
        public Entry {
            modelPaths = List.copyOf(modelPaths);
            animationPaths = List.copyOf(animationPaths);
        }

        public String key() {
            return type + ":" + id;
        }

        /** Same entry under a different display name. */
        public Entry renamed(String newName) {
            return new Entry(type, id, newName, hasModel, texturePath, modelPaths, animationPaths,
                    scale, flipUVs, translationX, translationY, translationZ);
        }

        public CosmeticKind kind() {
            return switch (type) {
                case "CAPE" -> CosmeticKind.CAPE;
                case "BANDANA" -> CosmeticKind.BANDANA;
                case "SHIELD" -> CosmeticKind.SHIELD;
                case "WINGS" -> CosmeticKind.WINGS;
                case "HAT", "EARS", "NOSE", "MOUTH", "EYES", "NECK", "MASK" -> CosmeticKind.HAT;
                case "BLC_NAMETAG_ICON" -> CosmeticKind.BLC_ICONS;
                case "INSIDER_NAMETAG_ICON", "INSIDER_NAMETAG_BORDER" ->
                        CosmeticKind.NAMETAG;
                case "AURA", "SPRAY", "PROJECTILE" -> CosmeticKind.AURA;
                default -> CosmeticKind.ACCESSORY;
            };
        }

        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("octoclient", "textures/cosmetics/" + texturePath);
        }
    }

    private record ModelLayout(float scale, float x, float y, float z, boolean flipUVs) {
    }
}
