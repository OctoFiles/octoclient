package dev.octoclient.module.utility;

import com.mojang.blaze3d.platform.InputConstants;
import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

public final class FreeLook extends Module implements Module.Tickable {
    private final NumberSetting cameraDistance = new NumberSetting(
            "Camera Distance", "Distance of the third-person camera", 4, 0, 64, 0.5);
    private final BooleanSetting returnToFirstPerson = new BooleanSetting(
            "Return to First Person", "Return to first person when Free Look ends", false);
    private final BooleanSetting scrollDistance = new BooleanSetting(
            "Scroll Camera Distance", "Use the mouse wheel to change camera distance", true);
    private final BooleanSetting blockInventoryScrolling = new BooleanSetting(
            "Block Inventory Scrolling", "Prevent inventory scrolling from changing camera distance", true);
    private final BooleanSetting applyDistanceInThirdPerson = new BooleanSetting(
            "Third-Person Camera Distance", "Use the configured distance in normal third person", false);

    private KeyMapping holdKey;
    private KeyMapping toggleKey;
    private boolean active;
    private boolean toggled;
    private float cameraYaw;
    private float cameraPitch;
    private CameraType previousCameraType;

    public FreeLook() {
        super("Free Look", "Look around without changing your character's direction", ModuleCategory.UTILITY, 'F');
        addSetting(cameraDistance);
        addSetting(returnToFirstPerson);
        addSetting(scrollDistance);
        addSetting(blockInventoryScrolling);
        addSetting(applyDistanceInThirdPerson);
    }

    public void registerKeyMappings() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("octoclient", "free_look"));
        holdKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.free_look_hold", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y,
                category));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.octoclient.free_look_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
                category));
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            stop(client);
            return;
        }
        if (client.gui.screen() != null) {
            if (active && !toggled) stop(client);
            return;
        }

        while (toggleKey.consumeClick()) {
            if (active && toggled) {
                toggled = false;
                stop(client);
            } else if (!active) {
                start(client);
                toggled = true;
            }
        }

        if (holdKey.isDown() && !toggled) {
            if (!active) start(client);
        } else if (active && !toggled) {
            stop(client);
        }
    }

    private void start(Minecraft client) {
        if (active || client.player == null) return;
        previousCameraType = client.options.getCameraType();
        cameraYaw = client.player.getYRot() + 180.0F;
        cameraPitch = -client.player.getXRot();
        active = true;
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    private void stop(Minecraft client) {
        if (!active) {
            toggled = false;
            return;
        }
        active = false;
        toggled = false;
        if (previousCameraType != null) {
            client.options.setCameraType(returnToFirstPerson.isEnabled()
                    ? CameraType.FIRST_PERSON : previousCameraType);
            previousCameraType = null;
        }
    }

    @Override
    protected void onDisable() {
        stop(Minecraft.getInstance());
    }

    public boolean isFreeLooking() {
        return active;
    }

    public boolean capturesTurn(Entity entity) {
        return active && entity == Minecraft.getInstance().player;
    }

    public void addLookDelta(double yawDelta, double pitchDelta) {
        cameraYaw += (float) yawDelta * 0.15F;
        cameraPitch = Mth.clamp(cameraPitch - (float) pitchDelta * 0.15F, -90.0F, 90.0F);
    }

    public float cameraYaw() {
        return cameraYaw;
    }

    public float cameraPitch() {
        return cameraPitch;
    }

    public float cameraDistance() {
        return cameraDistance.getFloat();
    }

    public boolean shouldApplyDistance() {
        return active || applyDistanceInThirdPerson.isEnabled();
    }

    public boolean scrollDistanceEnabled() {
        return scrollDistance.isEnabled();
    }

    public boolean blockInventoryScrolling() {
        return blockInventoryScrolling.isEnabled();
    }

    public void changeDistance(double scroll) {
        cameraDistance.setRaw(cameraDistance.getSnapped() - Math.signum(scroll) * 0.5);
    }

    public void resetView() {
        Minecraft client = Minecraft.getInstance();
        if (active && client.player != null) {
            cameraYaw = client.player.getYRot() + 180.0F;
            cameraPitch = -client.player.getXRot();
        }
    }
}
