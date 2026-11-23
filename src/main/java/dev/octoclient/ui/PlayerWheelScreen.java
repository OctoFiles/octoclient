package dev.octoclient.ui;

import dev.octoclient.module.hud.Friends;
import dev.octoclient.module.utility.Emotes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PlayerWheelScreen extends Screen {
    private final String playerName;
    private final Screen parent;

    public PlayerWheelScreen(String playerName, Screen parent) {
        super(Component.literal("Player Actions"));
        this.playerName = playerName;
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x99000000);
        int cx = width / 2;
        int cy = height / 2;
        graphics.centeredText(font, Component.literal("Player: " + playerName), cx, cy - 100, 0xFFFFFFFF);
        drawChoice(graphics, cx, cy - 55, Friends.isFriend(playerName) ? "Remove Friend" : "Add Friend", 0xFF344C3B);
        drawChoice(graphics, cx + 82, cy, "Invite (copy)", 0xFF34405B);
        drawChoice(graphics, cx, cy + 55, "Emote", 0xFF5A3F66);
        drawChoice(graphics, cx - 82, cy, "Copy Name", 0xFF5C4B32);
        graphics.centeredText(font, Component.literal("Right-click a player name to open this menu"),
                cx, cy + 94, 0xFFB8BBC8);
    }

    private void drawChoice(GuiGraphicsExtractor graphics, int cx, int cy, String label, int color) {
        graphics.fill(cx - 52, cy - 12, cx + 52, cy + 12, color);
        graphics.centeredText(font, label, cx, cy - 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        int x = (int) event.x();
        int y = (int) event.y();
        int cx = width / 2;
        int cy = height / 2;
        if (inside(x, y, cx, cy - 55)) {
            boolean wasFriend = Friends.isFriend(playerName);
            boolean changed = Friends.toggleFriend(playerName);
            showMessage(changed ? (wasFriend ? "Removed " : "Added ") + playerName + (wasFriend ? " from" : " to") + " friends."
                    : "Friend list is full or the name is invalid.");
        } else if (inside(x, y, cx + 82, cy)) {
            var server = Minecraft.getInstance().getCurrentServer();
            String address = server == null ? null : server.ip;
            Minecraft.getInstance().keyboardHandler.setClipboard(Friends.invitationText(playerName, address));
            showMessage("Invitation text copied to clipboard.");
        } else if (inside(x, y, cx, cy + 55)) {
            showMessage(Emotes.playSelected() ? "Emote started." : "Enable Emotes to play a pose.");
        } else if (inside(x, y, cx - 82, cy)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(playerName);
            showMessage("Player name copied.");
        } else {
            onClose();
        }
        return true;
    }

    private boolean inside(int x, int y, int cx, int cy) {
        return x >= cx - 52 && x <= cx + 52 && y >= cy - 12 && y <= cy + 12;
    }

    private void showMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
