package dev.octoclient.module;

import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.Setting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.ActionSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Base class for every client module (HUD, Render or Utility). */
public abstract class Module {
    private final String name;
    private final String description;
    private ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private boolean favorited;

    /** Fired when the module is toggled (enabled or disabled). */
    private Consumer<Module> onToggle = m -> {};

    /**
     * Corner rounding of this module, in GUI pixels: the card in the dashboard
     * and the panel the module draws on the HUD share it, so a widget keeps the
     * same shape in both places.
     */
    private final NumberSetting roundness =
            new NumberSetting("Corner Radius", "How round this module is", 10, 0, 24, 1);
    private final KeybindSetting toggleKey =
            new KeybindSetting("Toggle Key", "Press this key to toggle the module", 0);
    /** Card look of this module; {@code Default} follows the global Module Style. */
    private final ModeSetting cardStyle = new ModeSetting("Card Style",
            "Card look for this module only", "Default",
            List.of("Default", "Glass", "Clear",
                    dev.octoclient.module.settings.ClientSettings.STYLE_FROSTED, "Solid"));
    private boolean toggleKeyWasDown;

    /** Reset row shown at the bottom of every module's settings. */
    private final ActionSetting resetAction = new ActionSetting("Reset",
            "Restore every setting of this module to its default", "Reset", this::resetToDefaults);

    /** Icon glyph drawn on the card, single unicode char. */
    private final char icon;

    /** Real Minecraft item used for the dashboard card icon. */
    private net.minecraft.world.item.Item iconItem;

    /** Lazily created stack (ItemStack cannot be built during mod init: components not bound yet). */
    private net.minecraft.world.item.ItemStack iconStack;

    protected Module(String name, String description, ModuleCategory category, char icon) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        addSetting(roundness);
        addSetting(toggleKey);
        addSetting(cardStyle);
    }

    /** Convenience constructor inferring the category from the module's package (lazy). */
    protected Module(String name, String description, char icon) {
        this.name = name;
        this.description = description;
        this.category = null;
        this.icon = icon;
        addSetting(roundness);
        addSetting(toggleKey);
        addSetting(cardStyle);
    }

    private static ModuleCategory categoryFor(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        if (pkg.endsWith(".hud")) {
            return ModuleCategory.HUD;
        }
        if (pkg.endsWith(".render")) {
            return ModuleCategory.RENDER;
        }
        if (pkg.endsWith(".cosmetics")) {
            return ModuleCategory.COSMETICS;
        }
        if (pkg.endsWith(".utility")) {
            return ModuleCategory.UTILITY;
        }
        if (pkg.endsWith(".secret")) {
            return ModuleCategory.SECRET;
        }
        return ModuleCategory.SETTINGS;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        if (category == null) {
            category = categoryFor(getClass());
        }
        return category;
    }

    public char getIcon() {
        return icon;
    }

    /** Dashboard card icon; the stack is created on first use (after game bootstrap). */
    public net.minecraft.world.item.ItemStack getIconItem() {
        if (iconStack == null && iconItem != null) {
            try {
                iconStack = new net.minecraft.world.item.ItemStack(iconItem);
            } catch (Exception e) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        }
        return iconStack == null ? net.minecraft.world.item.ItemStack.EMPTY : iconStack;
    }

    /** Assigns a real item texture as the module's dashboard icon (stack created lazily). */
    protected Module withIconItem(net.minecraft.world.item.Item item) {
        this.iconItem = item;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFavorited() {
        return favorited;
    }

    /** Corner rounding setting shared by the dashboard card and the HUD panel. */
    public NumberSetting roundness() {
        return roundness;
    }

    public KeybindSetting toggleKeySetting() {
        return toggleKey;
    }

    /** Effective card style of this module (Glass, Clear or Solid). */
    public String effectiveCardStyle() {
        String own = cardStyle.getValue();
        if (own == null || own.equals("Default")) {
            return dev.octoclient.module.settings.ClientSettings.moduleStyle();
        }
        return own;
    }

    /** The "Reset" action row appended to this module's settings list. */
    public ActionSetting resetAction() {
        return resetAction;
    }

    /** Restores every setting of this module to its default value. */
    public void resetToDefaults() {
        for (Setting<?> setting : settings) {
            setting.reset();
        }
        dev.octoclient.config.ConfigManager.save();
    }

    /** Poll an optional module toggle binding, ignoring key repeats while held. */
    public void pollToggleKey(net.minecraft.client.Minecraft client, boolean allowToggle) {
        int key = toggleKey.getKeyCode();
        boolean down = key != 0 && org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().handle(), key)
                == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (down && !toggleKeyWasDown && allowToggle) {
            toggle();
        }
        toggleKeyWasDown = down;
    }

    /**
     * Effective corner radius in GUI pixels. The global Module Corners setting
     * can flatten every module at once without changing the saved Radius value,
     * so switching back restores each module's own rounding.
     */
    public int cornerRadiusPx() {
        if (dev.octoclient.module.settings.ClientSettings.modulesSquare()) {
            return 0;
        }
        return roundness.getInt();
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /** Settings persisted in config; modules may include editor-only hidden settings. */
    public List<Setting<?>> getConfigSettings() {
        return getSettings();
    }

    public void setOnToggle(Consumer<Module> onToggle) {
        this.onToggle = onToggle;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        onToggle.accept(this);
    }

    protected void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    protected void removeSetting(Setting<?> setting) {
        settings.remove(setting);
    }

    /** Typed settings lookup by name. */
    @SuppressWarnings("unchecked")
    public <T extends Setting<?>> T setting(String name, Class<T> type) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equals(name)) {
                return type.cast(setting);
            }
        }
        throw new IllegalStateException("Module " + this.name + " has no setting named " + name);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Modules that implement this get polled every client tick while enabled. */
    public interface Tickable {
        void onTick(net.minecraft.client.Minecraft client);
    }
}
