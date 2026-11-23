package dev.octoclient.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.cosmetics.Cape;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Loads bundled GIF capes into client-side textures and advances their frames. */
public final class AnimatedCapeTextures {
    private static final int MAX_FRAME_DIMENSION = 256;
    /** Dev-only: log every GIF frame advance so playback can be verified in a smoke test. */
    private static final boolean DEBUG = Boolean.getBoolean("octoclient.debugCapeAnim");
    private static boolean initialized;
    private static boolean texturesLoaded;
    private static final List<AnimatedCape> capeTextures = new ArrayList<>();

    private AnimatedCapeTextures() {
    }

    public static Identifier textureId(String fileName) {
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".gif")) {
            String baseName = fileName.substring(0, fileName.length() - 4);
            return Identifier.fromNamespaceAndPath("octoclient", "dynamic/capes/" + baseName);
        }
        return Identifier.fromNamespaceAndPath("octoclient", "textures/cosmetic/capes/" + fileName);
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!texturesLoaded) {
                loadAnimatedTextures(client);
            }
            // Advance every loaded clip, including previews and remote players.
            // Gating this on the current wearer made a cape resume from a stale
            // frame whenever it became visible, which read as "not animated".
            for (AnimatedCape animatedCape : capeTextures) {
                animatedCape.tick(client);
            }
        });
    }

    private static void loadAnimatedTextures(Minecraft client) {
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof Cape cape && cape.isAnimated()) {
                try {
                    AnimatedCape texture = load(cape.fileName(), cape.texture());
                    client.getTextureManager().register(cape.texture(), texture.texture);
                    capeTextures.add(texture);
                    if (DEBUG) {
                        System.out.printf("[OctoClient] cape animation %s: %d frames%n",
                                cape.fileName(), texture.frames.size());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load cape animation " + cape.fileName(), e);
                }
            }
        }
        texturesLoaded = true;
    }

    private static AnimatedCape load(String fileName, Identifier id) throws IOException {
        String resource = "/assets/octoclient/textures/cosmetic/capes/" + fileName;
        try (InputStream stream = AnimatedCapeTextures.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource " + resource);
            }
            try (ImageInputStream input = ImageIO.createImageInputStream(stream)) {
                if (input == null) {
                    throw new IOException("Could not read GIF stream " + resource);
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new IOException("No GIF reader for " + resource);
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, false, false);
                    List<Frame> frames = decode(reader, resource);
                    NativeImage firstFrame = toNativeImage(frames.getFirst());
                    DynamicTexture texture = new DynamicTexture(() -> "OctoClient " + fileName, firstFrame);
                    return new AnimatedCape(fileName, id, texture, frames);
                } finally {
                    reader.dispose();
                }
            }
        }
    }

    private static List<Frame> decode(ImageReader reader, String resource) throws IOException {
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        int frameCount = reader.getNumImages(true);
        if (width <= 0 || height <= 0 || frameCount <= 0) {
            throw new IOException("Invalid GIF dimensions or frame count for " + resource);
        }

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        List<Frame> frames = new ArrayList<>(frameCount);
        BufferedImage restorePrevious = null;
        String previousDisposal = "none";
        int previousLeft = 0;
        int previousTop = 0;
        int previousWidth = 0;
        int previousHeight = 0;

        try {
            for (int i = 0; i < frameCount; i++) {
                if (previousDisposal.equals("restoreToBackgroundColor")) {
                    graphics.setComposite(AlphaComposite.Clear);
                    graphics.fillRect(previousLeft, previousTop, previousWidth, previousHeight);
                    graphics.setComposite(AlphaComposite.SrcOver);
                } else if (previousDisposal.equals("restoreToPrevious") && restorePrevious != null) {
                    graphics.setComposite(AlphaComposite.Src);
                    graphics.drawImage(restorePrevious, 0, 0, null);
                    graphics.setComposite(AlphaComposite.SrcOver);
                }

                IIOMetadata metadata = reader.getImageMetadata(i);
                org.w3c.dom.Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
                org.w3c.dom.Node descriptor = child(root, "ImageDescriptor");
                int left = intAttribute(descriptor, "imageLeftPosition", 0);
                int top = intAttribute(descriptor, "imageTopPosition", 0);
                BufferedImage image = reader.read(i);
                String disposal = stringAttribute(child(root, "GraphicControlExtension"), "disposalMethod", "none");
                if (disposal.equals("restoreToPrevious")) {
                    restorePrevious = copy(canvas);
                } else {
                    restorePrevious = null;
                }

                graphics.drawImage(image, left, top, null);
                BufferedImage scaled = scale(canvas);
                int delay = intAttribute(child(root, "GraphicControlExtension"), "delayTime", 10) * 10;
                frames.add(new Frame(scaled.getWidth(), scaled.getHeight(),
                        scaled.getRGB(0, 0, scaled.getWidth(), scaled.getHeight(), null, 0, scaled.getWidth()),
                        Math.max(20, delay)));
                previousDisposal = disposal;
                previousLeft = left;
                previousTop = top;
                previousWidth = image.getWidth();
                previousHeight = image.getHeight();
            }
        } finally {
            graphics.dispose();
        }
        return frames;
    }

    private static BufferedImage scale(BufferedImage source) {
        float scale = Math.min(1f, (float) MAX_FRAME_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static NativeImage toNativeImage(Frame frame) {
        NativeImage image = new NativeImage(frame.width, frame.height, false);
        for (int y = 0; y < frame.height; y++) {
            for (int x = 0; x < frame.width; x++) {
                int argb = frame.pixels[y * frame.width + x];
                int abgr = (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >>> 16) & 0xFF);
                image.setPixelABGR(x, y, abgr);
            }
        }
        return image;
    }

    private static org.w3c.dom.Node child(org.w3c.dom.Node node, String name) {
        if (node == null) {
            return null;
        }
        for (org.w3c.dom.Node current = node.getFirstChild(); current != null; current = current.getNextSibling()) {
            if (current.getNodeName().equals(name)) {
                return current;
            }
        }
        return null;
    }

    private static int intAttribute(org.w3c.dom.Node node, String name, int fallback) {
        String value = stringAttribute(node, name, null);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static String stringAttribute(org.w3c.dom.Node node, String name, String fallback) {
        if (node == null || node.getAttributes() == null || node.getAttributes().getNamedItem(name) == null) {
            return fallback;
        }
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    private record Frame(int width, int height, int[] pixels, int delayMillis) {
    }

    private static final class AnimatedCape {
        private final String fileName;
        private final Identifier id;
        private DynamicTexture texture;
        private final List<Frame> frames;
        private int frameIndex;
        private long lastFrameTime = System.currentTimeMillis();

        private AnimatedCape(String fileName, Identifier id, DynamicTexture texture, List<Frame> frames) {
            this.fileName = fileName;
            this.id = id;
            this.texture = texture;
            this.frames = frames;
        }

        /**
         * Resource reloads and pack changes can either detach a dynamic texture
         * from the manager or dispose its GPU handle while leaving the Java
         * object registered. In both cases uploads become no-ops, so rebuild
         * the texture from the current frame before advancing the clip again.
         */
        private void ensureTexture(Minecraft client) {
            boolean detached = client.getTextureManager().getTexture(id) != texture;
            boolean disposed = false;
            if (!detached) {
                try {
                    texture.getTextureView();
                } catch (IllegalStateException e) {
                    disposed = true;
                }
            }
            if (!detached && !disposed) {
                return;
            }

            DynamicTexture replacement = new DynamicTexture(
                    () -> "OctoClient " + fileName, toNativeImage(frames.get(frameIndex)));
            client.getTextureManager().register(id, replacement);
            texture = replacement;
            if (DEBUG) {
                System.out.printf("[OctoClient] cape texture %s re-registered (%s)%n",
                        fileName, detached ? "detached" : "disposed");
            }
        }

        private void tick(Minecraft client) {
            ensureTexture(client);
            long now = System.currentTimeMillis();
            Frame frame = frames.get(frameIndex);
            if (now - lastFrameTime < frame.delayMillis) {
                return;
            }
            lastFrameTime = now;
            frameIndex = (frameIndex + 1) % frames.size();
            texture.setPixels(toNativeImage(frames.get(frameIndex)));
            texture.upload();
            if (DEBUG) {
                System.out.printf("[OctoClient] cape frame %s %d/%d%n",
                        fileName, frameIndex + 1, frames.size());
            }
        }
    }
}
