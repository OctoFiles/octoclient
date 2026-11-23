package dev.octoclient.mixin;

import dev.octoclient.module.render.ChatAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ChatComponent.class)
public class ChatAnimationsMixin {
    @ModifyArgs(
            method = "forEachLine",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;accept(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V"))
    private void octoclient$smoothChatEntrance(Args args) {
        GuiMessage.Line line = args.get(0);
        float alpha = args.get(2);
        int tick = Minecraft.getInstance().gui.hud.getGuiTicks();
        args.set(2, ChatAnimations.entranceAlpha(line.addedTime(), tick, alpha));
    }
}
