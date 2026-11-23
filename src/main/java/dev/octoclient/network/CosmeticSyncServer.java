package dev.octoclient.network;

import dev.octoclient.module.cosmetics.AuraDesigns;
import dev.octoclient.module.cosmetics.AccessoryDesigns;
import dev.octoclient.module.cosmetics.HatDesigns;
import dev.octoclient.module.cosmetics.BandanaDesigns;
import dev.octoclient.module.cosmetics.WingDesigns;
import dev.octoclient.module.cosmetics.CosmeticCatalog;
import dev.octoclient.network.CosmeticPayloads.CosmeticStatePayload;
import dev.octoclient.network.CosmeticPayloads.CosmeticUpdatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Relays each player's selected cosmetics to everyone else on the server.
 *
 * <p>Only OctoClient clients take part: the payloads are optional, so vanilla or
 * other modded clients simply never see them. The server never renders anything
 * itself; it just keeps the last loadout per player and broadcasts changes.</p>
 */
public final class CosmeticSyncServer {
    private static final Map<UUID, CosmeticLoadout> LOADOUTS = new HashMap<>();
    private static boolean initialized;

    private CosmeticSyncServer() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerPlayNetworking.registerGlobalReceiver(CosmeticPayloads.UPDATE_TYPE, (payload, context) ->
                context.server().execute(() -> update(context.server(), context.player(), payload.loadout())));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            for (Map.Entry<UUID, CosmeticLoadout> entry : LOADOUTS.entrySet()) {
                send(handler.player, new CosmeticStatePayload(entry.getKey(), entry.getValue().encode()));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();
            if (LOADOUTS.remove(playerId) != null) {
                broadcast(server, new CosmeticStatePayload(playerId, ""));
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> LOADOUTS.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LOADOUTS.clear());
    }

    private static void update(MinecraftServer server, ServerPlayer player, String rawLoadout) {
        CosmeticLoadout loadout = CosmeticLoadout.decode(rawLoadout);
        if (!isValid(loadout)) {
            System.err.println("[OctoClient] Rejected invalid cosmetic sync from "
                    + player.getGameProfile().name());
            return;
        }

        UUID playerId = player.getUUID();
        if (loadout.isEmpty()) {
            if (LOADOUTS.remove(playerId) == null) {
                return;
            }
        } else {
            LOADOUTS.put(playerId, loadout);
        }
        broadcast(server, new CosmeticStatePayload(playerId, loadout.isEmpty() ? "" : loadout.encode()));
    }

    /** Designs must be ones this build actually ships, otherwise nothing renders. */
    private static boolean isValid(CosmeticLoadout loadout) {
        if (!loadout.bandana().isEmpty() && !BandanaDesigns.names().contains(loadout.bandana())) {
            return false;
        }
        if (!loadout.wings().isEmpty() && !WingDesigns.names().contains(loadout.wings())) {
            return false;
        }
        if (!loadout.accessory().isEmpty() && !AccessoryDesigns.names().contains(loadout.accessory())) {
            return false;
        }
        if (!loadout.hatDesign().isEmpty() && !HatDesigns.names().contains(loadout.hatDesign())) {
            return false;
        }
        if (!CosmeticCatalog.isValidSelection(loadout.catalogSelection())) {
            return false;
        }
        if (!loadout.minimeSkinUser().isEmpty()
                && (!loadout.minimeSkinUser().matches("[A-Za-z0-9_]{1,16}")
                || !AccessoryDesigns.usesMinecraftSkin(loadout.accessory()))) {
            return false;
        }
        return !loadout.aura() || AuraDesigns.isValid(loadout.auraShape());
    }

    private static void broadcast(MinecraftServer server, CosmeticStatePayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    private static void send(ServerPlayer player, CosmeticStatePayload payload) {
        if (ServerPlayNetworking.canSend(player, CosmeticPayloads.STATE_TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
