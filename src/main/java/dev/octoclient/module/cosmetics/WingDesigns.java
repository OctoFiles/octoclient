package dev.octoclient.module.cosmetics;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WingDesigns {
    private static final String[] TEXTURES = {
            "black_onyx_wings/black_onyx_wings.png",
            "christmas_wings/christmas_wings.png",
            "dark_wings/dark_wings.png",
            "dark_wings/variants/ash_dark_wings/ash_dark_wings.png",
            "dark_wings/variants/black_dark_wings/black_dark_wings.png",
            "dimension_aether_wings/dimension_aether_wings.png",
            "dimension_end_wings/dimension_end_wings.png",
            "dimension_nether_wings/dimension_nether_wings.png",
            "fireworkwings_v2/fireworkwings_v2.png",
            "firework_wings/firework_wings.png",
            "galaxy_wings/galaxy_wings.png",
            "galaxy_wings/variants/blue_galaxy_wings/blue_galaxy_wings.png",
            "galaxy_wings/variants/orange_galaxy_wings/orange_galaxy_wings.png",
            "galaxy_wings/variants/pink_galaxy_wings/pink_galaxy_wings.png",
            "gold_wings/gold_wings.png",
            "gold_wings/variants/rose_gold_wings/rose_gold_wings.png",
            "heavenly_wings/heavenly_wings.png",
            "ice_dragon_wings/ice_dragon_wings.png",
            "ice_dragon_wings/variants/fire_dragon_wings/fire_dragon_wings.png",
            "ice_wings/ice_wings.png",
            "ice_wings/variants/darkblue_ice_wings/darkblue_ice_wings.png",
            "ice_wings/variants/gray_ice_wings/gray_ice_wings.png",
            "neon_rgb_wings/neon_rgb_wings.png",
            "neon_wings/neon_wings.png",
            "neon_wings/variants/green_neon_wings/green_neon_wings.png",
            "neon_wings/variants/orange_neon_wings/orange_neon_wings.png",
            "parrot_wings/parrot_wings.png",
            "parrot_wings/variants/blueparrot_wings/blueparrot_wings.png",
            "parrot_wings/variants/greenparrot_wings/greenparrot_wings.png",
            "parrot_wings/variants/yellowparrot_wings/yellowparrot_wings.png",
            "purple_obsidian_wings/purple_obsidian_wings.png",
            "white_crystal_wings/white_crystal_wings.png"
    };

    /**
     * Per-design scale and offset, taken from the original client's
     * {@code <name>.norisk.json} ({@code defaultSettings}). Values are in the
     * same units NoRisk uses: scale is a multiplier, offset is in pixels.
     * Variants inherit the settings of their base design (the first path
     * segment).
     */
    private static final Map<String, float[]> SETTINGS = Map.ofEntries(
            Map.entry("black_onyx_wings", new float[]{1.2f, 0f, 0f, 0.5f}),
            Map.entry("christmas_wings", new float[]{0.7f, 0f, 0f, -0.3f}),
            Map.entry("cloud_wings", new float[]{1f, 0f, -3f, 0f}),
            Map.entry("cny_wings", new float[]{1f, 0f, -3f, 0f}),
            Map.entry("dark_wings", new float[]{1f, 0f, 0f, 3f}),
            Map.entry("gold_wings", new float[]{1.15f, 0f, 0f, 0f}),
            Map.entry("ice_dragon_wings", new float[]{1.2f, 0f, -2f, 0f}),
            Map.entry("ice_wings", new float[]{1f, 0f, 0f, -0.025f}),
            Map.entry("neon_rgb_wings", new float[]{1.2f, 0f, 0f, 0f}),
            Map.entry("neon_wings", new float[]{1.2f, 0f, 0f, 0f}),
            Map.entry("parrot_wings", new float[]{0.6f, 0f, 0f, 0f}),
            Map.entry("purple_obsidian_wings", new float[]{1.2f, 0f, 0f, 3f}),
            Map.entry("white_crystal_wings", new float[]{1.2f, 0f, 0f, 3f}),
            Map.entry("yinyang_wingsvtwo", new float[]{1.2f, 0f, 0f, 3f}));
    /**
     * The bundled wings were authored small for the original client's camera;
     * at the vanilla player scale they read as stubs, so every design is lifted
     * by the same factor instead of editing each entry above.
     */
    public static final float GLOBAL_SCALE = 1.25f;
    public static final List<Design> ALL = createDesigns();

    private WingDesigns() {
    }

    private static List<Design> createDesigns() {
        List<Design> designs = new ArrayList<>(TEXTURES.length);
        for (String path : TEXTURES) {
            String fileName = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            String label = fileName.replace('_', ' ');
            StringBuilder title = new StringBuilder();
            for (String word : label.split(" ")) {
                if (!title.isEmpty()) {
                    title.append(' ');
                }
                title.append(word.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(word.substring(1));
            }
            String folder = path.substring(0, path.indexOf('/'));
            float[] settings = SETTINGS.getOrDefault(folder, new float[]{1f, 0f, 0f, 0f});
            designs.add(new Design(title.toString(), path, settings[0] * GLOBAL_SCALE,
                    settings[1], settings[2], settings[3]));
        }
        return List.copyOf(designs);
    }

    public static List<String> names() {
        return ALL.stream().map(Design::name).toList();
    }

    public static Design byName(String name) {
        return ALL.stream().filter(design -> design.name().equals(name)).findFirst().orElse(null);
    }

    public record Design(String name, String texturePath, float scale,
                         float offsetX, float offsetY, float offsetZ) {
        public Identifier texture() {
            String resourcePath = texturePath.substring(0, texturePath.length() - 4);
            return Identifier.fromNamespaceAndPath("octoclient", "textures/cosmetic/wings/" + resourcePath + ".png");
        }

        public String geometryResource() {
            String geometryPath = texturePath.substring(0, texturePath.length() - 4) + ".geo.json";
            return "/assets/octoclient/cosmetic/wings/" + geometryPath;
        }

        public String animationResource() {
            String animationPath = texturePath.substring(0, texturePath.length() - 4) + ".animation.json";
            return "/assets/octoclient/cosmetic/wings/" + animationPath;
        }
    }
}
