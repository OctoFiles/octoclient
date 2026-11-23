package dev.octoclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * Text helpers for the dashboard and the HUD editor.
 *
 * <p>All UI text renders with Minecraft's own font ({@code minecraft:default}):
 * the 8 px bitmap glyphs, one pixel of advance per glyph gap, and the vanilla
 * bold pass that draws every glyph a second time one pixel to the right. That
 * is what makes the dashboard read as part of the game instead of a web page
 * pasted on top of it.</p>
 *
 * <p>Sizes are therefore Minecraft sizes: {@code 1.0} = one glyph pixel = 8 px
 * of text, {@code 2.0} = the same glyphs at double size (16 px), exactly like
 * vanilla titles. Only Font Awesome is still a TTF, for the icon glyphs.</p>
 *
 * <p>The dashboard uses {@link #UI} rather than {@code minecraft:default}: it
 * is the same ascii atlas, but declared as an OctoClient font, so its glyph
 * sheet gets the mipmapped linear sampler from FontTextureMixin. At the
 * fractional GUI scales this UI renders at, that is the difference between
 * uneven doubled pixels and clean text.</p>
 */
public final class UiFonts {
    /** Vanilla ascii atlas declared in the octoclient namespace (font/ui.json). */
    public static final Identifier UI = Identifier.fromNamespaceAndPath("octoclient", "ui");
    /** Font Awesome Free 6.5.2 solid icon font (assets/octoclient/font/octo_icons.json). */
    public static final Identifier ICONS = Identifier.fromNamespaceAndPath("octoclient", "octo_icons");

    /** Glyph height of the vanilla font in GUI pixels. */
    public static final int TEXT_HEIGHT = 8;
    /** Vanilla line box. */
    public static final int LINE_HEIGHT = 9;

    // Font Awesome solid codepoints used by the dashboard (verified present in
    // fa-solid-900.ttf 6.5.2 via Java AWT canDisplay).
    public static final int ICON_GAUGE = 0xF625;          // fa-gauge
    public static final int ICON_EYE = 0xF06E;            // fa-eye
    public static final int ICON_SCREWDRIVER = 0xF7D9;    // fa-screwdriver-wrench
    public static final int ICON_SLIDERS = 0xF1DE;        // fa-sliders
    public static final int ICON_SEARCH = 0xF002;         // fa-magnifying-glass
    public static final int ICON_SHIELD = 0xF3ED;         // fa-shield-halved
    public static final int ICON_CHECK = 0xF058;          // fa-circle-check
    public static final int ICON_XMARK = 0xF00D;          // fa-xmark
    public static final int ICON_GEAR = 0xF013;           // fa-gear
    public static final int ICON_CHEVRON = 0xF078;        // fa-chevron-down
    public static final int ICON_CHEVRON_UP = 0xF077;     // fa-chevron-up
    public static final int ICON_CHEVRON_LEFT = 0xF053;   // fa-chevron-left
    public static final int ICON_CHEVRON_RIGHT = 0xF054;  // fa-chevron-right
    public static final int ICON_SHIRT = 0xF553;          // fa-shirt
    public static final int ICON_STAR = 0xF005;          // fa-star

    private UiFonts() {
    }

    /** Vanilla-glyph component, bold uses the game's own bold pass. */
    public static Component component(String text, boolean bold) {
        return Component.literal(text).withStyle(Style.EMPTY.withBold(bold)
                .withFont(new FontDescription.Resource(UI)));
    }

    /** Pre-shaped sequence ready for GuiGraphicsExtractor.text(). */
    public static FormattedCharSequence seq(String text, boolean bold) {
        return component(text, bold).getVisualOrderText();
    }

    /** Width of a text string in GUI pixels (vanilla metrics). */
    public static int width(String text, boolean bold) {
        return Minecraft.getInstance().font.width(component(text, bold));
    }

    /** Component that renders a single Font Awesome icon glyph. */
    public static Component iconComponent(int codepoint) {
        return Component.literal(new String(Character.toChars(codepoint)))
                .withStyle(Style.EMPTY.withFont(new FontDescription.Resource(ICONS)));
    }

    /** Pre-shaped icon sequence ready for GuiGraphicsExtractor.text(). */
    public static FormattedCharSequence iconSeq(int codepoint) {
        return iconComponent(codepoint).getVisualOrderText();
    }

    /** Width of an icon glyph in GUI pixels. */
    public static int iconWidth(int codepoint) {
        return Minecraft.getInstance().font.width(iconComponent(codepoint));
    }
}
