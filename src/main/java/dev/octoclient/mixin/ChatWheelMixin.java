package dev.octoclient.mixin;

import dev.octoclient.module.utility.WheelModule;
import dev.octoclient.ui.PlayerWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public abstract class ChatWheelMixin {
    private static final Pattern MESSAGE_TARGET = Pattern.compile("^/(?:msg|tell|w)\\s+([A-Za-z0-9_]{1,16})\\b");

    @Shadow
    private ChatComponent.DisplayMode displayMode;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void octoclient$openPlayerWheel(MouseButtonEvent event, boolean doubleClick,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !WheelModule.active()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(
                client.font, (int) event.x(), (int) event.y()).includeInsertions(true);
        client.gui.hud.getChat().captureClickableText(finder, client.getWindow().getGuiScaledHeight(),
                client.gui.hud.getGuiTicks(), displayMode);
        String playerName = playerName(finder.result());
        if (playerName != null) {
            client.setScreenAndShow(new PlayerWheelScreen(playerName, (Screen) (Object) this));
            cir.setReturnValue(true);
        }
    }

    private String playerName(Style style) {
        if (style == null) {
            return null;
        }
        String insertion = style.getInsertion();
        if (validPlayerName(insertion)) {
            return insertion;
        }
        HoverEvent hover = style.getHoverEvent();
        if (hover instanceof HoverEvent.ShowEntity entity && entity.entity().name.isPresent()) {
            String name = entity.entity().name.get().getString();
            if (validPlayerName(name)) {
                return name;
            }
        }
        ClickEvent click = style.getClickEvent();
        if (click instanceof ClickEvent.SuggestCommand suggest) {
            Matcher matcher = MESSAGE_TARGET.matcher(suggest.command());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private boolean validPlayerName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }
}
