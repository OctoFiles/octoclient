package dev.octoclient.mixin;

import dev.octoclient.module.cosmetics.NameTextRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class NameTextRenderStateMixin implements NameTextRenderState {
    @Unique
    private Component octoclient$nameText;

    @Unique
    private boolean octoclient$nameTextAbove;

    @Override
    public Component octoclient$getNameText() {
        return octoclient$nameText;
    }

    @Override
    public boolean octoclient$isNameTextAbove() {
        return octoclient$nameTextAbove;
    }

    @Override
    public void octoclient$setNameText(Component text, boolean above) {
        octoclient$nameText = text;
        octoclient$nameTextAbove = above;
    }
}
