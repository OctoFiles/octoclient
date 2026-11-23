package dev.octoclient.mixin;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * FontTexture hardcodes FilterMode.NEAREST for its glyph sheets and allocates
 * them with a single mip level. NEAREST is exactly right for the vanilla pixel
 * font, but the Font Awesome icon glyphs in the OctoClient sheet are TTF glyphs
 * rasterized with oversample, and they show stair-stepping at the fractional
 * GUI scales this UI renders at.
 * <p>
 * Filtering alone cannot fix that at <i>every</i> scale: the dashboard
 * minifies glyphs (small window) while the HUD magnifies them at guiScale, and
 * one filter mode cannot do both. So our own sheets get a full mip chain and a
 * trilinear sampler instead:
 * <ul>
 *   <li>{@code createTexture} receives 9 mip levels (256..1) instead of 1, only
 *       for sheets whose name starts with {@code octoclient:}.</li>
 *   <li>The sampler becomes LINEAR with an <i>unlimited</i> max LOD, so
 *       {@code GL_LINEAR_MIPMAP_LINEAR} actually walks the chain instead of
 *       being clamped back to level 0 - which is what {@code getRepeat} does by
 *       passing {@code maxLod = 0}.</li>
 *   <li>The sheet is zero-filled once on construction, then mipmaps are
 *       regenerated after every glyph upload, so generated levels never average
 *       in undefined texels from the unused part of the atlas.</li>
 * </ul>
 * Vanilla sheets are untouched: the name check keeps their sampler and their
 * single mip level exactly as Mojang intended.
 * <p>
 * The HEAD handler must be static (it runs before super()); it records whether
 * this sheet belongs to the octoclient namespace in a thread-local, which the
 * constructor-body injects read. {@link #octoclient$ownSheet} captures the same
 * fact per instance, because the thread-local is long gone by draw time.
 */
@Mixin(FontTexture.class)
public class FontTextureMixin {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("octoclient-font");

    /** Mirrors the sheet size FontTexture itself hardcodes. */
    @Unique
    private static final int OCTOCLIENT_SHEET_SIZE = 256;

    /** floor(log2(256)) + 1 - the complete chain down to 1x1. */
    @Unique
    private static final int OCTOCLIENT_MIP_LEVELS = 9;

    /**
     * Name of the glyph sheet currently being constructed on this thread, or
     * {@code null} when the sheet does not belong to OctoClient.
     */
    @Unique
    private static final ThreadLocal<String> OCTOCLIENT_SHEET = ThreadLocal.withInitial(() -> null);

    /** Captured at construction; {@link #OCTOCLIENT_SHEET} is stale afterwards. */
    @Unique
    private boolean octoclient$ownSheet;

    @Inject(method = "<init>", at = @At("HEAD"))
    private static void octoclient$markOwnSheet(Supplier<String> name, GlyphRenderTypes renderTypes, boolean colored, CallbackInfo ci) {
        String sheet = name.get();
        OCTOCLIENT_SHEET.set(sheet != null && sheet.startsWith("octoclient:") ? sheet : null);
    }

    /**
     * Hand {@code createTexture} a full mip chain instead of a single level.
     * Argument 6 is {@code mipLevels} (after usage, format, width, height and
     * depth) - GlDevice allocates every level it is given and sets
     * GL_TEXTURE_MAX_LEVEL accordingly.
     */
    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/GpuFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;"
        ),
        index = 6
    )
    private int octoclient$allocateMipChain(int mipLevels) {
        return OCTOCLIENT_SHEET.get() == null ? mipLevels : OCTOCLIENT_MIP_LEVELS;
    }

    /**
     * SamplerCache.getRepeat always passes {@code maxLod = 0} to getSampler,
     * which pins the sampler to mip level 0 even though the min filter is
     * already GL_LINEAR_MIPMAP_LINEAR. That is invisible while the texture has
     * one level, but it would silently disable the chain we just allocated -
     * so the whole call is replaced for our sheets.
     */
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/SamplerCache;getRepeat(Lcom/mojang/blaze3d/textures/FilterMode;)Lcom/mojang/blaze3d/textures/GpuSampler;"
        )
    )
    private GpuSampler octoclient$trilinearOwnSheet(SamplerCache cache, FilterMode mode) {
        String sheet = OCTOCLIENT_SHEET.get();
        if (sheet == null) {
            return cache.getRepeat(mode);
        }
        LOGGER.info("[OctoClient] Glyph sheet '{}' uses LINEAR filtering with {} mip levels", sheet, OCTOCLIENT_MIP_LEVELS);
        return cache.getSampler(
            AddressMode.REPEAT,
            AddressMode.REPEAT,
            FilterMode.LINEAR,
            FilterMode.LINEAR,
            true
        );
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void octoclient$rememberOwnSheet(CallbackInfo ci) {
        this.octoclient$ownSheet = OCTOCLIENT_SHEET.get() != null;
        if (!this.octoclient$ownSheet || !RenderSystem.isOnRenderThread()) {
            return;
        }
        // glTexImage2D left the texels undefined; zero them once so mipmap
        // generation never averages garbage in from the unused atlas area.
        octoclient$zeroSheet();
        octoclient$generateMipmaps();
    }

    @Inject(method = "add", at = @At("RETURN"))
    private void octoclient$regenerateMipmaps(
        GlyphInfo info,
        GlyphBitmap bitmap,
        CallbackInfoReturnable<BakedSheetGlyph> cir
    ) {
        if (!this.octoclient$ownSheet || !RenderSystem.isOnRenderThread()) {
            return;
        }
        octoclient$generateMipmaps();
    }

    @Unique
    private void octoclient$zeroSheet() {
        GlTexture glTexture = octoclient$glTexture();
        if (glTexture == null) {
            return;
        }
        boolean colored = glTexture.getFormat() == GpuFormat.RGBA8_UNORM;
        ByteBuffer clear = ByteBuffer.allocateDirect(
            OCTOCLIENT_SHEET_SIZE * OCTOCLIENT_SHEET_SIZE * (colored ? 4 : 1)
        );
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        // Route the bind through GlStateManager so its per-unit cache stays in
        // sync; a raw glBindTexture here would desync every later draw.
        GlStateManager._bindTexture(glTexture.glId());
        try {
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                OCTOCLIENT_SHEET_SIZE,
                OCTOCLIENT_SHEET_SIZE,
                colored ? GL11.GL_RGBA : GL11.GL_RED,
                GL11.GL_UNSIGNED_BYTE,
                clear
            );
        } finally {
            GlStateManager._bindTexture(previous);
        }
    }

    @Unique
    private void octoclient$generateMipmaps() {
        GlTexture glTexture = octoclient$glTexture();
        if (glTexture == null) {
            return;
        }
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._bindTexture(glTexture.glId());
        try {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        } finally {
            GlStateManager._bindTexture(previous);
        }
    }

    @Unique
    private GlTexture octoclient$glTexture() {
        GpuTexture texture = ((FontTexture) (Object) this).getTexture();
        return texture instanceof GlTexture glTexture ? glTexture : null;
    }
}
