package dev.octoclient.mixin;

import dev.octoclient.module.render.ItemHighlighter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class ItemHighlighterMixin {
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void octoclient$outlineMatchingItem(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                                                CallbackInfo ci) {
        if (ItemHighlighter.shouldHighlight(slot.getItem())) {
            graphics.outline(slot.x - 1, slot.y - 1, 18, 18, ItemHighlighter.highlightColor());
        }
    }
}
