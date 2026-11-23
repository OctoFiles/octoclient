package dev.octoclient.module.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.math.Axis;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.Setting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemModel extends Module {
    private static ItemModel instance;

    private final NumberSetting scale = new NumberSetting("Scale", "First-person item model size", 0.9, 0.5, 1.5, 0.05);
    private final NumberSetting offsetX = new NumberSetting("Offset X", "Horizontal item position", 0, -0.5, 0.5, 0.01);
    private final NumberSetting offsetY = new NumberSetting("Offset Y", "Vertical item position", 0, -0.5, 0.5, 0.01);
    private final NumberSetting offsetZ = new NumberSetting("Offset Z", "Depth item position", 0, -0.5, 0.5, 0.01);
    private final NumberSetting rotationX = new NumberSetting("Rotation X", "Item rotation around X", 0, -180, 180, 1);
    private final NumberSetting rotationY = new NumberSetting("Rotation Y", "Item rotation around Y", 0, -180, 180, 1);
    private final NumberSetting rotationZ = new NumberSetting("Rotation Z", "Item rotation around Z", 0, -180, 180, 1);
    private final TextSetting savedProfiles =
            new TextSetting("Item Profiles Data", "Persistent item transform profiles", "{}", 100_000);
    private final Map<String, ItemProfile> profiles = new LinkedHashMap<>();

    public ItemModel() {
        super("Item Model", "Customize first-person 3D item models", 'I');
        addSetting(scale);
        addSetting(offsetX);
        addSetting(offsetY);
        addSetting(offsetZ);
        addSetting(rotationX);
        addSetting(rotationY);
        addSetting(rotationZ);
        addSetting(new ActionSetting("Create Held Item Profile",
                "Add individual transform settings for the item in your hand",
                "Create Profile", this::createHeldItemProfile));
        addSetting(new ActionSetting("Remove Held Item Profile",
                "Remove individual transform settings for the item in your hand",
                "Remove Profile", this::removeHeldItemProfile));
        instance = this;
    }

    @Override
    public List<Setting<?>> getConfigSettings() {
        List<Setting<?>> settings = new ArrayList<>(super.getConfigSettings());
        settings.add(savedProfiles);
        return List.copyOf(settings);
    }

    public static void persistSavedProfiles() {
        if (instance != null) {
            instance.savedProfiles.setValue(instance.serializeProfiles());
        }
    }

    public static void loadSavedProfiles() {
        if (instance == null) {
            return;
        }
        instance.profiles.clear();
        try {
            JsonObject stored = JsonParser.parseString(instance.savedProfiles.getValue()).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : stored.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                instance.addProfile(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        } catch (RuntimeException error) {
            System.err.println("[OctoClient] Could not load item model profiles: " + error.getMessage());
        }
    }

    public static boolean shouldApply(ItemStack stack, ItemDisplayContext context) {
        return instance != null && instance.isEnabled() && !stack.isEmpty() && firstPerson(context);
    }

    public static void transform(ItemStack stack, PoseStack pose) {
        ItemProfile profile = instance.profiles.get(itemId(stack));
        NumberSetting scale = profile == null ? instance.scale : profile.scale;
        NumberSetting offsetX = profile == null ? instance.offsetX : profile.offsetX;
        NumberSetting offsetY = profile == null ? instance.offsetY : profile.offsetY;
        NumberSetting offsetZ = profile == null ? instance.offsetZ : profile.offsetZ;
        NumberSetting rotationX = profile == null ? instance.rotationX : profile.rotationX;
        NumberSetting rotationY = profile == null ? instance.rotationY : profile.rotationY;
        NumberSetting rotationZ = profile == null ? instance.rotationZ : profile.rotationZ;
        pose.translate(offsetX.getSnapped(), offsetY.getSnapped(), offsetZ.getSnapped());
        pose.mulPose(Axis.XP.rotationDegrees(rotationX.getFloat()));
        pose.mulPose(Axis.YP.rotationDegrees(rotationY.getFloat()));
        pose.mulPose(Axis.ZP.rotationDegrees(rotationZ.getFloat()));
        float size = scale.getFloat();
        pose.scale(size, size, size);
    }

    private void createHeldItemProfile() {
        ItemStack held = heldItem();
        if (held.isEmpty()) {
            System.err.println("[OctoClient] Hold an item before creating its item model profile");
            return;
        }
        String id = itemId(held);
        if (profiles.containsKey(id)) {
            return;
        }
        JsonObject defaults = new JsonObject();
        defaults.addProperty("Scale", scale.getValue());
        defaults.addProperty("Offset X", offsetX.getValue());
        defaults.addProperty("Offset Y", offsetY.getValue());
        defaults.addProperty("Offset Z", offsetZ.getValue());
        defaults.addProperty("Rotation X", rotationX.getValue());
        defaults.addProperty("Rotation Y", rotationY.getValue());
        defaults.addProperty("Rotation Z", rotationZ.getValue());
        addProfile(id, defaults);
        dev.octoclient.config.ConfigManager.save();
    }

    private void removeHeldItemProfile() {
        ItemStack held = heldItem();
        if (held.isEmpty()) {
            System.err.println("[OctoClient] Hold an item before removing its item model profile");
            return;
        }
        ItemProfile profile = profiles.remove(itemId(held));
        if (profile == null) {
            return;
        }
        profile.settings().forEach(this::removeSetting);
        dev.octoclient.config.ConfigManager.save();
    }

    private void addProfile(String id, JsonObject values) {
        String prefix = "Item " + id + " - ";
        ItemProfile profile = new ItemProfile(
                numberSetting(values, "Scale", scale, prefix + "Scale", "First-person item model size", 0.5, 1.5, 0.05),
                numberSetting(values, "Offset X", offsetX, prefix + "Offset X", "Horizontal item position", -0.5, 0.5, 0.01),
                numberSetting(values, "Offset Y", offsetY, prefix + "Offset Y", "Vertical item position", -0.5, 0.5, 0.01),
                numberSetting(values, "Offset Z", offsetZ, prefix + "Offset Z", "Depth item position", -0.5, 0.5, 0.01),
                numberSetting(values, "Rotation X", rotationX, prefix + "Rotation X", "Item rotation around X", -180, 180, 1),
                numberSetting(values, "Rotation Y", rotationY, prefix + "Rotation Y", "Item rotation around Y", -180, 180, 1),
                numberSetting(values, "Rotation Z", rotationZ, prefix + "Rotation Z", "Item rotation around Z", -180, 180, 1));
        profiles.put(id, profile);
        profile.settings().forEach(this::addSetting);
    }

    private static NumberSetting numberSetting(JsonObject values, String valueName, NumberSetting fallback,
                                               String name, String description, double min, double max, double step) {
        double value = values.has(valueName) ? values.get(valueName).getAsDouble() : fallback.getValue();
        return new NumberSetting(name, description, value, min, max, step);
    }

    private String serializeProfiles() {
        JsonObject stored = new JsonObject();
        profiles.forEach((id, profile) -> {
            JsonObject values = new JsonObject();
            values.addProperty("Scale", profile.scale.getValue());
            values.addProperty("Offset X", profile.offsetX.getValue());
            values.addProperty("Offset Y", profile.offsetY.getValue());
            values.addProperty("Offset Z", profile.offsetZ.getValue());
            values.addProperty("Rotation X", profile.rotationX.getValue());
            values.addProperty("Rotation Y", profile.rotationY.getValue());
            values.addProperty("Rotation Z", profile.rotationZ.getValue());
            stored.add(id, values);
        });
        return stored.toString();
    }

    private static ItemStack heldItem() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack mainHand = client.player.getMainHandItem();
        return mainHand.isEmpty() ? client.player.getOffhandItem() : mainHand;
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    static boolean firstPerson(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private record ItemProfile(NumberSetting scale, NumberSetting offsetX, NumberSetting offsetY,
                               NumberSetting offsetZ, NumberSetting rotationX, NumberSetting rotationY,
                               NumberSetting rotationZ) {
        private List<NumberSetting> settings() {
            return List.of(scale, offsetX, offsetY, offsetZ, rotationX, rotationY, rotationZ);
        }
    }
}
