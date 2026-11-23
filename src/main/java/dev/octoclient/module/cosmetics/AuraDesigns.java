package dev.octoclient.module.cosmetics;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.stream.Stream;

public final class AuraDesigns {
    private static final List<String> LEGACY_SHAPES = List.of("Ring", "Halo", "Column");

    public static final List<Design> ALL = List.of(
            new Design("Autumn Aura", "autumn_aura/autumn_aura.png", "autumn_aura/autumn_aura", 1f),
            new Design("Bubble Aura", "bubble_aura/bubble_aura.png", "bubble_aura/bubble_aura", 1f),
            new Design("Creaking Aura", "creaking_aura/creaking_aura.png", "creaking_aura/creaking_aura", 1f),
            new Design("Dark Pumpkin Aura",
                    "pumpkin_aura/variants/dark_pumpkin_aura/dark_pumpkin_aura.png",
                    "pumpkin_aura/variants/dark_pumpkin_aura/dark_pumpkin_aura", 1f),
            new Design("Easter Aura", "easter_aura/easter_aura.png", "easter_aura/easter_aura", 1f),
            new Design("Enchant Aura", "enchant_aura/enchant.png", "enchant_aura/enchant_aura", 1f, true),
            new Design("Fire Aura", "fire_aura/fire_aura.png", "fire_aura/fire_aura", 1f),
            new Design("Firework Aura", "firework_aura/firework_aura.png", "firework_aura/firework_aura", 1.2f),
            new Design("Gamescom Aura 25",
                    "gamescomaura25/gamescomaura25.png", "gamescomaura25/gamescomaura25", 1f),
            new Design("Lotusflower Aura",
                    "lotusflower_aura/lotusflower_aura.png", "lotusflower_aura/lotusflower_aura", 1.2f),
            new Design("Neon Aura", "neon_aura/neon_aura.png", "neon_aura/neon_aura", 1f),
            new Design("Blue Neon Aura",
                    "neon_aura/variants/blue_neon_aura/blue_neon_aura.png",
                    "neon_aura/variants/blue_neon_aura/blue_neon_aura", 1f),
            new Design("Green Neon Aura",
                    "neon_aura/variants/green_neon_aura/green_neon_aura.png",
                    "neon_aura/variants/green_neon_aura/green_neon_aura", 1f),
            new Design("Orange Neon Aura",
                    "neon_aura/variants/orange_neon_aura/orange_neon_aura.png",
                    "neon_aura/variants/orange_neon_aura/orange_neon_aura", 1f),
            new Design("Note Aura", "note_aura/note_aura.png", "note_aura/note_aura", 1f),
            new Design("Pumpkin Aura", "pumpkin_aura/pumpkin_aura.png", "pumpkin_aura/pumpkin_aura", 1f),
            new Design("Sakura Aura", "sakura_aura/sakura_aura.png", "sakura_aura/sakura_aura", 1f),
            new Design("Snow Aura", "snow_aura/snow_aura.png", "snow_aura/snow_aura", 1f),
            new Design("Soul Aura", "soul_aura/soul_aura.png", "soul_aura/soul_aura", 1.2f),
            new Design("Splash Aura", "splash_aura/splash_aura.png", "splash_aura/splash_aura", 0.9f),
            new Design("Spring Aura", "spring_aura/spring_aura.png", "spring_aura/spring_aura", 1f),
            new Design("Sunflower Aura",
                    "sunflower_aura/sunflower_aura.png", "sunflower_aura/sunflower_aura", 1.2f),
            new Design("Yang Aura", "yang_aura/yang_aura.png", "yang_aura/yang_aura", 0.9f),
            new Design("Yin Aura", "yin_aura/yin_aura.png", "yin_aura/yin_aura", 1f)
    );
    public static final List<String> OPTIONS = Stream.concat(LEGACY_SHAPES.stream(), names().stream()).toList();

    private AuraDesigns() {
    }

    public static List<String> names() {
        return ALL.stream().map(Design::name).toList();
    }

    public static Design byName(String name) {
        return ALL.stream().filter(design -> design.name().equals(name)).findFirst().orElse(null);
    }

    public static boolean isValid(String name) {
        return OPTIONS.contains(name);
    }

    public record Design(String name, String texturePath, String modelPath, float scale, boolean particleEffect) {
        public Design(String name, String texturePath, String modelPath, float scale) {
            this(name, texturePath, modelPath, scale, false);
        }

        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("octoclient", "textures/cosmetic/auras/" + texturePath);
        }

        public String geometryResource() {
            return "/assets/octoclient/cosmetic/auras/" + modelPath + ".geo.json";
        }

        public String animationResource() {
            return "/assets/octoclient/cosmetic/auras/" + modelPath + ".animation.json";
        }
    }
}
