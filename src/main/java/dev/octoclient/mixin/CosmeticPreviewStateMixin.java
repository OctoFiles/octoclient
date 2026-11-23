package dev.octoclient.mixin;

import dev.octoclient.module.cosmetics.CosmeticPreviewState;
import dev.octoclient.network.CosmeticLoadout;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class CosmeticPreviewStateMixin implements CosmeticPreviewState {
    @Unique
    private boolean octoclient$cosmeticPreview;

    @Unique
    private CosmeticLoadout octoclient$cosmeticPreviewLoadout;

    @Override
    public boolean octoclient$isCosmeticPreview() {
        return octoclient$cosmeticPreview;
    }

    @Override
    public void octoclient$setCosmeticPreview(boolean preview) {
        octoclient$cosmeticPreview = preview;
    }

    @Override
    public CosmeticLoadout octoclient$getCosmeticPreviewLoadout() {
        return octoclient$cosmeticPreviewLoadout;
    }

    @Override
    public void octoclient$setCosmeticPreviewLoadout(CosmeticLoadout loadout) {
        octoclient$cosmeticPreviewLoadout = loadout;
    }
}
