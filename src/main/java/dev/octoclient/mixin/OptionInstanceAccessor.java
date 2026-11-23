package dev.octoclient.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * OptionInstance.set() validates values against the slider range and silently
 * drops out-of-range ones ("Illegal option value"). Fullbright needs gamma
 * values above the vanilla slider maximum, so this accessor writes the field
 * directly.
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor<T> {
    @Accessor("value")
    void octoclient$forceValue(T value);
}
