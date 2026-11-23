package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.mixin.FoodDataAccessor;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hunger level plus the hidden saturation value. */
public class SaturationHud extends HudModule {
    private final BooleanSetting label = new BooleanSetting("Label", "Show the SAT prefix", true);
    private final BooleanSetting tooltipInfo = new BooleanSetting("Show Tooltip Info", "Show food-item nutrition details in this HUD", false);
    private final BooleanSetting showSaturation = new BooleanSetting("Show Saturation", "Show the current saturation value", true);
    private final BooleanSetting showExhaustion = new BooleanSetting("Show Exhaustion", "Show current exhaustion", false);
    private final BooleanSetting showRestoredHealth = new BooleanSetting("Show Restored Health", "Show estimated health regeneration potential from held food", false);
    private final BooleanSetting showRestoredHunger = new BooleanSetting("Show Restored Hunger", "Show hunger restored by held food", false);
    private final BooleanSetting foodBarOverlay = new BooleanSetting("Food Bar Overlay",
            "AppleSkin-style preview of the hunger and saturation a held food restores", true);

    public SaturationHud() {
        super("Saturation", "Hunger and saturation", 'H');
        this.x = 4;
        this.y = 4;
        addSetting(label);
        addSetting(tooltipInfo);
        addSetting(showSaturation);
        addSetting(showExhaustion);
        addSetting(showRestoredHealth);
        addSetting(showRestoredHunger);
        addSetting(foodBarOverlay);
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        var player = ctx.player();
        if (player == null) {
            return;
        }
        var food = player.getFoodData();
        List<String> values = new ArrayList<>();
        values.add((label.isEnabled() ? "HUN " : "") + food.getFoodLevel());
        if (showSaturation.isEnabled()) {
            values.add(String.format(Locale.ROOT, "SAT %.1f", food.getSaturationLevel()));
        }
        if (showExhaustion.isEnabled() && food instanceof FoodDataAccessor accessor) {
            values.add(String.format(Locale.ROOT, "EXH %.1f", accessor.octoclient$getExhaustionLevel()));
        }
        var heldFood = player.getMainHandItem().get(DataComponents.FOOD);
        if (tooltipInfo.isEnabled() && heldFood != null) {
            values.add(String.format(Locale.ROOT, "Food: +%d, sat %.1f", heldFood.nutrition(),
                    heldFood.saturation() * heldFood.nutrition() * 2f));
        }
        if (showRestoredHunger.isEnabled() && heldFood != null) {
            values.add("+" + heldFood.nutrition() + " hunger");
        }
        if (showRestoredHealth.isEnabled()) {
            float potential = heldFood == null ? 0f
                    : Math.min(Math.max(0f, 20f - player.getHealth()), heldFood.nutrition() / 6f);
            values.add(String.format(Locale.ROOT, "Heal ~%.1f", potential));
        }
        String text = String.join("  |  ", values);
        chip(ctx, text, Theme.TEXT_SECONDARY);
        drawFoodBarOverlay(ctx);
    }

    /**
     * AppleSkin-style overlay on the vanilla hunger bar: the hunger points and
     * saturation a held food item would restore are drawn on top of the icons.
     */
    private void drawFoodBarOverlay(HudDrawContext ctx) {
        var player = ctx.player();
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        var food = player.getFoodData();
        int hunger = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        int right = ctx.screenWidth / 2 + 91;
        int y = ctx.screenHeight - 39;

        // Current saturation: gold bar over the hunger icons, like AppleSkin.
        if (showSaturation.isEnabled() && saturation > 0.01f) {
            int points = (int) Math.ceil(Math.min(20f, saturation));
            for (int k = 0; k < points; k++) {
                int icon = k / 2;
                int x = right - icon * 8 - 9;
                boolean rightHalf = k % 2 == 0;
                int px = rightHalf ? x + 4 : x + 1;
                screenFill(ctx, px, y + 5, px + 3, y + 8, 0xCCF6C445);
            }
        }

        if (!foodBarOverlay.isEnabled()) {
            return;
        }
        var held = player.getMainHandItem().get(DataComponents.FOOD);
        if (held == null) {
            held = player.getOffhandItem().get(DataComponents.FOOD);
        }
        if (held == null) {
            return;
        }
        // Flash between 0.35 and 0.9 so restored food is visible at a glance.
        float flash = 0.62f + 0.28f * (float) Math.sin(System.currentTimeMillis() / 190.0);
        int alpha = Math.round(flash * 255f);
        int restoreHunger = Math.max(0, Math.min(held.nutrition(), 20 - hunger));
        float restoreSaturation = held.saturation() * held.nutrition() * 2f;

        // Hunger: one small white marker per restored half-shank.
        for (int k = 0; k < restoreHunger; k++) {
            int point = hunger + k;
            if (point >= 20) {
                break;
            }
            int icon = point / 2;
            int x = right - icon * 8 - 9;
            boolean rightHalf = point % 2 == 0;
            int px = rightHalf ? x + 4 : x + 1;
            screenFill(ctx, px, y + 1, px + 3, y + 8, (alpha << 24) | 0xFFFFFF);
        }

        // Saturation: gold strip under the icons the value would fill.
        int satPoints = Math.max(0, Math.round(restoreSaturation));
        for (int k = 0; k < satPoints; k++) {
            int point = (int) Math.floor(saturation) + k;
            if (point < 0 || point >= 20) {
                break;
            }
            int icon = point / 2;
            int x = right - icon * 8 - 9;
            screenFill(ctx, x + 1, y + 6, x + 8, y + 8, (alpha << 24) | 0xF6C445);
        }
    }

    /** Fills a rectangle given in real screen pixels, undoing the module's HUD scale. */
    private void screenFill(HudDrawContext ctx, float x1, float y1, float x2, float y2, int argb) {
        float s = sizeSetting().getFloat();
        if (s <= 0.01f) {
            return;
        }
        float ax = this.x;
        float ay = this.y;
        ctx.fill(ax + (x1 - ax) / s, ay + (y1 - ay) / s,
                ax + (x2 - ax) / s, ay + (y2 - ay) / s, argb);
    }
}
