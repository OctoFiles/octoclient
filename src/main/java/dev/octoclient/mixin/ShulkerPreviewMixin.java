package dev.octoclient.mixin;

import dev.octoclient.module.hud.ShulkerPreview;
import dev.octoclient.ui.ShulkerPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ShulkerPreviewMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void octoclient$openShulkerPreview(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int code = ShulkerPreview.openKeyCode();
        if (code == 0 || event.key() != code || hoveredSlot == null) {
            return;
        }
        ItemStack stack = hoveredSlot.getItem();
        if (!ShulkerPreviewScreen.isShulker(stack)) {
            return;
        }
        Minecraft.getInstance().setScreenAndShow(
                new ShulkerPreviewScreen(stack, (Screen) (Object) this));
        cir.setReturnValue(true);
    }

    @Inject(method = "extractTooltip", at = @At("TAIL"))
    private void octoclient$drawShulkerPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                               CallbackInfo ci) {
        if (!ShulkerPreview.shouldPreview() || hoveredSlot == null) {
            return;
        }
        ItemStack shulker = hoveredSlot.getItem();
        if (!(shulker.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);
        int panelWidth = 9 * 18 + 12;
        int panelHeight = 3 * 18 + 28;
        int panelX = mouseX + 14;
        int panelY = mouseY + 12;
        if (panelX + panelWidth > graphics.guiWidth()) {
            panelX = mouseX - panelWidth - 8;
        }
        panelX = Math.max(2, Math.min(panelX, graphics.guiWidth() - panelWidth - 2));
        panelY = Math.max(2, Math.min(panelY, graphics.guiHeight() - panelHeight - 2));
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0101010);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFFB267FF);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, "Shulker Contents",
                panelX + 6, panelY + 5, 0xFFFFFFFF, true);
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item.isEmpty()) {
                continue;
            }
            int x = panelX + 6 + (i % 9) * 18;
            int y = panelY + 18 + (i / 9) * 18;
            graphics.item(item, x, y);
            if (ShulkerPreview.showCounts()) {
                graphics.itemDecorations(net.minecraft.client.Minecraft.getInstance().font, item, x, y);
            }
        }
    }
}
