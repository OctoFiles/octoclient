package dev.octoclient.module.cosmetics;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

public final class BandanaDesigns {
    private static final List<String> FILES = List.of(
            "black_lightning_bandana/black_lightning_bandana.png",
            "cny_bandana/cny_bandana.png",
            "green_lightning_bandana/green_lightning_bandana.png",
            "heaven_lightning_bandana/heaven_lightning_bandana.png",
            "norisk_inferno_bandana/norisk_inferno_bandana.png",
            "norisk_plasma_bandana/norisk_plasma_bandana.png",
            "pink_lightning_bandana/pink_lightning_bandana.png",
            "purple_lightning_bandana/purple_lightning_bandana.png",
            "red_lightning_bandana/red_lightning_bandana.png",
            "spring_bandana/spring_bandana.png",
            "summer_love_bandana/summer_love_bandana.png",
            "turquoise_lightning_bandana/turquoise_lightning_bandana.png",
            "white_lightning_bandana/white_lightning_bandana.png"
    );

    public static final List<Design> ALL = FILES.stream()
            .map(BandanaDesigns::create)
            .toList();

    private BandanaDesigns() {
    }

    private static Design create(String texturePath) {
        String fileName = texturePath.substring(texturePath.lastIndexOf('/') + 1, texturePath.length() - 4);
        StringBuilder name = new StringBuilder();
        for (String word : fileName.split("_")) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return new Design(name.toString(), texturePath);
    }

    public static List<String> names() {
        return ALL.stream().map(Design::name).toList();
    }

    public static Design byName(String name) {
        return ALL.stream().filter(design -> design.name().equals(name)).findFirst().orElse(null);
    }

    public record Design(String name, String texturePath) {
        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("octoclient",
                    "textures/cosmetic/bandanas/" + texturePath);
        }

        public String geometryResource() {
            return "/assets/octoclient/cosmetic/bandanas/"
                    + texturePath.substring(0, texturePath.length() - 4) + ".geo.json";
        }

        public String animationResource() {
            return "/assets/octoclient/cosmetic/bandanas/"
                    + texturePath.substring(0, texturePath.length() - 4) + ".animation.json";
        }
    }
}
