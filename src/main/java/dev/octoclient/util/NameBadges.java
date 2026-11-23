package dev.octoclient.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * OctoClient badge marks rendered next to the names of team members.
 *
 * <p>Each badge is a single glyph of {@code octoclient:badges}
 * (assets/octoclient/font/badges.json), so it can be prepended to any
 * name component: world nametags and the tab list.</p>
 */
public final class NameBadges {
    /** Bitmap font holding the octopus marks. */
    public static final Identifier FONT = Identifier.fromNamespaceAndPath("octoclient", "badges");
    private static final UUID DOCTOFROG = UUID.fromString("da2ef042-b12c-4da4-9605-17a17bb89ae3");
    private static final UUID VOIDWALKER120 = UUID.fromString("b50d1174-36fa-4390-bb78-d9204a200806");
    private static final UUID DOCTOCAPYBARA = UUID.fromString("cab4836d-c40c-4c9d-b6c6-9c6dd51aaad8");
    private static final Map<UUID, Character> BADGES = Map.of(
            DOCTOFROG, '\ue020',
            VOIDWALKER120, '\ue021',
            DOCTOCAPYBARA, '\ue022');
    /**
     * Offline, migrated or launcher-created accounts can report a different
     * UUID than the one a badge is registered for, so the name is matched too.
     */
    private static final Map<String, Character> NAME_BADGES = Map.of(
            "doctofrog", '\ue020',
            "voidwalker120", '\ue021',
            "doctocapybara", '\ue022');

    private NameBadges() {
    }

    /** Badge glyph plus its trailing space, or {@code null} when the player has none. */
    public static Component mark(UUID playerId) {
        return mark(playerId, null);
    }

    /**
     * Badge glyph for a player, looked up by UUID first and by name second, or
     * {@code null} when they have none.
     */
    public static Component mark(UUID playerId, String playerName) {
        Character glyph = playerId == null ? null : BADGES.get(playerId);
        if (glyph == null && playerName != null && !playerName.isEmpty()) {
            glyph = NAME_BADGES.get(playerName.toLowerCase(Locale.ROOT));
        }
        if (glyph == null) {
            return null;
        }
        return Component.literal(glyph + " ")
                .withStyle(Style.EMPTY.withFont(new FontDescription.Resource(FONT)));
    }

    /** Puts the player's badge in front of {@code name}, unchanged when they have none. */
    public static Component prefix(UUID playerId, Component name) {
        return prefix(playerId, null, name);
    }

    /** Puts the player's badge in front of {@code name}, matching the UUID or the name. */
    public static Component prefix(UUID playerId, String playerName, Component name) {
        Component mark = mark(playerId, playerName);
        if (mark == null || name == null) {
            return name;
        }
        MutableComponent result = Component.empty();
        result.append(mark);
        result.append(name);
        return result;
    }
}
