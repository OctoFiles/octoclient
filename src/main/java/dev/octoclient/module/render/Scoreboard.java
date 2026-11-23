package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;

/** Controls sidebar scoreboard sizing, score visibility and text shadow. */
public class Scoreboard extends Module {
    private static Scoreboard instance;
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", "Overall scoreboard scale", 1, 0.5, 2, 0.05);
    private final NumberSetting corners = new NumberSetting("Corners", "Scoreboard panel corner radius", 3, 0, 12, 1);
    private final BooleanSetting dynamicPadding =
            new BooleanSetting("Dynamic Padding", "Keep compact spacing around scoreboard rows", true);
    private final NumberSetting width = new NumberSetting("Width", "Horizontal size multiplier", 1, 0.5, 2, 0.05);
    private final NumberSetting height = new NumberSetting("Height", "Vertical size multiplier", 1, 0.5, 2, 0.05);
    private final BooleanSetting showNumbers =
            new BooleanSetting("Show Numbers", "Show score values beside sidebar entries", true);
    private final BooleanSetting fontShadow =
            new BooleanSetting("Font Shadow", "Draw shadows beneath scoreboard text", true);
    private final BooleanSetting hideScoreboard =
            new BooleanSetting("Hide Scoreboard", "Hide the vanilla sidebar scoreboard", false);

    public Scoreboard() {
        super("Scoreboard", "Customize the in-game sidebar scoreboard", 'S');
        addSetting(hudScale);
        addSetting(corners);
        addSetting(dynamicPadding);
        addSetting(width);
        addSetting(height);
        addSetting(showNumbers);
        addSetting(fontShadow);
        addSetting(hideScoreboard);
        instance = this;
    }

    public static boolean isHidden() {
        return instance != null && instance.isEnabled() && instance.hideScoreboard.isEnabled();
    }

    public static boolean numbersEnabled() {
        return instance == null || !instance.isEnabled() || instance.showNumbers.isEnabled();
    }

    public static boolean fontShadowEnabled() {
        return instance == null || !instance.isEnabled() || instance.fontShadow.isEnabled();
    }

    public static void begin(GuiGraphicsExtractor graphics) {
        if (instance == null || !instance.isEnabled()) {
            return;
        }
        float sx = instance.hudScale.getFloat() * instance.width.getFloat();
        float sy = instance.hudScale.getFloat() * instance.height.getFloat();
        graphics.pose().pushMatrix();
        graphics.pose().translate(graphics.guiWidth(), graphics.guiHeight() / 2f);
        graphics.pose().scale(sx, sy);
        graphics.pose().translate(-graphics.guiWidth(), -graphics.guiHeight() / 2f);
    }

    public static void end(GuiGraphicsExtractor graphics) {
        if (instance != null && instance.isEnabled() && !isHidden()) {
            graphics.pose().popMatrix();
        }
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        if (ClearBackground.clearsScoreboard()) {
            return;
        }
        Scoreboard module = instance;
        if (module == null || !module.isEnabled()) {
            graphics.fill(x1, y1, x2, y2, color);
            return;
        }
        int insetX = module.dynamicPadding.isEnabled() ? 0 : 2;
        int insetY = module.dynamicPadding.isEnabled() ? 0 : 1;
        int left = x1 + insetX;
        int top = y1 + insetY;
        int right = Math.max(left, x2 - insetX);
        int bottom = Math.max(top, y2 - insetY);
        int radius = module.corners.getInt();
        if (radius == 0) {
            graphics.fill(left, top, right, bottom, color);
        } else {
            Draw.roundedRect(graphics, left, top, right - left, bottom - top, radius, color);
        }
    }

    public static MutableComponent formatScore(PlayerScoreEntry entry, NumberFormat format) {
        return numbersEnabled() ? entry.formatValue(format) : Component.empty();
    }
}
