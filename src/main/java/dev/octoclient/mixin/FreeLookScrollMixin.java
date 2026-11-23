package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.utility.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class FreeLookScrollMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void octoclient$scrollFreeLookDistance(long window, double horizontal, double vertical,
                                                    CallbackInfo ci) {
        FreeLook freeLook = ModuleManager.get().byName("Free Look", FreeLook.class).orElse(null);
        Minecraft client = Minecraft.getInstance();
        boolean inventoryOpen = client.gui.screen() instanceof AbstractContainerScreen<?>;
        if (freeLook == null || !freeLook.isFreeLooking() || !freeLook.scrollDistanceEnabled()
                || vertical == 0.0 || (client.gui.screen() != null
                && (!inventoryOpen || freeLook.blockInventoryScrolling()))) {
            return;
        }
        freeLook.changeDistance(vertical);
        ci.cancel();
    }
}
