package dev.octoclient.mixin;

import com.mojang.authlib.GameProfile;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import dev.octoclient.util.Draw;
import dev.octoclient.util.NameBadges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Title menu look: the panorama stays untouched, the splash is dropped, the
 * vanilla logo and the client mark sit above the stock button column, and the
 * player's own skin walks in from the right once when the game starts, then
 * stays put and faces the camera.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    private static final Identifier OCTOCLIENT$LOGO_SPRITE =
            Identifier.fromNamespaceAndPath("octoclient", "logo");

    private static PlayerSkin octoclient$skin;
    private static boolean octoclient$skinRequested;
    private static long octoclient$walkStart;

    /** The version/copyright footer belongs at the very top. */
    @ModifyArg(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"),
            index = 3)
    private int octoclient$liftFooter(int y) {
        return 8;
    }

    /**
     * The owner's own skin, looked up once from the signed-in account.
     *
     * <p>The default skin is shown right away and replaced as soon as the skin
     * service answers, so a download that finishes a moment later still puts the
     * real skin on the menu instead of leaving the placeholder there forever.</p>
     */
    private static PlayerSkin octoclient$localSkin() {
        if (!octoclient$skinRequested) {
            octoclient$skinRequested = true;
            Minecraft mc = Minecraft.getInstance();
            GameProfile profile = mc.getGameProfile();
            try {
                octoclient$skin = DefaultPlayerSkin.get(profile);
            } catch (Throwable ignored) {
                // No model at all then.
            }
            try {
                // The cached lookup answers immediately; the real skin request
                // replaces it once the skin service has it, so an account whose
                // skin arrives a moment later is not stuck on the placeholder.
                PlayerSkin cached = mc.getSkinManager().createLookup(profile, false).get();
                if (cached != null) {
                    octoclient$skin = cached;
                }
                mc.getSkinManager().get(profile).thenAccept(found ->
                        found.ifPresent(skin -> octoclient$skin = skin));
            } catch (Throwable ignored) {
                // Offline dev profiles have no skin service; the default stays.
            }
        }
        return octoclient$skin;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void octoclient$drawMenuDecor(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                          float partialTick, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        int baseY = self.height / 4 + 48;

        // Client mark above the first entry, drawn as a GUI atlas sprite
        // (assets/octoclient/textures/gui/sprites/logo.png), with the plain
        // Minecraft wordmark in the game font right above it.
        int logo = Math.round(self.height * 0.15f);
        int logoY = baseY - logo - 10;
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, OCTOCLIENT$LOGO_SPRITE,
                self.width / 2 - logo / 2, logoY, logo, logo);
        // The real Minecraft logo, at its native size, above the client mark.
        int logoTextureWidth = 256;
        int logoTextureHeight = 64;
        int logoWidth = 256;
        int logoHeight = 44;
        gfx.blit(RenderPipelines.GUI_TEXTURED, LogoRenderer.MINECRAFT_LOGO,
                self.width / 2 - logoWidth / 2, 22, 0f, 0f,
                logoWidth, logoHeight, logoTextureWidth, logoTextureHeight);

        PlayerSkin skin = octoclient$localSkin();
        if (skin == null) {
            return;
        }
        // The title is drawn behind the loading overlay, so the clock only
        // starts once the game has actually finished loading: that is the
        // moment the menu becomes visible. After that first run the avatar
        // stands still where it arrived.
        if (octoclient$walkStart == 0L && Minecraft.getInstance().isGameLoadFinished()) {
            octoclient$walkStart = System.nanoTime();
        }
        float elapsed = octoclient$walkStart == 0L
                ? 0f
                : (System.nanoTime() - octoclient$walkStart) / 1_000_000_000f;
        float walk = Math.min(1f, elapsed / 1.5f);
        float travel = 1f - (float) Math.pow(1f - walk, 3f);
        float turn = Math.min(1f, Math.max(0f, (elapsed - 1.45f) / 0.6f));
        boolean walking = walk < 1f;

        int boxH = Math.min(320, Math.round(self.height * 0.42f));
        int boxW = Math.round(boxH * 0.75f);
        int cx = Math.round(self.width * 0.76f) + Math.round((1f - travel) * boxW * 1.4f);
        int y0 = self.height / 4 + 20 + Math.round((float) Math.sin(walk * Math.PI * 4f) * 5f * (1f - walk));
        int x0 = cx - boxW / 2;
        int x1 = x0 + boxW;
        int y1 = y0 + boxH;

        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = skin.cape() != null;
        state.boundingBoxWidth = 0.6f;
        state.boundingBoxHeight = 1.8f;
        state.lightCoords = 0xF000F0;
        state.pose = Pose.STANDING;
        // Facing the camera is a body rotation of 180; walking in shows a
        // profile that turns to the viewer once the walk is done.
        state.bodyRot = 180f + 70f * (1f - turn);
        state.yRot = 0f;
        state.xRot = 0f;
        state.scale = 1f;
        state.nameTag = null;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        // Real limb swing while the avatar crosses the screen.
        state.walkAnimationPos = walking ? elapsed * 9f : 0f;
        state.walkAnimationSpeed = walking ? 1f : 0f;

        float bob = state.boundingBoxHeight / 2f + 0.0625f;
        // GUI space is Y-up, so a half turn around Z makes the avatar face us.
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf();
        float size = Math.min((x1 - x0) / 1.9f, (y1 - y0) / 1.9f);
        gfx.entity(state, size, new Vector3f(0f, bob, 0f), rotation, xRotation,
                x0, y0, x1, y1);

        octoclient$drawNameTag(gfx, cx, y0);
    }

    /** Plain vanilla-style name tag over the avatar, with the owner's mark. */
    private void octoclient$drawNameTag(GuiGraphicsExtractor gfx, int cx, int avatarTop) {
        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = mc.getGameProfile();
        String name = profile.name();
        // Team members carry their own badge glyph; everyone else gets the
        // client mark. The badge is matched by UUID and, for offline or
        // migrated accounts, by the account name.
        Component badge = NameBadges.mark(profile.id(), name);
        int icon = 26;
        // The badge is a 10 px bitmap glyph, so it is scaled up to fill the
        // same square the client mark uses instead of sitting tiny in the plate.
        float badgeScale = icon / 10f;
        int markWidth = badge == null ? icon : Math.max(icon, Math.round(mc.font.width(badge) * badgeScale));
        int textWidth = Math.round(Draw.uiWidth(name, true));
        int gap = 5;
        int padX = 4;
        int padY = 2;
        int tagW = markWidth + gap + textWidth + padX * 2;
        int tagH = icon + padY * 2;
        int tagX = cx - tagW / 2;
        int tagY = avatarTop - tagH - 8;

        // The vanilla name tag plate: a flat 25% black box, no rounding, no rim.
        gfx.fill(tagX, tagY, tagX + tagW, tagY + tagH, 0x40000000);
        if (badge != null) {
            var pose = gfx.pose();
            pose.pushMatrix();
            pose.scale(badgeScale, badgeScale);
            gfx.text(mc.font, badge, Math.round((tagX + padX) / badgeScale),
                    Math.round((tagY + padY) / badgeScale), 0xFFFFFFFF, true);
            pose.popMatrix();
        } else {
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, OCTOCLIENT$LOGO_SPRITE,
                    tagX + padX, tagY + padY, icon, icon);
        }
        Draw.ui(gfx, name, tagX + padX + markWidth + gap, tagY + (tagH - 8) / 2f,
                0xFFFFFFFF, 1f, true, true);
    }
}

@Mixin(LogoRenderer.class)
abstract class TitleLogoMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void octoclient$hideLogo(CallbackInfo ci) {
        ci.cancel();
    }
}

@Mixin(SplashRenderer.class)
abstract class TitleSplashMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void octoclient$hideSplash(CallbackInfo ci) {
        ci.cancel();
    }
}
