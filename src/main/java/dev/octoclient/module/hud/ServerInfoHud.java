package dev.octoclient.module.hud;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.utility.StreamerMode;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public class ServerInfoHud extends HudModule {
    private final NumberSetting corners = new NumberSetting("Corners", "Widget corner radius", 10, 0, 24, 1);
    private final BooleanSetting dynamicPadding = new BooleanSetting("Dynamic Padding", "Size to the server name", true);
    private final NumberSetting panelWidth = new NumberSetting("Width", "Fixed panel width when dynamic padding is off", 180, 40, 500, 5);
    private final NumberSetting panelHeight = new NumberSetting("Height", "Fixed panel height when dynamic padding is off", 28, 12, 150, 2);
    private final ColorSetting color = new ColorSetting("Color", "Server label color", 0xFFE8ECF2);
    private final BooleanSetting showImage = new BooleanSetting("Show Image", "Show the server icon", false);
    private final NumberSetting imageSize = new NumberSetting("Image Size", "Server icon size in pixels", 20, 8, 48, 1);
    private Identifier serverIcon;
    private int serverIconHash;

    public ServerInfoHud() {
        super("Server Info", "Show server name and IP", 'S');
        addSetting(corners);
        addSetting(dynamicPadding);
        addSetting(panelWidth);
        addSetting(panelHeight);
        addSetting(color);
        addSetting(showImage);
        addSetting(imageSize);
        this.x = 10;
        this.y = 100;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        float s = sizeSetting().getFloat();
        int pad = 6;
        
        Minecraft mc = Minecraft.getInstance();
        String serverName = "Singleplayer";
        if (mc.getCurrentServer() != null) {
            ServerData data = mc.getCurrentServer();
            serverName = data.name != null && !data.name.isEmpty() ? data.name : (data.ip != null ? data.ip : "Multiplayer");
        }
        
        // Streamer Mode masks the address but keeps the widget alive.
        StreamerMode streamer = ModuleManager.get().byName("Streamer Mode", StreamerMode.class).orElse(null);
        boolean hideServer = streamer != null && streamer.hidesServer() && mc.getCurrentServer() != null;
        if (hideServer) {
            serverName = "Hidden";
        }

        String text = serverName;
        int image = showImage.isEnabled() && !hideServer && mc.getCurrentServer() != null
                && serverIcon(ctx, mc.getCurrentServer()) != null ? imageSize.getInt() : 0;
        int textW = (int) (ctx.textWidth(text) * s);
        int textH = (int) (ctx.textHeight() * s);
        int contentWidth = textW + (image > 0 ? image + pad : 0);
        int naturalWidth = contentWidth + pad * 2;
        int naturalHeight = Math.max(textH, image) + pad * 2;
        this.width = dynamicPadding.isEnabled() ? naturalWidth : panelWidth.getInt();
        this.height = dynamicPadding.isEnabled() ? naturalHeight : panelHeight.getInt();

        drawPanel(ctx, (int) x, (int) y, (int) this.width, (int) this.height,
                (int) (corners.getFloat() * s), 0x8E97C4, 0.94f, 0f, 70);
        float textX = x + pad + image + (image > 0 ? pad : 0);
        float textY = y + Math.max(pad, (height - textH) / 2f);
        if (image > 0 && serverIcon != null) {
            ctx.gfx.blit(serverIcon, (int) x + pad, (int) y + pad, image, image, 0, 0, 1, 1);
        }
        ctx.drawTextScaled(text, textX, textY, color.getValue(), s, false);
    }

    private Identifier serverIcon(HudDrawContext ctx, ServerData data) {
        byte[] icon = data.getIconBytes();
        if (icon == null || icon.length == 0) {
            serverIcon = null;
            return null;
        }
        int hash = Arrays.hashCode(icon);
        if (serverIcon != null && hash == serverIconHash) {
            return serverIcon;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(icon)) {
            NativeImage image = NativeImage.read(input);
            serverIcon = Identifier.fromNamespaceAndPath("octoclient", "server_icon");
            ctx.mc.getTextureManager().register(serverIcon,
                    new DynamicTexture(() -> "OctoClient server icon", image));
            serverIconHash = hash;
            return serverIcon;
        } catch (IOException e) {
            serverIcon = null;
            System.err.println("[OctoClient] Could not load server icon: " + e.getMessage());
            return null;
        }
    }
}
