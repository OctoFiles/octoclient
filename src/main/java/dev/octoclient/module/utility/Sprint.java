package dev.octoclient.module.utility;

import dev.octoclient.module.hud.HudDrawContext;
import dev.octoclient.module.hud.HudModule;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Automatic sprinting with an optional fly-speed control and status HUD. */
public class Sprint extends HudModule implements Module.Tickable {
    private static Sprint instance;

    private final ModeSetting direction = new ModeSetting("Direction", "Movement directions that enable sprint",
            "Forward", List.of("Forward", "Any"));
    private final BooleanSetting allowUsingItems =
            new BooleanSetting("While Using Items", "Keep sprint active while using an item", false);
    private final BooleanSetting notification =
            new BooleanSetting("HUD Notification", "Show a brief status message when toggled", true);
    private final TextSetting textFormat =
            new TextSetting("Text Format", "Use {status} for the current sprint state", "Sprint: {status}", 48);
    private final ColorSetting color = new ColorSetting("Color", "Sprint status text color", 0xFFFFFFFF);
    private final BooleanSetting dynamicPadding =
            new BooleanSetting("Dynamic Padding", "Size padding to the rendered text", true);
    private final NumberSetting fixedWidth = new NumberSetting("Width", "Fixed HUD width; zero uses text width", 0, 0, 400, 1);
    private final NumberSetting fixedHeight = new NumberSetting("Height", "Fixed HUD height; zero uses text height", 0, 0, 100, 1);
    private final BooleanSetting showHud =
            new BooleanSetting("Show in HUD", "Show sprint state on the HUD", true);
    private final BooleanSetting enableFlyBoost =
            new BooleanSetting("Enable Fly Boost", "Apply the configured speed while flying", false);
    private final NumberSetting flyBoost =
            new NumberSetting("Fly Boost Value", "Player flying speed while boosted", 0.1, 0.01, 1.0, 0.01);
    private final BooleanSetting changeBoostByScrolling =
            new BooleanSetting("Change Boost by Scrolling", "Adjust fly boost with the mouse wheel", false);
    private final NumberSetting flyBoostScrollIncrement =
            new NumberSetting("Fly Boost Scroll Increment", "Fly speed change per scroll notch", 0.01, 0.001, 0.1, 0.001);
    private float originalFlyingSpeed;
    private boolean boostApplied;

    public Sprint() {
        super("ToggleSprint", "Automatically sprint while moving", 'R');
        addSetting(direction);
        addSetting(allowUsingItems);
        addSetting(notification);
        addSetting(textFormat);
        addSetting(color);
        addSetting(dynamicPadding);
        addSetting(fixedWidth);
        addSetting(fixedHeight);
        addSetting(showHud);
        addSetting(enableFlyBoost);
        addSetting(flyBoost);
        addSetting(changeBoostByScrolling);
        addSetting(flyBoostScrollIncrement);
        x = 4;
        y = 28;
        instance = this;
    }

    @Override
    protected void onEnable() {
        showNotification("ToggleSprint enabled");
    }

    @Override
    protected void onDisable() {
        restoreFlyingSpeed();
        showNotification("ToggleSprint disabled");
    }

    private void showNotification(String text) {
        Minecraft client = Minecraft.getInstance();
        if (notification.isEnabled() && client.gui != null && client.player != null) {
            client.player.sendOverlayMessage(Component.literal(text));
        }
    }

    @Override
    public void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        if (player.getAbilities().mayfly && player.getAbilities().flying && enableFlyBoost.isEnabled()) {
            if (!boostApplied) {
                originalFlyingSpeed = player.getAbilities().getFlyingSpeed();
                boostApplied = true;
            }
            player.getAbilities().setFlyingSpeed(flyBoost.getFloat());
        } else {
            restoreFlyingSpeed();
        }

        if (client.gui.screen() != null
                || (!allowUsingItems.isEnabled() && player.isUsingItem())) {
            return;
        }
        boolean moving = direction.getValue().equals("Any")
                ? (client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown())
                : client.options.keyUp.isDown() && !client.options.keyDown.isDown();
        if (moving) {
            player.setSprinting(true);
        }
    }

    private void restoreFlyingSpeed() {
        Minecraft client = Minecraft.getInstance();
        if (boostApplied && client.player != null) {
            client.player.getAbilities().setFlyingSpeed(originalFlyingSpeed);
        }
        boostApplied = false;
    }

    public static boolean adjustFlyBoostFromScroll(double vertical) {
        Sprint module = instance;
        Minecraft client = Minecraft.getInstance();
        if (module == null || !module.isEnabled() || !module.enableFlyBoost.isEnabled()
                || !module.changeBoostByScrolling.isEnabled() || vertical == 0
                || client.player == null || client.gui.screen() != null
                || !client.player.getAbilities().flying || !client.player.getAbilities().mayfly) {
            return false;
        }
        double direction = Math.signum(vertical);
        module.flyBoost.setRaw(module.flyBoost.getValue()
                + direction * module.flyBoostScrollIncrement.getSnapped());
        return true;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (!showHud.isEnabled() || ctx.player() == null) {
            return;
        }
        String status = ctx.player().isSprinting() ? "On" : "Off";
        String text = textFormat.getValue().replace("{status}", status);
        int textWidth = ctx.textWidth(text);
        int textHeight = ctx.textHeight();
        int padding = dynamicPadding.isEnabled() ? 6 : 0;
        int panelWidth = fixedWidth.getInt() == 0 ? textWidth + padding * 2
                : Math.max(fixedWidth.getInt(), textWidth);
        int panelHeight = fixedHeight.getInt() == 0 ? textHeight + padding * 2
                : Math.max(fixedHeight.getInt(), textHeight);
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        ctx.drawText(text, x + (panelWidth - textWidth) / 2f, y + (panelHeight - textHeight) / 2f,
                color.getValue(), false);
    }
}
