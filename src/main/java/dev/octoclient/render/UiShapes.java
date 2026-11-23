package dev.octoclient.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * Signed-distance shapes for the dashboard chrome.
 *
 * <p>Every pill, track and knob used to be a stack of 1px rounded-rect fills in
 * virtual coordinates. The dashboard is scaled by a fractional factor, so those
 * edges stair-step and the toggle switches read as pixel art. This pipeline
 * draws the same rounded box from an SDF in the fragment stage: the outline is
 * antialiased at device resolution and stays smooth at any GUI scale.</p>
 *
 * <p>It only carries a fill colour; borders are drawn as a slightly larger
 * shape behind the fill, which keeps the AA perfect and needs no second colour
 * channel in the vertex stream. Like {@link LiquidGlass}, a failure disables
 * the path and callers fall back to the pixel-snapped rectangles.</p>
 */
public final class UiShapes {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("octoclient", "core/ui_shape");

    private static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("Normal", GpuFormat.RGBA8_SNORM)
            .build();

    private static RenderPipeline pipeline;
    private static boolean failed;
    private static boolean announced;

    private UiShapes() {
    }

    /** True while the shape pipeline is healthy. */
    public static boolean isAvailable() {
        return !failed;
    }

    private static RenderPipeline pipeline() {
        if (pipeline == null) {
            pipeline = RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                            .withLocation(Identifier.fromNamespaceAndPath("octoclient", "pipeline/ui_shape"))
                            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                            .withVertexShader(SHADER)
                            .withFragmentShader(SHADER)
                            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                            .withVertexBinding(0, FORMAT)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .build());
        }
        return pipeline;
    }

    /**
     * Queues one antialiased rounded rectangle.
     *
     * @param radius corner radius in GUI pixels
     * @param argb   fill colour, alpha included
     * @return false when the caller should fall back to a plain rectangle fill
     */
    public static boolean draw(GuiGraphicsExtractor gfx, float x, float y, float w, float h,
                               float radius, int argb) {
        if (failed || gfx == null || w < 0.5f || h < 0.5f) {
            return false;
        }
        try {
            RenderPipeline pipe = pipeline();
            float radiusFrac = radius / Math.max(1f, Math.min(w, h));
            ScreenRectangle scissor = gfx.scissorStack.peek();
            Matrix3x2f pose = new Matrix3x2f(gfx.pose());
            ScreenRectangle rect = boundsOf(x, y, w, h, pose);
            // A shape that is scrolled out of its scissor must not reach the
            // renderer at all: it would carry an empty scissor area and the
            // scissor call rejects 0x0.
            if (scissor != null && scissor.intersection(rect) == null) {
                return true;
            }
            gfx.guiRenderState.addGuiElement(new Shape(pipe, pose,
                    x, y, x + w, y + h, argb, radiusFrac, scissor));
            if (!announced) {
                announced = true;
                System.out.println("[OctoClient] SDF shapes active");
            }
            return true;
        } catch (Throwable t) {
            failed = true;
            System.err.println("[OctoClient] SDF shapes disabled: " + t);
            return false;
        }
    }

    private record Shape(RenderPipeline pipeline, Matrix3x2fc pose,
                         float x0, float y0, float x1, float y1, int argb,
                         float radiusFrac, ScreenRectangle scissorArea) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            vertex(consumer, x0, y0, 0.0F, 0.0F);
            vertex(consumer, x0, y1, 0.0F, 1.0F);
            vertex(consumer, x1, y1, 1.0F, 1.0F);
            vertex(consumer, x1, y0, 1.0F, 0.0F);
        }

        private void vertex(VertexConsumer consumer, float px, float py, float u, float v) {
            consumer.addVertexWith2DPose(pose, px, py)
                    .setColor(argb)
                    .setUv(u, v)
                    .setNormal(radiusFrac, 0.0F, 0.0F);
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public ScreenRectangle bounds() {
            ScreenRectangle rect = boundsOf(x0, y0, x1 - x0, y1 - y0, pose);
            if (scissorArea == null) {
                return rect;
            }
            ScreenRectangle clipped = scissorArea.intersection(rect);
            // Never hand back an empty rectangle: the renderer cannot build a
            // scissor from it. draw() already dropped fully clipped shapes.
            return clipped != null ? clipped : rect;
        }
    }

    /** Screen-space bounds of a float design rectangle under the GUI pose. */
    private static ScreenRectangle boundsOf(float x, float y, float w, float h, Matrix3x2fc pose) {
        ScreenRectangle rect = new ScreenRectangle((int) Math.floor(x), (int) Math.floor(y),
                Math.max(1, (int) Math.ceil(w)), Math.max(1, (int) Math.ceil(h)));
        return rect.transformMaxBounds(pose);
    }
}
