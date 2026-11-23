package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** One searchable, mutually exclusive catalog item in the cosmetics dashboard. */
public final class CatalogCosmetic extends Module implements CosmeticPart {
    private final CosmeticCatalog.Entry entry;

    public CatalogCosmetic(CosmeticCatalog.Entry entry) {
        super(entry.type().replace('_', ' ') + ": " + entry.name() + " (#" + entry.id() + ")",
                "Wear this catalog cosmetic and share it with other OctoClient users", 'C');
        this.entry = entry;
    }

    public CosmeticCatalog.Entry entry() {
        return entry;
    }

    @Override
    public CosmeticKind kind() {
        return entry.kind();
    }

    @Override
    protected void onEnable() {
        disableCatalogType(entry.type(), this);
        for (Module module : ModuleManager.get().getModules()) {
            boolean conflicts = switch (entry.type()) {
                case "CAPE" -> module instanceof Cape;
                case "BANDANA" -> module instanceof Bandana;
                case "WINGS" -> module instanceof Wings;
                case "HAT" -> module instanceof Hat || module instanceof HatDesign;
                case "AURA" -> module instanceof Aura;
                default -> false;
            };
            if (conflicts && module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }

    public static void disableCatalogType(String type, Module except) {
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof CatalogCosmetic cosmetic && cosmetic != except
                    && cosmetic.entry.type().equals(type) && cosmetic.isEnabled()) {
                cosmetic.setEnabled(false);
            }
        }
    }

    public static Item iconFor(CosmeticCatalog.Entry entry) {
        return switch (entry.type()) {
            case "CAPE" -> Items.ELYTRA;
            case "BANDANA", "HAT", "EARS", "NOSE", "MOUTH", "EYES", "MASK" -> Items.LEATHER_HELMET;
            case "WINGS" -> Items.PHANTOM_MEMBRANE;
            case "SHIELD" -> Items.SHIELD;
            case "BOW" -> Items.BOW;
            case "SWORD" -> Items.DIAMOND_SWORD;
            case "PET", "PLAYER_PET" -> Items.LEAD;
            case "INSIDER_NAMETAG_ICON", "BLC_NAMETAG_ICON", "INSIDER_NAMETAG_BORDER" -> Items.NAME_TAG;
            case "AURA", "PROJECTILE" -> Items.END_ROD;
            case "SPRAY" -> Items.PAINTING;
            default -> Items.LEATHER_CHESTPLATE;
        };
    }
}
