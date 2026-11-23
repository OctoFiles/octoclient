package dev.octoclient.module.cosmetics;

import dev.octoclient.network.CosmeticLoadout;

public interface CosmeticPreviewState {
    boolean octoclient$isCosmeticPreview();

    void octoclient$setCosmeticPreview(boolean preview);

    CosmeticLoadout octoclient$getCosmeticPreviewLoadout();

    void octoclient$setCosmeticPreviewLoadout(CosmeticLoadout loadout);
}
