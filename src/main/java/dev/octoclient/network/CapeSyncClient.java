package dev.octoclient.network;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.cosmetics.Cape;
import dev.octoclient.network.CapePayloads.CapeStatePayload;
import dev.octoclient.network.CapePayloads.CapeUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapeSyncClient {
    private static final Map<UUID, CapeStatePayload> REMOTE_CAPES = new ConcurrentHashMap<>();
    private static String lastSentCape;
    private static CapeSettings lastSentSettings;
    private static boolean initialized;

    private CapeSyncClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientPlayNetworking.registerGlobalReceiver(CapePayloads.STATE_TYPE, (payload, context) ->
                context.client().execute(() -> updateRemoteCape(payload)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            lastSentCape = null;
            lastSentSettings = null;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            REMOTE_CAPES.clear();
            lastSentCape = null;
            lastSentSettings = null;
        });

        ClientTickEvents.END_CLIENT_TICK.register(CapeSyncClient::syncSelection);
    }

    public static String remoteCape(UUID playerId) {
        CapeStatePayload relayed = REMOTE_CAPES.get(playerId);
        if (relayed != null && !relayed.capeId().isEmpty()) {
            return relayed.capeId();
        }
        CosmeticLoadout shared = CosmeticApi.cached(playerId);
        if (shared != null && !shared.cape().isEmpty() && isValidCape(shared.cape())) {
            return shared.cape();
        }
        return null;
    }

    /** True when the wearer's cape settings were relayed by the server or API. */
    public static boolean hasRemoteSettings(UUID playerId) {
        CapeStatePayload relayed = REMOTE_CAPES.get(playerId);
        if (relayed != null && !relayed.capeId().isEmpty()) {
            return true;
        }
        CosmeticLoadout shared = CosmeticApi.cached(playerId);
        return shared != null && !shared.cape().isEmpty() && !shared.capeSettings().isEmpty();
    }

    /** The wearer's movement settings, falling back to the API loadout and then defaults. */
    public static CapeSettings remoteSettings(UUID playerId) {
        CapeStatePayload relayed = REMOTE_CAPES.get(playerId);
        if (relayed != null && !relayed.capeId().isEmpty()) {
            return relayed.settings();
        }
        CosmeticLoadout shared = CosmeticApi.cached(playerId);
        if (shared != null && !shared.cape().isEmpty() && !shared.capeSettings().isEmpty()) {
            return CapeSettings.decode(shared.capeSettings());
        }
        return CapeSettings.DEFAULT;
    }

    public static boolean isCapeVisible(String fileName) {
        if (isRemoteCape(fileName)) {
            return true;
        }
        return ModuleManager.get().getModules().stream()
                .filter(Cape.class::isInstance)
                .map(Cape.class::cast)
                .anyMatch(cape -> cape.isEnabled() && cape.fileName().equals(fileName));
    }

    private static boolean isRemoteCape(String fileName) {
        for (CapeStatePayload payload : REMOTE_CAPES.values()) {
            if (fileName.equals(payload.capeId())) {
                return true;
            }
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return false;
        }
        for (Player player : client.level.players()) {
            if (player != client.player) {
                CosmeticLoadout shared = CosmeticApi.cached(player.getUUID());
                if (shared != null && fileName.equals(shared.cape())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCapeMoving(String fileName) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return false;
        }
        for (Player player : client.level.players()) {
            boolean localCape = player == client.player && ModuleManager.get().getModules().stream()
                    .filter(Cape.class::isInstance)
                    .map(Cape.class::cast)
                    .anyMatch(cape -> cape.isEnabled() && cape.fileName().equals(fileName));
            boolean remoteCape = player != client.player
                    && fileName.equals(remoteCape(player.getUUID()));
            if ((localCape || remoteCape) && isEntityMoving(player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEntityMoving(Entity entity) {
        var velocity = entity.getDeltaMovement();
        double horizontalSpeedSqr = velocity.x * velocity.x + velocity.z * velocity.z;
        if (entity.onGround()) {
            return horizontalSpeedSqr > 0.0004;
        }
        return velocity.lengthSqr() > 0.0004;
    }

    private static void updateRemoteCape(CapeStatePayload payload) {
        if (payload.capeId().isEmpty()) {
            REMOTE_CAPES.remove(payload.playerId());
        } else if (isValidCape(payload.capeId())) {
            REMOTE_CAPES.put(payload.playerId(), payload);
        } else {
            System.err.println("[OctoClient] Ignored invalid cape sync id from server: " + payload.capeId());
        }
    }

    private static void syncSelection(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            lastSentCape = null;
            lastSentSettings = null;
            return;
        }

        Cape selected = ModuleManager.get().getModules().stream()
                .filter(Cape.class::isInstance)
                .map(Cape.class::cast)
                .filter(Cape::isEnabled)
                .findFirst()
                .orElse(null);
        String selectedCape = selected == null ? "" : selected.fileName();
        CapeSettings settings = selected == null ? CapeSettings.DEFAULT : CapeSettings.from(selected);
        if (selectedCape.equals(lastSentCape) && settings.equals(lastSentSettings)) {
            return;
        }

        if (ClientPlayNetworking.canSend(CapePayloads.UPDATE_TYPE)) {
            ClientPlayNetworking.send(new CapeUpdatePayload(selectedCape, settings));
        }
        lastSentCape = selectedCape;
        lastSentSettings = settings;
    }

    private static boolean isValidCape(String capeId) {
        return ModuleManager.get().getModules().stream()
                .filter(Cape.class::isInstance)
                .map(Cape.class::cast)
                .anyMatch(cape -> cape.fileName().equals(capeId));
    }
}
