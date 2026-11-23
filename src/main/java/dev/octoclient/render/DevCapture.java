package dev.octoclient.render;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

import java.nio.IntBuffer;

/**
 * Dev-only helper: writes one screenshot of the finished frame while the
 * dashboard is open. Enabled with {@code -Doctoclient.screenshot=<name>}.
 * Inert in normal play.
 */
public final class DevCapture {
    private static final String NAME = System.getProperty("octoclient.screenshot");
    private static final boolean ANY_SCREEN = Boolean.getBoolean("octoclient.screenshotAnyScreen");
    private static final boolean THIRD_PERSON = Boolean.getBoolean("octoclient.thirdPerson");
    /** Dev-only: look at the player's front instead of the back. */
    private static final boolean FRONT_VIEW = Boolean.getBoolean("octoclient.frontView");
    /** Dev-only: pin the player's facing so world captures are reproducible. */
    private static final boolean FACE_FORWARD = Boolean.getBoolean("octoclient.faceForward");
    private static int frames;
    private static boolean done;
    /** Dev-only: after the back shot, turn the camera around and grab the front too. */
    private static final boolean PAIR = Boolean.getBoolean("octoclient.pairShots");
    private static boolean secondShot;
    /** Dev-only: take this many shots a few frames apart to see an animation cycle. */
    private static final int SERIES = Integer.getInteger("octoclient.series", 0);
    private static int shotIndex;
    /**
     * Dev-only: drop the catalog wings from every other shot of a series. The
     * interleaved wingless frames come from the same run and the same lighting,
     * so subtracting them isolates the wing silhouette instead of the world.
     */
    private static final boolean WING_TOGGLE = Boolean.getBoolean("octoclient.wingToggle");
    /** Dev-only: drop the catalog shield from every other shot of a series. */
    private static final boolean SHIELD_TOGGLE = Boolean.getBoolean("octoclient.shieldToggle");
    /** Dev-only: move the test window to a secondary monitor (1-based). */
    private static final int MONITOR = Integer.getInteger("octoclient.monitor", 0);
    private static boolean monitorApplied;

    /** True when a dev capture wants this shot rendered without catalog wings. */
    public static boolean hideWings() {
        return WING_TOGGLE && (shotIndex & 1) == 1;
    }

    /** True when a dev capture should show the vanilla shield instead of the catalog skin. */
    public static boolean hideShield() {
        return SHIELD_TOGGLE && (shotIndex & 1) == 1;
    }

    private DevCapture() {
    }

    private static void placeWindow(Minecraft minecraft) {
        if (MONITOR <= 0 || monitorApplied) {
            return;
        }
        Window window = minecraft.getWindow();
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (window == null || monitors == null || monitors.limit() < MONITOR) {
            return;
        }
        long target = monitors.get(MONITOR - 1);
        IntBuffer x = BufferUtils.createIntBuffer(1);
        IntBuffer y = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetMonitorPos(target, x, y);
        GLFW.glfwSetWindowPos(window.handle(), x.get(0), y.get(0));
        GLFW.glfwFocusWindow(window.handle());
        monitorApplied = true;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        placeWindow(minecraft);

        if (NAME == null || NAME.isEmpty() || done) {
            return;
        }

        // Player cosmetics sit on the body, so a world capture needs third person.
        if (THIRD_PERSON && minecraft.level != null && minecraft.gui.screen() == null) {
            boolean front = FRONT_VIEW || secondShot;
            CameraType wanted = front ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
            if (minecraft.options.getCameraType() != wanted) {
                minecraft.options.setCameraType(wanted);
            }
        }
        if (FACE_FORWARD && minecraft.player != null) {
            minecraft.player.setYRot(0f);
            minecraft.player.setYHeadRot(0f);
            minecraft.player.setYBodyRot(0f);
        }
        // Dev captures should not depend on the time of day the world was left
        // in; the commands are only sent once, on the first frame.
        if (frames == 1 && minecraft.player != null
                && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand("time set noon");
            minecraft.player.connection.sendCommand("weather clear");
            String position = System.getProperty("octoclient.capturePos");
            if (position != null && !position.isEmpty()) {
                minecraft.player.connection.sendCommand("tp @s " + position);
            }
            String extra = System.getProperty("octoclient.captureCommand");
            if (extra != null && !extra.isEmpty()) {
                for (String command : extra.split(";")) {
                    if (!command.isBlank()) {
                        minecraft.player.connection.sendCommand(command.trim());
                    }
                }
            }
        }
        Screen screen = minecraft.gui.screen();
        boolean dashboard = screen != null && screen.getClass().getSimpleName().equals("DashboardScreen");
        if (!ANY_SCREEN && !dashboard) {
            frames = 0;
            return;
        }
        if (ANY_SCREEN && (minecraft.level == null || minecraft.gui.screen() != null)) {
            // A world capture should show the world, not whatever screen is open.
            frames = 0;
            return;
        }

        if (++frames < 90) {
            return;
        }

        String shotName = (secondShot ? NAME + "-front" : NAME)
                + (SERIES > 0 ? "-s" + shotIndex : "");
        done = true;
        System.out.println("[OctoClient] capture camera=" + minecraft.options.getCameraType()
                + " frontView=" + FRONT_VIEW + " thirdPerson=" + THIRD_PERSON
                + " name=" + shotName
                + " enabled=" + dev.octoclient.module.ModuleManager.get()
                        .getByCategory(dev.octoclient.module.ModuleCategory.COSMETICS).stream()
                        .filter(dev.octoclient.module.Module::isEnabled)
                        .map(dev.octoclient.module.Module::getName).toList()
                + " " + CosmeticLayers.capeDebug());
        try {
            Screenshot.grab(minecraft.gameDirectory, shotName + ".png",
                    minecraft.gameRenderer.mainRenderTarget(), 1,
                    (Component message) -> System.out.println("[OctoClient] capture: " + message.getString()));
        } catch (Throwable t) {
            System.err.println("[OctoClient] capture failed: " + t);
        }
        if (PAIR && !secondShot) {
            secondShot = true;
            done = false;
            frames = 0;
        } else if (SERIES > 0 && ++shotIndex < SERIES) {
            done = false;
            // Full 90-frame gap so a series actually shows animation instead
            // of eight nearly identical frames from the same few ticks.
            frames = 0;
        }
    }
}
