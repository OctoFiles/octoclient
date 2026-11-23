package dev.octoclient;

import dev.octoclient.network.CapePayloads;
import dev.octoclient.network.CapeSyncServer;
import dev.octoclient.network.CosmeticPayloads;
import dev.octoclient.network.CosmeticSyncServer;
import net.fabricmc.api.ModInitializer;

public final class OctoClientCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        CapePayloads.registerTypes();
        CapeSyncServer.initialize();
        CosmeticPayloads.registerTypes();
        CosmeticSyncServer.initialize();
    }
}
