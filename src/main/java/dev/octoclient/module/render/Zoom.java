package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Hold-to-zoom with configurable key, smoothing, scroll strength and sensitivity. */
public class Zoom extends Module implements Module.Tickable {
    private final KeybindSetting zoomKey =
            new KeybindSetting("Zoom Key", "Hold this key to zoom in", GLFW.GLFW_KEY_C);
    private final BooleanSetting enableScrolling =
            new BooleanSetting("Enable Scrolling", "Adjust zoom strength with the mouse wheel", true);
    private final BooleanSetting smoothCamera =
            new BooleanSetting("Smooth Camera", "Enable Minecraft's smooth-camera mode while zooming", false);
    private final BooleanSetting smoothZoom =
            new BooleanSetting("Smooth Zoom", "Animate zoom transitions", true);
    private final BooleanSetting adjustMouseSensitivity =
            new BooleanSetting("Adjust Mouse Sensitivity", "Reduce mouse sensitivity as the view zooms in", true);
    private final NumberSetting zoomInDuration =
            new NumberSetting("Zoom In Duration", "Zoom-in transition time in milliseconds", 180, 0, 1500, 10);
    private final NumberSetting zoomOutDuration =
            new NumberSetting("Zoom Out Duration", "Zoom-out transition time in milliseconds", 220, 0, 1500, 10);
    private final NumberSetting scrollStrength =
            new NumberSetting("Scroll Strength", "FOV change per mouse-wheel notch", 3, 1, 15, 1);
    private final ModeSetting easing =
            new ModeSetting("Easing", "Transition curve", "Ease Out",
                    List.of("Linear", "Ease In", "Ease Out", "Ease In Out"));
    private final NumberSetting amount =
            new NumberSetting("Zoom Amount", "How many FOV degrees to reduce", 14, 2, 100, 1);

    private boolean zoomed;
    private boolean originalSmoothCamera;
    private double originalSensitivity;
    private int baseline;
    private float current;
    private float transitionStart;
    private float transitionTarget;
    private long transitionStartNanos;

    public Zoom() {
        super("Zoom", "Hold a key to zoom in", 'Z');
        addSetting(zoomKey);
        addSetting(enableScrolling);
        addSetting(smoothCamera);
        addSetting(smoothZoom);
        addSetting(adjustMouseSensitivity);
        addSetting(zoomInDuration);
        addSetting(zoomOutDuration);
        addSetting(scrollStrength);
        addSetting(easing);
        addSetting(amount);
    }

    @Override
    protected void onDisable() {
        restore();
    }

    @Override
    public void onTick(Minecraft client) {
        boolean held = zoomKey.getKeyCode() != 0
                && GLFW.glfwGetKey(client.getWindow().handle(), zoomKey.getKeyCode()) == GLFW.GLFW_PRESS;
        boolean requested = held && client.gui.screen() == null && client.level != null;
        if (requested && !zoomed) {
            beginZoom(client);
        }
        if (!zoomed) {
            return;
        }

        float target = requested ? Math.max(1f, baseline - amount.getFloat()) : baseline;
        if (requested != (transitionTarget < baseline)) {
            startTransition(target);
        } else if (Math.abs(target - transitionTarget) > 0.01f) {
            startTransition(target);
        }

        if (!smoothZoom.isEnabled()) {
            current = target;
            client.options.fov().set(Math.round(current));
            if (!requested) {
                restore();
            }
            return;
        }

        long duration = Math.round((requested ? zoomInDuration : zoomOutDuration).getSnapped() * 1_000_000L);
        float progress = duration == 0 ? 1f
                : Math.min(1f, (System.nanoTime() - transitionStartNanos) / (float) duration);
        float eased = ease(progress, easing.getValue());
        current = transitionStart + (transitionTarget - transitionStart) * eased;
        client.options.fov().set(Math.round(current));
        if (adjustMouseSensitivity.isEnabled()) {
            double ratio = Math.max(0.1, Math.min(1.0, current / Math.max(1, baseline)));
            client.options.sensitivity().set(originalSensitivity * ratio);
        }
        if (!requested && progress >= 1f) {
            restore();
        }
    }

    private void beginZoom(Minecraft client) {
        zoomed = true;
        baseline = client.options.fov().get();
        current = baseline;
        originalSensitivity = client.options.sensitivity().get();
        originalSmoothCamera = client.options.smoothCamera;
        if (smoothCamera.isEnabled()) {
            client.options.smoothCamera = true;
        }
        startTransition(Math.max(1f, baseline - amount.getFloat()));
    }

    private void startTransition(float target) {
        transitionStart = current;
        transitionTarget = target;
        transitionStartNanos = System.nanoTime();
    }

    private static float ease(float value, String mode) {
        float t = Math.max(0f, Math.min(1f, value));
        return switch (mode) {
            case "Ease In" -> t * t;
            case "Ease Out" -> 1f - (1f - t) * (1f - t);
            case "Ease In Out" -> t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
            default -> t;
        };
    }

    public static boolean adjustFromScroll(double vertical) {
        Zoom module = ModuleManagerHolder.zoom();
        Minecraft client = Minecraft.getInstance();
        if (module == null || !module.isEnabled() || !module.enableScrolling.isEnabled()
                || !module.zoomed || vertical == 0 || client.gui.screen() != null) {
            return false;
        }
        module.amount.setRaw(module.amount.getValue()
                + Math.signum(vertical) * module.scrollStrength.getSnapped());
        return true;
    }

    private void restore() {
        if (!zoomed) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        client.options.fov().set(baseline);
        client.options.sensitivity().set(originalSensitivity);
        client.options.smoothCamera = originalSmoothCamera;
        zoomed = false;
        current = baseline;
    }

    private static final class ModuleManagerHolder {
        private static Zoom zoom() {
            return dev.octoclient.module.ModuleManager.get()
                    .byName("Zoom", Zoom.class).orElse(null);
        }
    }
}
