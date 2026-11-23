package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.math.Axis;
import org.joml.Vector3f;

public class Skin3DHud extends HudModule {
    private final NumberSetting detailDistance =
            new NumberSetting("Detail Distance", "Distance before skin overlays are simplified", 64, 0, 256, 1);
    private final BooleanSetting showHat = new BooleanSetting("Show Hat", "Show the outer head layer", true);
    private final BooleanSetting snowJacket = new BooleanSetting("Snow Jacket", "Show the jacket overlay", true);
    private final BooleanSetting leftSleeve = new BooleanSetting("Show Left Sleeve", "Show the left sleeve overlay", true);
    private final BooleanSetting rightSleeve = new BooleanSetting("Show Right Sleeve", "Show the right sleeve overlay", true);
    private final BooleanSetting leftPants = new BooleanSetting("Show Left Pants", "Show the left pants overlay", true);
    private final BooleanSetting rightPants = new BooleanSetting("Show Right Pants", "Show the right pants overlay", true);
    private final NumberSetting voxelSize = new NumberSetting("Voxel Size", "Overall skin model voxel scale", 1, 0.5, 2, 0.05);
    public Skin3DHud() {
        super("3D Skin", "Show your player skin as a 3D HUD model", '3');
        addSetting(detailDistance);
        addSetting(showHat);
        addSetting(snowJacket);
        addSetting(leftSleeve);
        addSetting(rightSleeve);
        addSetting(leftPants);
        addSetting(rightPants);
        addSetting(voxelSize);
        x = 20;
        y = 90;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (ctx.mc.player == null) {
            width = 64;
            height = 120;
            return;
        }
        double cameraDistance = ctx.mc.gameRenderer.mainCamera().position()
                .distanceTo(ctx.player().position());
        boolean showDetail = cameraDistance <= detailDistance.getFloat();
        EntityRenderState state = ctx.mc.getEntityRenderDispatcher().getRenderer(ctx.mc.player)
                .createRenderState(ctx.mc.player, tickDelta);
        if (state instanceof AvatarRenderState avatar) {
            avatar.showHat = showDetail && showHat.isEnabled();
            avatar.showJacket = showDetail && snowJacket.isEnabled();
            avatar.showLeftSleeve = showDetail && leftSleeve.isEnabled();
            avatar.showRightSleeve = showDetail && rightSleeve.isEnabled();
            avatar.showLeftPants = showDetail && leftPants.isEnabled();
            avatar.showRightPants = showDetail && rightPants.isEnabled();
            avatar.scale *= voxelSize.getFloat();
            if (avatar.scale != 0f) {
                avatar.boundingBoxWidth /= avatar.scale;
                avatar.boundingBoxHeight /= avatar.scale;
            }
        }
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180f;
            living.yRot = 180f;
            living.xRot = 0f;
        }
        state.nameTag = null;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        int widgetWidth = 64;
        int widgetHeight = 120;
        float bob = state.boundingBoxHeight / 2f + 0.0625f;
        ctx.gfx.entity(state, 64, new Vector3f(0f, bob, 0f), Axis.YP.rotationDegrees(180f), Axis.XP.rotationDegrees(0f),
                (int) x, (int) y, (int) x + widgetWidth, (int) y + widgetHeight);
        width = widgetWidth;
        height = widgetHeight;
    }
}
