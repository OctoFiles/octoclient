package dev.octoclient.module.cosmetics;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AccessoryDesigns {
    private static final Set<String> MINECRAFT_SKIN_DESIGNS =
            Set.of("minimearm", "minimehand", "minimehead", "minimeschultersit");
    private static final List<String> FOLDERS = List.of(
            "amor", "bowtie", "bug_box", "bunny_tail", "candy_cane", "creaking_staff",
            "dimension_sleeve", "duck_ring", "float", "gold_katana", "katana", "lava_staff",
            "minimearm", "minimehand", "minimehead", "minimeschultersit", "minime_charm",
            "minime_necklace", "mizu", "nrc_chain", "pumpkin_slicer", "scarf", "winter_scarf",
            "wm_flag", "wm_flowerchain", "yang_katana", "yinyang_dagger", "yin_katana");
    private static final Map<String, Set<String>> ATTACHMENTS = Map.of(
            "dimension_sleeve", Set.of("armorRightArm"),
            "minimearm", Set.of("armorLeftArm"),
            "minimehand", Set.of("armorLeftArm"),
            "minimehead", Set.of("armorHead"),
            "scarf", Set.of("armorBody", "armorHead"));
    private static final Map<String, Float> SCALES = Map.of(
            "amor", 1.1f,
            "bug_box", 1.1f,
            "bunny_tail", 1.1f,
            "gold_katana", 0.4f,
            "yang_katana", 0.4f,
            "yin_katana", 0.4f);
    private static final Map<String, String> REVERSED_SWORD_BONES = Map.of(
            "gold_katana", "sword",
            "katana", "katana",
            "yang_katana", "sword",
            "yin_katana", "sword");

    public static final List<Design> ALL = FOLDERS.stream()
            .map(AccessoryDesigns::create)
            .toList();

    private AccessoryDesigns() {
    }

    private static Design create(String folder) {
        String[] words = folder.split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return new Design(name.toString(), folder, ATTACHMENTS.getOrDefault(folder, Set.of("armorBody")),
                SCALES.getOrDefault(folder, 1f), REVERSED_SWORD_BONES.get(folder));
    }

    public static List<String> names() {
        return ALL.stream().map(Design::name).toList();
    }

    public static Design byName(String name) {
        return ALL.stream().filter(design -> design.name().equals(name)).findFirst().orElse(null);
    }

    public static boolean usesMinecraftSkin(String designName) {
        Design design = byName(designName);
        return design != null && MINECRAFT_SKIN_DESIGNS.contains(design.folder());
    }

    public record Design(String name, String folder, Set<String> attachmentBones, float scale,
                         String reversedSwordBone) {
        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("octoclient", "textures/cosmetic/accessories/" + folder + ".png");
        }

        public String geometryResource() {
            return "/assets/octoclient/cosmetic/accessories/" + folder + "/" + folder + ".geo.json";
        }

        public String animationResource() {
            return "/assets/octoclient/cosmetic/accessories/" + folder + "/" + folder + ".animation.json";
        }

    }
}
