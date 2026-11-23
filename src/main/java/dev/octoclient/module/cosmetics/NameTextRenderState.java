package dev.octoclient.module.cosmetics;

import net.minecraft.network.chat.Component;

public interface NameTextRenderState {
    Component octoclient$getNameText();

    boolean octoclient$isNameTextAbove();

    void octoclient$setNameText(Component text, boolean above);
}
