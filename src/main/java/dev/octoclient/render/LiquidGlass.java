package dev.octoclient.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

/**
 * Real refractive glass for the client GUI.
 *
 * <p>Once per frame the world framebuffer is copied into an off-screen texture.
 * Panels are then queued into the batched GUI renderer with a custom pipeline
 * that samples that copy and bends it along the panel rim, which is what makes
 * the sheet read as a thick, liquid lens instead of a flat tint.
 *
 * <p>Every entry point is guarded: if the pipeline, the copy or the texture
 * allocation fails for any reason the whole feature disables itself and the
 * callers fall back to the layered glass they had before.
 */
public final class LiquidGlass {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("octoclient", "core/liquid_glass");
    private static final Identifier SATURATION_SHADER =
            Identifier.fromNamespaceAndPath("octoclient", "core/color_saturation");
    /** Clear variant: same optics without the smoked body, for the module cards. */
    private static final Identifier SHADER_CLEAR = Identifier.fromNamespaceAndPath("octoclient", "core/liquid_glass_clear");
    /** Dark frosted variant: heavy frost behind a matte graphite pane. */
    private static final Identifier SHADER_FROSTED =
            Identifier.fromNamespaceAndPath("octoclient", "core/liquid_glass_frosted");
    /** Style selector for {@link #draw(GuiGraphicsExtractor, int, int, int, int, float, float, float, int, int, int)}. */
    private static final int MODE_SHELL = 0;
    private static final int MODE_CLEAR = 1;
    private static final int MODE_FROSTED = 2;
    /** Whole-window frost used as the dashboard backdrop. */
    private static final Identifier SHADER_BLUR = Identifier.fromNamespaceAndPath("octoclient", "core/blur");

    private static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("Normal", GpuFormat.RGBA8_SNORM)
            .build();

    private static RenderPipeline pipeline;
    private static RenderPipeline clearPipeline;
    private static RenderPipeline frostedPipeline;
    private static RenderPipeline saturationPipeline;
    private static RenderPipeline blurPipeline;
    private static TextureTarget backdrop;
    private static GpuFormat backdropFormat;

    private static boolean failed;
    private static boolean requested;
    private static boolean ready;
    private static boolean announced;
    private static boolean saturationFailed;
    private static boolean blurFailed;

    private LiquidGlass() {
    }

    /** True while the refractive path is healthy and a backdrop is available. */
    public static boolean isAvailable() {
        return !failed && ready;
    }

    private static RenderPipeline pipeline(int mode) {
        if (mode == MODE_FROSTED) {
            if (frostedPipeline == null) {
                frostedPipeline = build("pipeline/liquid_glass_frosted", SHADER_FROSTED);
            }
            return frostedPipeline;
        }
        if (mode == MODE_CLEAR) {
            if (clearPipeline == null) {
                clearPipeline = build("pipeline/liquid_glass_clear", SHADER_CLEAR);
            }
            return clearPipeline;
        }
        if (pipeline == null) {
            pipeline = build("pipeline/liquid_glass", SHADER);
        }
        return pipeline;
    }

    private static RenderPipeline build(String location, Identifier shader) {
        return RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("octoclient", location))
                        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withVertexShader(SHADER)
                        .withFragmentShader(shader)
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withVertexBinding(0, FORMAT)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .build());
    }

    /**
     * Queues one glass sheet. Returns false when the caller should draw its own
     * fallback instead.
     *
     * @param radius  corner radius in GUI pixels
     * @param opacity sheet opacity, 0..1
     * @param hover   pointer hover amount, 0..1
     * @param tintRgb body tint, 0xRRGGBB
     * @param borderAlpha strength of the 1px rim, 0..255
     */
    public static boolean draw(GuiGraphicsExtractor gfx, int x, int y, int width, int height,
                               float radius, float opacity, float hover, int tintRgb, int borderAlpha) {
        return draw(gfx, x, y, width, height, radius, opacity, hover, tintRgb, borderAlpha, false);
    }

    /** Queues a full-screen saturation pass using the same frame-safe backdrop copy. */
    public static void drawSaturation(GuiGraphicsExtractor gfx, float saturation) {
        drawSaturation(gfx, 0f, saturation, 1f, 1f);
    }

    /** Queues a full-screen hue, saturation, brightness, and contrast pass. */
    public static void drawSaturation(GuiGraphicsExtractor gfx, float hue, float saturation,
                                      float brightness, float contrast) {
        if (saturationFailed || failed || gfx == null) {
            return;
        }
        requested = true;
        if (!ready) {
            return;
        }
        try {
            if (saturationPipeline == null) {
                saturationPipeline = build("pipeline/color_saturation", SATURATION_SHADER);
            }
            TextureSetup setup = TextureSetup.singleTexture(
                    backdrop.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            int width = gfx.guiWidth();
            int height = gfx.guiHeight();
            ScreenRectangle scissor = gfx.scissorStack.peek();
            org.joml.Matrix3x2f pose = new org.joml.Matrix3x2f(gfx.pose());
            int contrastByte = Math.round(Math.max(0f, Math.min(2f, contrast)) * 127.5f);
            gfx.guiRenderState.addGuiElement(new Panel(saturationPipeline, setup, pose,
                    0, 0, width, height, 0xFFFFFF, contrastByte,
                    Math.max(0f, Math.min(1f, hue)), saturation, brightness, scissor));
            requested = true;
        } catch (RuntimeException e) {
            saturationFailed = true;
            System.err.println("[OctoClient] color saturation disabled: " + e);
        }
    }

    /**
     * Queues a whole-window frost behind the GUI, Blur-mod style: the already
     * rendered scene is blurred by sampling the frame copy in a two-ring disc.
     * The quad spans the window exactly, so the sheet has square corners.
     *
     * @param radiusPx blur radius as a fraction of the window height, so the
     *                 frost keeps the same strength at every resolution
     * @param dim      0..1 darkening applied on top of the frost
     * @return false while no backdrop copy exists, so the caller can fall back
     */
    public static boolean drawBlur(GuiGraphicsExtractor gfx, float radiusPx, float dim) {
        if (blurFailed || failed || gfx == null) {
            return false;
        }
        requested = true;
        if (!ready) {
            return false;
        }
        try {
            if (blurPipeline == null) {
                blurPipeline = build("pipeline/blur", SHADER_BLUR);
            }
            TextureSetup setup = TextureSetup.singleTexture(
                    backdrop.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            int width = gfx.guiWidth();
            int height = gfx.guiHeight();
            ScreenRectangle scissor = gfx.scissorStack.peek();
            org.joml.Matrix3x2f pose = new org.joml.Matrix3x2f(gfx.pose());
            // The Normal channel that carries the corner radius for glass
            // sheets carries the blur radius here, as a height fraction.
            gfx.guiRenderState.addGuiElement(new Panel(blurPipeline, setup, pose,
                    0, 0, width, height, 0xFFFFFF, 0, radiusPx,
                    Math.max(0f, Math.min(1f, dim)), 0f, scissor));
            requested = true;
            return true;
        } catch (Throwable t) {
            blurFailed = true;
            System.err.println("[OctoClient] screen blur disabled: " + t);
            return false;
        }
    }

    /**
     * Same as {@link #draw(GuiGraphicsExtractor, int, int, int, int, float, float, float, int, int)},
     * but {@code clear} swaps the smoked body for the clear-liquid variant: the
     * sheet then shows the scene through it instead of lifting every colour
     * toward grey.
     */
    public static boolean draw(GuiGraphicsExtractor gfx, int x, int y, int width, int height,
                               float radius, float opacity, float hover, int tintRgb, int borderAlpha,
                               boolean clear) {
        return draw(gfx, x, y, width, height, radius, opacity, hover, tintRgb, borderAlpha,
                clear ? MODE_CLEAR : MODE_SHELL);
    }

    /**
     * Dark frosted glass ("Dark Glassmorphism"): a matte graphite pane over a
     * heavy backdrop frost. Same rim and corner treatment as the other sheets,
     * but no smoked lift and no specular glint, so it stays calm and milky
     * instead of shiny.
     */
    public static boolean drawFrosted(GuiGraphicsExtractor gfx, int x, int y, int width, int height,
                                      float radius, float opacity, float hover, int tintRgb, int borderAlpha) {
        return draw(gfx, x, y, width, height, radius, opacity, hover, tintRgb, borderAlpha, MODE_FROSTED);
    }

    private static boolean draw(GuiGraphicsExtractor gfx, int x, int y, int width, int height,
                                float radius, float opacity, float hover, int tintRgb, int borderAlpha,
                                int mode) {
        if (failed || gfx == null || width < 2 || height < 2) {
            return false;
        }

        // Ask for a backdrop even while the first one is still missing, otherwise
        // the capture hook would never run and the fallback would stick forever.
        requested = true;
        if (!ready) {
            return false;
        }

        try {
            RenderPipeline pipe = pipeline(mode);
            TextureSetup setup = TextureSetup.singleTexture(
                    backdrop.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

            int x1 = x + width;
            int y1 = y + height;
            float radiusFrac = radius / (float) Math.max(1, Math.min(width, height));
            ScreenRectangle scissor = gfx.scissorStack.peek();
            org.joml.Matrix3x2f pose = new org.joml.Matrix3x2f(gfx.pose());
            ScreenRectangle rect = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
            // Drop fully clipped sheets: an empty scissor area would make the
            // renderer reject the 0x0 rectangle.
            if (scissor != null && scissor.intersection(rect) == null) {
                return true;
            }

            gfx.guiRenderState.addGuiElement(new Panel(pipe, setup, pose,
                    x, y, x1, y1, tintRgb, borderAlpha, radiusFrac, opacity, hover, scissor));
            requested = true;
            return true;
        } catch (Throwable t) {
            failed = true;
            System.err.println("[OctoClient] liquid glass disabled: " + t);
            return false;
        }
    }

    /**
     * Called from the render loop right before the batched GUI pass executes.
     * The world has already been drawn into the main target at this point and
     * no render pass is open, so a plain texture copy is legal here.
     */
    public static void captureBackdrop() {
        if (failed || !requested) {
            return;
        }
        requested = false;

        try {
            Minecraft minecraft = Minecraft.getInstance();
            RenderTarget main = minecraft.gameRenderer.mainRenderTarget();
            if (main == null || main.getColorTexture() == null) {
                ready = false;
                return;
            }

            int width = main.width;
            int height = main.height;
            if (width <= 0 || height <= 0) {
                ready = false;
                return;
            }

            GpuFormat format = main.getColorTexture().getFormat();
            if (backdrop == null || backdropFormat != format) {
                if (backdrop != null) {
                    backdrop.destroyBuffers();
                }
                backdrop = new TextureTarget("octoclient liquid glass backdrop", width, height, false, format);
                backdropFormat = format;
            } else if (backdrop.width != width || backdrop.height != height) {
                backdrop.resize(width, height);
            }

            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    main.getColorTexture(), backdrop.getColorTexture(),
                    0, 0, 0, 0, 0, width, height);

            ready = true;
            if (!announced) {
                announced = true;
                System.out.println("[OctoClient] liquid glass active (" + width + "x" + height + ")");
            }
        } catch (Throwable t) {
            failed = true;
            ready = false;
            System.err.println("[OctoClient] liquid glass backdrop failed: " + t);
            if (backdrop != null) {
                try {
                    backdrop.destroyBuffers();
                } catch (Throwable ignored) {
                }
                backdrop = null;
            }
        }
    }

    private record Panel(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                         int x0, int y0, int x1, int y1, int tint, int borderAlpha,
                         float radiusFrac, float opacity, float hover,
                         ScreenRectangle scissorArea) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            vertex(consumer, x0, y0, 0.0F, 0.0F);
            vertex(consumer, x0, y1, 0.0F, 1.0F);
            vertex(consumer, x1, y1, 1.0F, 1.0F);
            vertex(consumer, x1, y0, 1.0F, 0.0F);
        }

        private void vertex(VertexConsumer consumer, float px, float py, float u, float v) {
            consumer.addVertexWith2DPose(pose, px, py)
                    .setColor((borderAlpha << 24) | (tint & 0xFFFFFF))
                    .setUv(u, v)
                    .setNormal(radiusFrac, opacity, hover);
        }

        @Override
        public ScreenRectangle bounds() {
            ScreenRectangle rect = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            if (scissorArea == null) {
                return rect;
            }
            ScreenRectangle clipped = scissorArea.intersection(rect);
            // Never return an empty rectangle; draw() already skipped fully
            // clipped sheets, and the renderer cannot enable a 0x0 scissor.
            return clipped != null ? clipped : rect;
        }
    }
}
