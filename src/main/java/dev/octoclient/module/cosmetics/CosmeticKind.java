package dev.octoclient.module.cosmetics;

/**
 * Sub-groups of the Cosmetics tab. The dashboard shows them as a filter row
 * inside the tab, and sorts the cards by this order.
 */
public enum CosmeticKind {
    CAPE("Capes"),
    BANDANA("Bandanas"),
    SHIELD("Shields"),
    WINGS("Wings"),
    ACCESSORY("Accessories"),
    HAT("Hats"),
    NAMETAG("Name Tags"),
    BLC_ICONS("BLC Icons"),
    AURA("Aura");

    public final String title;

    CosmeticKind(String title) {
        this.title = title;
    }
}
