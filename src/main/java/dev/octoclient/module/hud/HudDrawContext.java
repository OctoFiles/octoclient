package dev.octoclient.module.hud;

import dev.octoclient.util.UiFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Thin wrapper over the vanilla GUI extractor that HUD modules draw through.
 * Keeps all Minecraft API usage in one place so modules stay readable.
 */
public final class HudDrawContext {
    public final GuiGraphicsExtractor gfx;
    public final Minecraft mc;
    public final float tickDelta;
    public final int screenWidth;
    public final int screenHeight;

    public HudDrawContext(GuiGraphicsExtractor gfx, Minecraft mc, float tickDelta, int screenWidth, int screenHeight) {
        this.gfx = gfx;
        this.mc = mc;
        this.tickDelta = tickDelta;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public Font font() {
        return mc.font;
    }

    public LocalPlayer player() {
        return mc.player;
    }

    // ------------------------------------------------------------------ text

    public int drawText(String text, float x, float y, int argb) {
        return drawText(text, x, y, argb, true);
    }

    public int drawText(String text, float x, float y, int argb, boolean shadow) {
        gfx.text(font(), UiFonts.seq(text, false), (int) x, (int) y, argb, shadow);
        return font().width(UiFonts.component(text, false));
    }

    public void drawTextScaled(String text, float x, float y, int argb, float scale, boolean shadow) {
        if (scale <= 0.01f) return;
        if (Math.abs(scale - 1.0f) < 0.001f) {
            drawText(text, x, y, argb, shadow);
            return;
        }
        org.joml.Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        gfx.text(font(), UiFonts.seq(text, false), (int) (x / scale), (int) (y / scale), argb, shadow);
        pose.popMatrix();
    }

    public void drawText(Component text, float x, float y, int argb, boolean shadow) {
        gfx.text(font(), text, (int) x, (int) y, argb, shadow);
    }

    public int textWidth(String text) {
        return font().width(UiFonts.component(text, false));
    }

    /**
     * Line box height in GUI px: the vanilla 8 px glyph cell plus its one-pixel
     * descender row, which is {@link UiFonts#LINE_HEIGHT}. Kept as a helper so HUD
     * modules never hardcode the 9.
     */
    public int textHeight() {
        return UiFonts.LINE_HEIGHT;
    }

    // ---------------------------------------------------------------- shapes

    public void fill(float x1, float y1, float x2, float y2, int argb) {
        gfx.fill((int) x1, (int) y1, (int) x2, (int) y2, argb);
    }

    /**
     * Rectangle outline (1px border). The vanilla outline() takes
     * (x, y, width, height) — convert the x2/y2 coordinates here so
     * modules can keep expressing rectangles as corner pairs.
     */
    public void outline(float x1, float y1, float x2, float y2, int argb) {
        gfx.outline((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1), argb);
    }

    public void fillGradient(float x1, float y1, float x2, float y2, int fromArgb, int toArgb) {
        gfx.fillGradient((int) x1, (int) y1, (int) x2, (int) y2, fromArgb, toArgb);
    }

    public void enableScissor(float x1, float y1, float x2, float y2) {
        gfx.enableScissor((int) x1, (int) y1, (int) x2, (int) y2);
    }

    public void disableScissor() {
        gfx.disableScissor();
    }

    // ----------------------------------------------------------------- items

    public void drawItem(ItemStack stack, float x, float y) {
        if (stack.isEmpty()) {
            return;
        }
        gfx.item(stack, (int) x, (int) y);
    }

    /** Item icon + vanilla count / durability decorations. */
    public void drawItemDecorations(ItemStack stack, float x, float y) {
        if (stack.isEmpty()) {
            return;
        }
        gfx.itemDecorations(font(), stack, (int) x, (int) y);
    }

    public ItemStack getArmor(EquipmentSlot slot) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return player.getItemBySlot(slot);
    }

    public ItemStack getOffhand() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return player.getOffhandItem();
    }

    public ItemStack getMainHand() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return player.getMainHandItem();
    }

    // ---------------------------------------------------------------- effects

    /**
     * The vanilla effect icon (the little art the inventory shows), not a
     * generic potion bottle. Uses the same sprite lookup as Mojang's own
     * effect list so ambience / new effects all look right.
     */
    public void drawEffectIcon(Holder<MobEffect> effect, float x, float y, float size) {
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect),
                (int) x, (int) y, (int) size, (int) size);
    }
}
