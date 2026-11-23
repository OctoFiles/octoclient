package dev.octoclient.module.hud;

import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TntTimerHud extends HudModule {
    private final NumberSetting range = new NumberSetting("Range", "Maximum distance to show primed TNT", 32, 4, 128, 1);
    private final NumberSetting maxEntries = new NumberSetting("Maximum Entries", "Maximum number of nearby TNT timers", 4, 1, 8, 1);
    private final BooleanSetting coloredLabel = new BooleanSetting("Colored Label", "Color TNT countdowns by remaining fuse", true);
    private final BooleanSetting showBackground = new BooleanSetting("Show Background", "Draw the timer panel background", true);
    private final BooleanSetting textShadow = new BooleanSetting("Text Shadow", "Draw text shadows", true);

    public TntTimerHud() {
        super("TNT Timer", "Show fuse countdowns for nearby primed TNT", 'T');
        addSetting(range);
        addSetting(maxEntries);
        addSetting(coloredLabel);
        addSetting(showBackground);
        addSetting(textShadow);
        x = 10;
        y = 150;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        if (mc.player == null || mc.level == null) {
            width = 60;
            height = 18;
            return;
        }

        double maxDistance = range.getSnapped();
        List<PrimedTnt> nearby = new ArrayList<>();
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof PrimedTnt tnt && tnt.getFuse() >= 0
                    && mc.player.distanceToSqr(tnt) <= maxDistance * maxDistance) {
                nearby.add(tnt);
            }
        }
        nearby.sort(Comparator.comparingDouble(mc.player::distanceToSqr));
        if (nearby.size() > maxEntries.getInt()) {
            nearby = nearby.subList(0, maxEntries.getInt());
        }

        List<String> rows = new ArrayList<>();
        rows.add("Primed TNT");
        for (PrimedTnt tnt : nearby) {
            double distance = mc.player.distanceTo(tnt);
            String fuse = String.format(Locale.ROOT, "%.1fs", Math.max(0, tnt.getFuse() / 20.0));
            rows.add(String.format(Locale.ROOT, "%s  %.0fm", fuse, distance));
        }
        if (nearby.isEmpty()) {
            rows.add("None nearby");
        }

        int textWidth = rows.stream().mapToInt(ctx::textWidth).max().orElse(60);
        int rowHeight = ctx.textHeight() + 3;
        int panelWidth = textWidth + 16;
        int panelHeight = rows.size() * rowHeight + 10;
        width = panelWidth;
        height = panelHeight;
        if (showBackground.isEnabled()) {
            drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, Math.min(cornerRadiusPx(), 8),
                    0x8E97C4, 0.94f, 0, 70);
        }
        int textY = (int) y + 5;
        for (int i = 0; i < rows.size(); i++) {
            int color = i == 0 ? 0xFFFFC857 : 0xFFFFFFFF;
            if (i > 0 && coloredLabel.isEnabled() && !nearby.isEmpty()) {
                int fuse = nearby.get(i - 1).getFuse();
                color = fuse <= 20 ? 0xFFFF5F6D : fuse <= 60 ? 0xFFFFC857 : 0xFFFFFFFF;
            }
            ctx.drawText(rows.get(i), x + 8, textY, color, textShadow.isEnabled());
            textY += rowHeight;
        }
    }
}
