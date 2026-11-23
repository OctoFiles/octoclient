package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.utility.StreamerMode;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(DebugEntryPosition.class)
public abstract class DebugCoordinatesMixin {
    @ModifyArg(
            method = "display",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;addToGroup(Lnet/minecraft/resources/Identifier;Ljava/util/Collection;)V"
            ),
            index = 1)
    private Collection<String> octo$maskCoordinates(Collection<String> lines) {
        StreamerMode streamerMode = ModuleManager.get().byName("Streamer Mode", StreamerMode.class).orElse(null);
        if (streamerMode == null || !streamerMode.masksF3Coordinates()) {
            return lines;
        }

        Collection<String> masked = new ArrayList<>(lines.size());
        for (String line : lines) {
            masked.add(streamerMode.maskF3CoordinateLine(line));
        }
        return masked;
    }
}
