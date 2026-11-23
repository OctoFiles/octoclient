package dev.octoclient.network;

import dev.octoclient.network.CapePayloads.CapeStatePayload;
import dev.octoclient.network.CapePayloads.CapeUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapeSyncServer {
    private static final Set<String> ALLOWED_CAPES = Set.of(
            "68f25dab817305fac760fc8e.gif",
            "68f25e6a817305fac760fed9.gif",
            "68f2800084c544a757d23ea1.gif",
            "68f30a718ac4a645dc51ccaf.png",
            "68fe6fbe8ac4a645dc529ee7.gif",
            "6a236be691b47a1fbef95266.gif",
            "skinmc-cape.png");

    private static final Map<UUID, CapeStatePayload> CAPES = new HashMap<>();
    private static boolean initialized;

    private CapeSyncServer() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerPlayNetworking.registerGlobalReceiver(CapePayloads.UPDATE_TYPE, (payload, context) ->
                context.server().execute(() -> update(context.server(), context.player(), payload)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            for (CapeStatePayload payload : CAPES.values()) {
                send(handler.player, payload);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();
            if (CAPES.remove(playerId) != null) {
                broadcast(server, new CapeStatePayload(playerId, "", CapeSettings.DEFAULT));
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> CAPES.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CAPES.clear());
    }

    private static void update(MinecraftServer server, ServerPlayer player, CapeUpdatePayload payload) {
        String capeId = payload.capeId();
        if (!capeId.isEmpty() && !ALLOWED_CAPES.contains(capeId)) {
            System.err.println("[OctoClient] Rejected invalid cape sync id from " + player.getGameProfile().name());
            return;
        }

        UUID playerId = player.getUUID();
        if (capeId.isEmpty()) {
            CAPES.remove(playerId);
        } else {
            CAPES.put(playerId, new CapeStatePayload(playerId, capeId, payload.settings()));
        }
        broadcast(server, new CapeStatePayload(playerId, capeId, payload.settings()));
    }

    private static void broadcast(MinecraftServer server, CapeStatePayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    private static void send(ServerPlayer player, CapeStatePayload payload) {
        if (ServerPlayNetworking.canSend(player, CapePayloads.STATE_TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
