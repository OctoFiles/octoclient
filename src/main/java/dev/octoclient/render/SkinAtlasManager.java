package dev.octoclient.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.octoclient.module.render.BadOptimization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Packs ready player-skin textures into one GPU atlas and supplies per-skin UV sprites. */
public final class SkinAtlasManager {
    private static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath("octoclient", "textures/skin_atlas");
    private static final int CELL_SIZE = 64;
    private static final int ATLAS_SIZE = 2048;
    private static final int CELLS_PER_ROW = ATLAS_SIZE / CELL_SIZE;
    private static final Map<Identifier, TextureAtlasSprite> SPRITES = new HashMap<>();
    private static DynamicTexture atlas;
    private static SpriteContents spriteContents;
    private static int nextSlot;
    private static boolean failed;

    private SkinAtlasManager() {
    }

    public static Identifier texture(Identifier skinTexture) {
        return sprite(skinTexture) == null ? null : ATLAS_ID;
    }

    public static TextureAtlasSprite sprite(Identifier skinTexture) {
        if (!BadOptimization.usesSkinAtlas() || failed || !RenderSystem.isOnRenderThread()) {
            return null;
        }
        TextureAtlasSprite existing = SPRITES.get(skinTexture);
        if (existing != null) {
            return existing;
        }
        if (nextSlot >= CELLS_PER_ROW * CELLS_PER_ROW) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        if (atlas == null) {
            createAtlas(client);
        }
        if (failed || atlas == null) {
            return null;
        }

        if (!(client.getTextureManager().getTexture(skinTexture) instanceof DynamicTexture source)
                || source.getTexture() == null
                || source.getPixels() == null
                || source.getPixels().getWidth() != CELL_SIZE
                || source.getPixels().getHeight() != CELL_SIZE) {
            return null;
        }

        int slot = nextSlot;
        int x = slot % CELLS_PER_ROW * CELL_SIZE;
        int y = slot / CELLS_PER_ROW * CELL_SIZE;
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    source.getTexture(), atlas.getTexture(), 0, 0, 0, x, y, CELL_SIZE, CELL_SIZE);
        } catch (RuntimeException error) {
            failed = true;
            System.err.println("[OctoClient] Skin atlas disabled after a texture-copy failure: " + error);
            return null;
        }

        TextureAtlasSprite sprite = new AtlasSprite(x, y);
        SPRITES.put(skinTexture, sprite);
        nextSlot++;
        return sprite;
    }

    private static void createAtlas(Minecraft client) {
        NativeImage pixels = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        pixels.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE, 0);
        atlas = new DynamicTexture(() -> "OctoClient skin atlas", pixels);
        client.getTextureManager().register(ATLAS_ID, atlas);

        NativeImage spriteImage = new NativeImage(NativeImage.Format.RGBA, CELL_SIZE, CELL_SIZE, false);
        spriteImage.fillRect(0, 0, CELL_SIZE, CELL_SIZE, 0);
        spriteContents = new SpriteContents(ATLAS_ID, new FrameSize(CELL_SIZE, CELL_SIZE), spriteImage);
    }

    private static final class AtlasSprite extends TextureAtlasSprite {
        private AtlasSprite(int x, int y) {
            super(ATLAS_ID, spriteContents, x, y, ATLAS_SIZE, ATLAS_SIZE, 0);
        }
    }
}
