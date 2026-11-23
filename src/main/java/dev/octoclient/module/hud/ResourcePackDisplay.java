package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.PackResources;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shows the selected resource packs currently active in the client. */
public class ResourcePackDisplay extends HudModule {
    private final Map<String, Identifier> packIcons = new HashMap<>();
    private final Set<String> missingIcons = new HashSet<>();
    private final ModeSetting view = new ModeSetting("View", "Show the top pack or all selected packs",
            "Top Pack", List.of("Top Pack", "All Active"));
    private final NumberSetting maxLines = new NumberSetting("Max Lines", "Maximum pack names shown", 4, 1, 10, 1);
    private final BooleanSetting showIds = new BooleanSetting("Show IDs", "Append internal pack identifiers", false);
    private final BooleanSetting dynamicPadding = new BooleanSetting("Dynamic Padding", "Size to the displayed text", true);
    private final NumberSetting widthSetting = new NumberSetting("Width", "Panel width when dynamic padding is off", 180, 40, 500, 5);
    private final NumberSetting heightSetting = new NumberSetting("Height", "Panel height when dynamic padding is off", 30, 12, 300, 2);
    private final ColorSetting color = new ColorSetting("Color", "Resource pack text color", 0xFFE8ECF2);
    private final BooleanSetting autoMode = new BooleanSetting("Auto Mode", "Position the widget automatically", false);
    private final ModeSetting autoPosition = new ModeSetting("Auto Mode Position", "Automatic screen corner",
            "Top Left", List.of("Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final BooleanSetting showImage = new BooleanSetting("Show Image", "Show the selected pack's pack.png icon", false);
    private final BooleanSetting showDescription = new BooleanSetting("Show Description", "Show the pack description", false);
    private final NumberSetting imageSize = new NumberSetting("Image Size", "Pack icon size in pixels", 24, 8, 64, 1);
    private final TextSetting customName = new TextSetting("Custom Name", "Optional replacement title", "", 64);
    private final BooleanSetting useCustomName = new BooleanSetting("Use Custom Name", "Use the custom title instead of the pack name", false);

    public ResourcePackDisplay() {
        super("Resource Pack Display", "Show currently enabled resource packs", 'R');
        addSetting(view);
        addSetting(maxLines);
        addSetting(showIds);
        addSetting(dynamicPadding);
        addSetting(widthSetting);
        addSetting(heightSetting);
        addSetting(color);
        addSetting(autoMode);
        addSetting(autoPosition);
        addSetting(showImage);
        addSetting(showDescription);
        addSetting(imageSize);
        addSetting(customName);
        addSetting(useCustomName);
        x = 10;
        y = 140;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        updateAutoPosition(ctx);
        List<Pack> selected = new ArrayList<>(ctx.mc.getResourcePackRepository().getSelectedPacks());
        selected.removeIf(pack -> pack.getId().equals("vanilla") || pack.getId().equals("fabric"));
        if (selected.isEmpty()) {
            drawLines(ctx, List.of("Resource Pack: Default"), null);
            return;
        }
        if (view.getValue().equals("Top Pack")) {
            Pack pack = selected.get(0);
            String title = title(pack);
            if (showIds.isEnabled()) {
                title += " [" + pack.getId() + "]";
            }
            List<String> lines = new ArrayList<>();
            lines.add(title);
            if (showDescription.isEnabled()) {
                lines.add(pack.getDescription().getString());
            }
            drawLines(ctx, lines, pack);
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add(useCustomName.isEnabled() && !customName.getValue().isBlank()
                ? customName.getValue() : "Resource Packs");
        for (Pack pack : selected.stream().limit(maxLines.getInt()).toList()) {
        String line = pack.getTitle().getString();
        if (showIds.isEnabled()) {
            line += " [" + pack.getId() + "]";
        }
        lines.add(line);
        if (showDescription.isEnabled()) {
            lines.add(pack.getDescription().getString());
        }
        }
        drawLines(ctx, lines, selected.get(0));
    }

    private String title(Pack pack) {
        String name = useCustomName.isEnabled() && !customName.getValue().isBlank()
            ? customName.getValue() : pack.getTitle().getString();
        return "Resource Pack: " + name;
    }

    private void updateAutoPosition(HudDrawContext ctx) {
        if (!autoMode.isEnabled()) {
        return;
        }
        int margin = 8;
        switch (autoPosition.getValue()) {
        case "Top Right" -> { x = ctx.screenWidth - width - margin; y = margin; }
        case "Bottom Left" -> { x = margin; y = ctx.screenHeight - height - margin; }
        case "Bottom Right" -> { x = ctx.screenWidth - width - margin; y = ctx.screenHeight - height - margin; }
        default -> { x = margin; y = margin; }
        }
    }

    private void drawLines(HudDrawContext ctx, List<String> lines, Pack pack) {
        int image = showImage.isEnabled() && pack != null ? imageSize.getInt() : 0;
        int pad = 6;
        int lineHeight = ctx.textHeight() + 2;
        int contentWidth = lines.stream().mapToInt(ctx::textWidth).max().orElse(0);
        int naturalWidth = contentWidth + pad * 2 + (image > 0 ? image + pad : 0);
        int naturalHeight = Math.max(lines.size() * lineHeight + pad * 2, image + pad * 2);
        int panelWidth = dynamicPadding.isEnabled() ? naturalWidth : Math.max(widthSetting.getInt(), naturalWidth);
        int panelHeight = dynamicPadding.isEnabled() ? naturalHeight : Math.max(heightSetting.getInt(), naturalHeight);
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        int textX = (int) x + pad + image + (image > 0 ? pad : 0);
        if (image > 0) {
        Identifier icon = packIcon(ctx, pack);
        if (icon != null) {
            ctx.gfx.blit(icon, (int) x + pad, (int) y + pad, image, image, 0, 0, 1, 1);
        }
        }
        for (int i = 0; i < lines.size(); i++) {
        ctx.drawText(lines.get(i), textX, y + pad + i * lineHeight, color.getValue(), false);
        }
    }

    private Identifier packIcon(HudDrawContext ctx, Pack pack) {
        String packId = pack.getId();
        Identifier cached = packIcons.get(packId);
        if (cached != null || missingIcons.contains(packId)) {
        return cached;
        }
        try (PackResources resources = pack.open()) {
        var iconResource = resources.getRootResource("pack.png");
        if (iconResource == null) {
            missingIcons.add(packId);
            return null;
        }
        try (InputStream input = iconResource.get()) {
            NativeImage image = NativeImage.read(input);
            Identifier textureId = Identifier.fromNamespaceAndPath(
                    "octoclient", "pack_icons/" + Integer.toUnsignedString(packId.hashCode(), 16));
            ctx.mc.getTextureManager().register(textureId,
                    new DynamicTexture(() -> "OctoClient pack icon " + packId, image));
            packIcons.put(packId, textureId);
            return textureId;
        }
        } catch (IOException e) {
        missingIcons.add(packId);
        System.err.println("[OctoClient] Could not load resource pack icon for " + packId + ": " + e.getMessage());
        return null;
        }
    }
}
