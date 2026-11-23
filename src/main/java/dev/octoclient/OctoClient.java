package dev.octoclient;

import com.mojang.blaze3d.platform.InputConstants;
import dev.octoclient.config.ConfigManager;
import dev.octoclient.config.LauncherCosmeticSync;
import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.hud.HudDrawContext;
import dev.octoclient.module.hud.HudModule;
import dev.octoclient.module.render.Zoom;
import dev.octoclient.module.render.MotionBlur;
import dev.octoclient.module.render.ColorSaturation;
import dev.octoclient.module.utility.AutoGG;
import dev.octoclient.module.utility.ChatSuffix;
import dev.octoclient.module.utility.FreeLook;
import dev.octoclient.module.utility.Panic;
import dev.octoclient.module.secret.SecretTabUnlock;
import dev.octoclient.module.utility.StreamerMode;
import dev.octoclient.network.CapeSyncClient;
import dev.octoclient.network.CosmeticSyncClient;
import dev.octoclient.render.AnimatedCapeTextures;
import dev.octoclient.render.CosmeticLayers;
import dev.octoclient.ui.DashboardScreen;
import dev.octoclient.ui.CosmeticsScreen;
import dev.octoclient.ui.EditHudScreen;
import dev.octoclient.util.ClickCounter;
import dev.octoclient.util.FpsCounter;
import dev.octoclient.util.TargetTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * OctoClient — dark glassmorphism utility client.
 *
 * <p>RSHIFT: dashboard | O: cosmetics | INSERT: HUD editor | Y/M: free look | DELETE: panic | C: zoom | G: fullbright</p>
 */
public class OctoClient implements ClientModInitializer {
    public static final String MOD_ID = "octoclient";
    public static final String VERSION = "1.0.0";

    private KeyMapping dashboardKey;
    private KeyMapping cosmeticsKey;
    private KeyMapping hudEditorKey;
    private KeyMapping freeLookResetKey;
    private KeyMapping panicKey;
    private KeyMapping fullbrightKey;

    /** Dev-only: -Doctoclient.autoDashboard=true opens the dashboard once a world is loaded. */
    private static final boolean AUTO_DASHBOARD = Boolean.getBoolean("octoclient.autoDashboard");
    /** Dev-only: -Doctoclient.dashboardAtTitle=true opens it on the title screen for captures. */
    private static final boolean DASHBOARD_AT_TITLE = Boolean.getBoolean("octoclient.dashboardAtTitle");
    /** Dev-only: open the cosmetics studio on the title screen instead. */
    private static final boolean COSMETICS_AT_TITLE = Boolean.getBoolean("octoclient.cosmeticsAtTitle");
    private boolean autoDashboardShown;
    private int dashboardAtTitleTicks;

    @Override
    public void onInitializeClient() {
        // Force module registry creation, then restore persisted state.
        ModuleManager.get();
        ConfigManager.load();
        LauncherCosmeticSync.applyOnStartup();
        dev.skylyrics.SkyLyricsClient.initializeIntegrated();
        CapeSyncClient.initialize();
        CosmeticSyncClient.initialize();
        TargetTracker.get().init();
        AnimatedCapeTextures.initialize();
        CosmeticLayers.register();

        registerKeys();
        registerHud();
        registerTickHandler();

        // Modules that hook Fabric events themselves.
        ModuleManager.get().byName("Chat Suffix", ChatSuffix.class).ifPresent(ChatSuffix::init);
        ModuleManager.get().byName("Auto GG", AutoGG.class).ifPresent(AutoGG::init);

        System.out.println("[OctoClient] v" + VERSION + " initialized (RSHIFT for the dashboard)");
    }

    private void registerKeys() {
        ModuleManager.get().byName("Free Look", FreeLook.class)
                .ifPresent(FreeLook::registerKeyMappings);

        dashboardKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.dashboard",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));

        cosmeticsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.cosmetics",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyMapping.Category.MISC));

        hudEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.hud_editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_INSERT,
                KeyMapping.Category.MISC));

        freeLookResetKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.free_look_reset",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.MISC));

        panicKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.panic",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                KeyMapping.Category.MISC));

        fullbrightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.fullbright",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.Category.MISC));
    }

    private void registerHud() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), (gfx, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            // The HUD editor draws its own previews.
            if (mc.gui.screen() instanceof EditHudScreen) {
                return;
            }

            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
            FpsCounter.get().onFrame();
            ClickCounter.get().sample();

            HudDrawContext ctx = new HudDrawContext(gfx, mc, tickDelta,
                    gfx.guiWidth(), gfx.guiHeight());
            for (HudModule module : ModuleManager.get().getHudModules()) {
                if (module.isEnabled()) {
                    try {
                        module.renderScaled(ctx, tickDelta, false, false);
                    } catch (Exception e) {
                        System.err.println("[OctoClient] HUD " + module.getName() + " failed: " + e);
                    }
                }
            }
            ModuleManager.get().byName("Color Saturation", ColorSaturation.class)
                    .ifPresent(saturationModule -> saturationModule.renderOverlay(gfx));
        });
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SecretTabUnlock.tick(client);
            ModuleManager.get().byName("Motion Blur", MotionBlur.class)
                    .ifPresent(MotionBlur::syncExternalConfig);
            ModuleManager.get().byName("Streamer Mode", StreamerMode.class)
                    .ifPresent(StreamerMode::syncTextureProtection);

            if (AUTO_DASHBOARD && !autoDashboardShown && client.player != null && client.gui.screen() == null) {
                autoDashboardShown = true;
                client.setScreenAndShow(new DashboardScreen());
            }
            // Dev-only capture path: the dashboard needs no world, so a title
            // screen run can grab it without loading a save.
            // The loading overlay still draws over the title screen and item
            // components are only bound afterwards, so wait a moment first.
            if ((DASHBOARD_AT_TITLE || COSMETICS_AT_TITLE) && !autoDashboardShown && client.player == null
                    && client.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
                // Give the loading overlay time to close before the capture.
                if (++dashboardAtTitleTicks > 120) {
                    autoDashboardShown = true;
                    client.setScreenAndShow(COSMETICS_AT_TITLE ? new CosmeticsScreen() : new DashboardScreen());
                }
            }
            if (dashboardKey.consumeClick() && client.player != null) {
                if (client.gui.screen() instanceof DashboardScreen) {
                    client.setScreenAndShow(null);
                } else if (client.gui.screen() == null) {
                    client.setScreenAndShow(new DashboardScreen());
                }
            }
            if (cosmeticsKey.consumeClick() && client.player != null) {
                if (client.gui.screen() instanceof CosmeticsScreen) {
                    client.setScreenAndShow(null);
                } else if (client.gui.screen() == null || client.gui.screen() instanceof PauseScreen) {
                    client.setScreenAndShow(new CosmeticsScreen());
                }
            }
            if (hudEditorKey.consumeClick() && client.player != null && client.gui.screen() == null) {
                client.setScreenAndShow(new EditHudScreen());
            }
            if (freeLookResetKey.consumeClick()) {
                ModuleManager.get().byName("Free Look", FreeLook.class)
                        .filter(FreeLook::isFreeLooking)
                        .ifPresent(FreeLook::resetView);
            }
            if (panicKey.consumeClick()) {
                ModuleManager.get().byName("Panic", Panic.class)
                        .filter(Module::isEnabled)
                        .ifPresent(Panic::trigger);
            }
            if (fullbrightKey.consumeClick()) {
                ModuleManager.get().byName("Fullbright")
                        .ifPresent(m -> m.setEnabled(!m.isEnabled()));
            }
            // Tick enabled utility modules.
            for (Module module : ModuleManager.get().getModules()) {
                module.pollToggleKey(client, client.player != null && client.gui.screen() == null);
                if (module.isEnabled() && module instanceof Module.Tickable tickable) {
                    try {
                        tickable.onTick(client);
                    } catch (Exception e) {
                        System.err.println("[OctoClient] Tick " + module.getName() + " failed: " + e);
                    }
                }
            }
        });
    }
}
