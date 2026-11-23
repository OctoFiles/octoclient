package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;

public class NoAdvancement extends Module {
    private static NoAdvancement instance;

    private final BooleanSetting advancements = new BooleanSetting("Advancements", "Hide advancement notifications", true);
    private final BooleanSetting recipes = new BooleanSetting("Recipes", "Hide recipe unlock notifications", false);
    private final BooleanSetting system = new BooleanSetting("System", "Hide system notifications", false);
    private final BooleanSetting tutorial = new BooleanSetting("Tutorial", "Hide tutorial prompts", false);
    private final BooleanSetting nowPlaying = new BooleanSetting("Now Playing", "Hide music track notifications", false);

    public NoAdvancement() {
        super("NoAdvancement", "Choose which toast notifications to hide", 'A');
        addSetting(advancements);
        addSetting(recipes);
        addSetting(system);
        addSetting(tutorial);
        addSetting(nowPlaying);
        instance = this;
    }

    public static boolean shouldHide(Toast toast) {
        NoAdvancement module = instance;
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return (toast instanceof AdvancementToast && module.advancements.isEnabled())
                || (toast instanceof RecipeToast && module.recipes.isEnabled())
                || (toast instanceof SystemToast && module.system.isEnabled())
                || (toast instanceof TutorialToast && module.tutorial.isEnabled())
                || (toast instanceof NowPlayingToast && module.nowPlaying.isEnabled());
    }
}
