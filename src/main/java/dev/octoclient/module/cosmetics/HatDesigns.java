package dev.octoclient.module.cosmetics;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HatDesigns {
    private static final List<String> MODELS = List.of(
            "amethyst_halo/amethyst_halo",
            "angel_halo/angel_halo",
            "angel_halo/variants/blue_angel_halo/blue_angel_halo",
            "angel_halo/variants/orange_angel_halo/orange_angel_halo",
            "angel_halo/variants/purple_angel_halo/purple_angel_halo",
            "angel_halo/variants/red_angel_halo/red_angel_halo",
            "angel_halo/variants/sparkling_halo/sparkling_halo",
            "antlers/antlers",
            "axolotl_beanie/axolotl_beanie",
            "axolotl_beanie/variants/blue_axolotl_beanie/blue_axolotl_beanie",
            "axolotl_beanie/variants/green_axolotl_beanie/green_axolotl_beanie",
            "axolotl_beanie/variants/yellow_axolotl_beanie/yellow_axolotl_beanie",
            "beach_hat/beach_hat",
            "beani/beani",
            "bear_beanie/bear_beanie",
            "bee_beanie/bee_beanie",
            "black_onyx_halo/black_onyx_halo",
            "bugcatcher_hat/bugcatcher_hat",
            "bunny_beanie/bunny_beanie",
            "bunny_ears/bunny_ears",
            "bunny_ears/variants/brown_bunny_ears/brown_bunny_ears",
            "cap/cap",
            "capy_hat/capy_hat",
            "capy_plush/capy_plush",
            "christmas_halo/christmas_halo",
            "christmas_hat/christmas_hat",
            "crabhat/crabhat",
            "creaking_hat/creaking_hat",
            "demon_horns/demon_horns",
            "duckhat/duckhat",
            "fedora/fedora",
            "fire_hat/fire_hat",
            "flowercrown/flowercrown",
            "flowerhat/flowerhat",
            "fox_beanie/fox_beanie",
            "frankenbolts/frankenbolts",
            "froggy_hat/froggy_hat",
            "galaxy_halo/galaxy_halo",
            "gold_rice_hat/gold_rice_hat",
            "greeke_helmet/greeke_helmet",
            "headphones/headphones",
            "headphones/variants/blue_headphones/blue_headphones",
            "holycapy_plush/holycapy_plush",
            "jester_hat/jester_hat",
            "knight_hat/knight_hat",
            "lotusflower_wreath/lotusflower_wreath",
            "manta_hat/manta_hat",
            "mini_crown/mini_crown",
            "nemes_hat/nemes_hat",
            "nitro_crystal/nitro_crystal",
            "ny_hat/ny_hat",
            "ny_hat/variants/red_ny_hat/red_ny_hat",
            "octopus_hat/octopus_hat",
            "pompom/pompom",
            "present/present",
            "present/variants/blue_present/blue_present",
            "present/variants/green_present/green_present",
            "propellor/propellor",
            "propellor/variants/propellorog/propellorog",
            "pumpkin/pumpkin",
            "purple_obsidian_halo/purple_obsidian_halo",
            "rice_hat/rice_hat",
            "rice_hat/variants/black_rice_hat/black_rice_hat",
            "rice_hat/variants/white_rice_hat/white_rice_hat",
            "royal_crown/royal_crown",
            "samurai_hat/samurai_hat",
            "shark_hat/shark_hat",
            "snorkel/snorkel",
            "snorkel/variants/snorkelog/snorkelog",
            "snowman/snowman",
            "sombrero/sombrero",
            "speaker_hat/speaker_hat",
            "straw_hat/straw_hat",
            "summerhat/summerhat",
            "sunflower_wreath/sunflower_wreath",
            "top_hat/top_hat",
            "trinkvogel/trinkvogel",
            "ushanka/ushanka",
            "white_crystal_halo/white_crystal_halo",
            "winnercrown/winnercrown",
            "winter_hat/winter_hat",
            "winter_hat/variants/blue_winter_hat/blue_winter_hat",
            "winter_hat/variants/green_winter_hat/green_winter_hat",
            "winter_hat/variants/orange_winter_hat/orange_winter_hat",
            "winter_hat/variants/pink_winter_hat/pink_winter_hat",
            "winter_hat/variants/purple_winter_hat/purple_winter_hat",
            "winter_hat/variants/yellow_winter_hat/yellow_winter_hat",
            "wizard_hat/wizard_hat",
            "wm_hat/wm_hat",
            "xmashat/xmashat");
    private static final Map<String, float[]> SETTINGS = Map.ofEntries(
            Map.entry("black_onyx_halo", new float[]{1.3f, 0f, 0f, 0f}),
            Map.entry("bunny_ears", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("christmas_hat", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("demon_horns", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("froggy_hat", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("galaxy_halo", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("greeke_helmet", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("jester_hat", new float[]{1f, 0f, 0f, -0.025f}),
            Map.entry("mini_crown", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("nemes_hat", new float[]{1.025f, 0f, 0f, 0f}),
            Map.entry("nitro_crystal", new float[]{1f, 0f, -3.5f, 0f}),
            Map.entry("propellor", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("purple_obsidian_halo", new float[]{1.3f, 0f, 0f, 0f}),
            Map.entry("sombrero", new float[]{1f, 0f, 0f, -0.025f}),
            Map.entry("trinkvogel", new float[]{0.9575f, 0f, 0f, -0.025f}),
            Map.entry("white_crystal_halo", new float[]{1.3f, 0f, 0f, 0f}),
            Map.entry("xmashat", new float[]{1.05f, 0f, 0f, -0.025f}));

    public static final List<Design> ALL = MODELS.stream().map(HatDesigns::create).toList();

    private HatDesigns() {
    }

    private static Design create(String modelPath) {
        String fileName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        StringBuilder name = new StringBuilder();
        for (String word : fileName.split("_")) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        String baseFolder = modelPath.substring(0, modelPath.indexOf('/'));
        float[] settings = SETTINGS.getOrDefault(baseFolder, new float[]{1f, 0f, 0f, 0f});
        return new Design(name.toString(), modelPath, settings[0], settings[1], settings[2], settings[3]);
    }

    public static List<String> names() {
        return ALL.stream().map(Design::name).toList();
    }

    public static Design byName(String name) {
        return ALL.stream().filter(design -> design.name().equals(name)).findFirst().orElse(null);
    }

    public record Design(String name, String modelPath, float scale,
                         float offsetX, float offsetY, float offsetZ) {
        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("octoclient", "textures/cosmetic/hats/" + modelPath + ".png");
        }

        public String geometryResource() {
            return "/assets/octoclient/cosmetic/hats/" + modelPath + ".geo.json";
        }

        public String animationResource() {
            return "/assets/octoclient/cosmetic/hats/" + modelPath + ".animation.json";
        }
    }
}
