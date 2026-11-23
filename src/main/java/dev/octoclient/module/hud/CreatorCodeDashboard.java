package dev.octoclient.module.hud;

import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** Local, manually recorded creator-code statistics; no external analytics service is assumed. */
public class CreatorCodeDashboard extends HudModule implements dev.octoclient.module.Module.Tickable {
    private final TextSetting[] codes = new TextSetting[3];
    private final NumberSetting[] views = new NumberSetting[3];
    private final NumberSetting[] clicks = new NumberSetting[3];
    private final NumberSetting[] redemptions = new NumberSetting[3];
    private final ModeSetting activeCode = new ModeSetting("Selected Code", "Code receiving hotkey events",
            "1", List.of("1", "2", "3"));
    private final KeybindSetting viewKey = new KeybindSetting("Record View Key", "Record one view manually", 0);
    private final KeybindSetting clickKey = new KeybindSetting("Record Click Key", "Record one click manually", 0);
    private final KeybindSetting redemptionKey =
            new KeybindSetting("Record Redemption Key", "Record one redemption manually", 0);
    private final boolean[] wasDown = new boolean[3];

    public CreatorCodeDashboard() {
        super("Creator Code Dashboard", "Manage creator codes and manually record their performance", 'C');
        addSetting(activeCode);
        addSetting(viewKey);
        addSetting(clickKey);
        addSetting(redemptionKey);
        for (int i = 0; i < codes.length; i++) {
            codes[i] = new TextSetting("Code " + (i + 1), "Creator code for entry " + (i + 1), "", 48);
            views[i] = new NumberSetting("Views " + (i + 1), "Manually tracked views", 0, 0, 1_000_000_000, 1);
            clicks[i] = new NumberSetting("Clicks " + (i + 1), "Manually tracked clicks", 0, 0, 1_000_000_000, 1);
            redemptions[i] = new NumberSetting("Redemptions " + (i + 1), "Manually tracked redemptions", 0, 0, 1_000_000_000, 1);
            addSetting(codes[i]);
            addSetting(views[i]);
            addSetting(clicks[i]);
            addSetting(redemptions[i]);
        }
        x = 10;
        y = 250;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.gui.screen() != null || client.player == null) {
            wasDown[0] = wasDown[1] = wasDown[2] = false;
            return;
        }
        long window = client.getWindow().handle();
        NumberSetting[] counters = {
                views[activeIndex()],
                clicks[activeIndex()],
                redemptions[activeIndex()]
        };
        KeybindSetting[] bindings = {viewKey, clickKey, redemptionKey};
        for (int i = 0; i < bindings.length; i++) {
            int key = bindings[i].getKeyCode();
            boolean down = key != 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            if (down && !wasDown[i]) {
                counters[i].setRaw(counters[i].getSnapped() + 1);
            }
            wasDown[i] = down;
        }
    }

    private int activeIndex() {
        return switch (activeCode.getValue()) {
            case "2" -> 1;
            case "3" -> 2;
            default -> 0;
        };
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        java.util.ArrayList<String> rows = new java.util.ArrayList<>();
        rows.add("Creator Codes");
        for (int i = 0; i < codes.length; i++) {
            String code = codes[i].getValue().trim();
            if (code.isEmpty()) {
                continue;
            }
            double clickRate = views[i].getSnapped() == 0 ? 0 : clicks[i].getSnapped() * 100 / views[i].getSnapped();
            double redemptionRate = clicks[i].getSnapped() == 0 ? 0
                    : redemptions[i].getSnapped() * 100 / clicks[i].getSnapped();
            rows.add(String.format(Locale.ROOT, "%s  V:%d C:%d R:%d  CTR %.1f%% / CVR %.1f%%",
                    code, views[i].getInt(), clicks[i].getInt(), redemptions[i].getInt(), clickRate, redemptionRate));
        }
        if (rows.size() == 1) {
            rows.add("Set codes in module settings");
        }
        int textWidth = rows.stream().mapToInt(ctx::textWidth).max().orElse(100);
        int rowHeight = ctx.textHeight() + 3;
        int panelWidth = textWidth + 16;
        int panelHeight = rows.size() * rowHeight + 10;
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, Math.min(cornerRadiusPx(), 8),
                0x8E97C4, 0.94f, 0, 70);
        int textY = (int) y + 5;
        for (int i = 0; i < rows.size(); i++) {
            ctx.drawText(rows.get(i), x + 8, textY, i == 0 ? 0xFFFFC857 : 0xFFFFFFFF, true);
            textY += rowHeight;
        }
    }
}
