package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Client-side player poses; other players see them only if separately supported by the server. */
public class Emotes extends Module implements Module.Tickable {
    private static Emotes instance;

    private final ModeSetting emote =
            new ModeSetting("Emote", "Choose the local player animation", "Dance", List.of("Dance", "Wave", "Cheer"));
    private final NumberSetting duration =
            new NumberSetting("Duration", "Emote length in seconds", 5, 1, 30, 1);
    private final KeybindSetting playKey = new KeybindSetting("Play Emote Key", "Start the selected emote", GLFW.GLFW_KEY_H);
    private boolean wasDown;
    private long startedAt;

    public Emotes() {
        super("Emotes", "Play an animated local player pose", 'E');
        addSetting(emote);
        addSetting(duration);
        addSetting(playKey);
        instance = this;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.gui.screen() != null || client.player == null || playKey.getKeyCode() == 0) {
            wasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(client.getWindow().handle(), playKey.getKeyCode()) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            startedAt = System.currentTimeMillis();
        }
        wasDown = down;
    }

    public static boolean playSelected() {
        if (instance == null || !instance.isEnabled()) {
            return false;
        }
        instance.startedAt = System.currentTimeMillis();
        return true;
    }

    public static boolean applyPose(PlayerModel model, AvatarRenderState state, int localPlayerId) {
        if (instance == null || !instance.isEnabled() || instance.startedAt == 0
                || state.id != localPlayerId
                || System.currentTimeMillis() - instance.startedAt > instance.duration.getInt() * 1000L) {
            return false;
        }
        float time = (System.currentTimeMillis() - instance.startedAt) / 1000f;
        float wave = (float) Math.sin(time * 7.5f);
        switch (instance.emote.getValue()) {
            case "Wave" -> {
                model.rightArm.xRot = -2.35f + wave * 0.12f;
                model.rightArm.zRot = -0.3f;
                model.leftArm.xRot = -0.15f;
                model.rightLeg.xRot = 0.08f;
                model.leftLeg.xRot = -0.08f;
            }
            case "Cheer" -> {
                model.rightArm.xRot = -3.0f + wave * 0.12f;
                model.leftArm.xRot = -3.0f - wave * 0.12f;
                model.rightArm.zRot = -0.2f;
                model.leftArm.zRot = 0.2f;
                model.rightLeg.xRot = wave * 0.12f;
                model.leftLeg.xRot = -wave * 0.12f;
            }
            default -> {
                model.rightArm.xRot = -0.75f + wave * 0.65f;
                model.leftArm.xRot = -0.75f - wave * 0.65f;
                model.rightArm.zRot = -0.25f;
                model.leftArm.zRot = 0.25f;
                model.rightLeg.xRot = wave * 0.35f;
                model.leftLeg.xRot = -wave * 0.35f;
                model.body.yRot = wave * 0.12f;
            }
        }
        return true;
    }
}
