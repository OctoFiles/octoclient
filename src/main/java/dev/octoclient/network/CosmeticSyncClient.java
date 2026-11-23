package dev.octoclient.network;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.cosmetics.Aura;
import dev.octoclient.module.cosmetics.Accessory;
import dev.octoclient.module.cosmetics.Bandana;
import dev.octoclient.module.cosmetics.CatalogCosmetic;
import dev.octoclient.module.cosmetics.Cape;
import dev.octoclient.module.cosmetics.CosmeticCatalog;
import dev.octoclient.module.cosmetics.Halo;
import dev.octoclient.module.cosmetics.Hat;
import dev.octoclient.module.cosmetics.HatDesign;
import dev.octoclient.module.cosmetics.Wings;
import dev.octoclient.module.render.Nametags;
import dev.octoclient.network.CosmeticPayloads.CosmeticStatePayload;
import dev.octoclient.network.CosmeticPayloads.CosmeticUpdatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of which cosmetics every player on the server is wearing, so the
 * render layers can draw them on other players too instead of only on you.
 */
public final class CosmeticSyncClient {
    private static final Map<UUID, CosmeticLoadout> REMOTE = new ConcurrentHashMap<>();
    private static volatile CosmeticLoadout local = CosmeticLoadout.EMPTY;
    private static CosmeticLoadout lastSent;
    private static boolean initialized;

    private CosmeticSyncClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientPlayNetworking.registerGlobalReceiver(CosmeticPayloads.STATE_TYPE, (payload, context) ->
                context.client().execute(() -> updateRemote(payload)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> lastSent = null);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            REMOTE.clear();
            lastSent = null;
            CosmeticApi.reset();
        });

        ClientTickEvents.END_CLIENT_TICK.register(CosmeticSyncClient::tick);
    }

    /** The cosmetics you have selected right now. */
    public static CosmeticLoadout localLoadout() {
        return local;
    }

    /**
     * The cosmetics another player reported, or {@link CosmeticLoadout#EMPTY}.
     *
     * <p>The server relay wins when it has an answer, because it is the freshest
     * source; otherwise the shared API is consulted, which is what makes this
     * work on servers that do not run the mod.</p>
     */
    public static CosmeticLoadout remoteLoadout(UUID playerId) {
        CosmeticLoadout relayed = REMOTE.get(playerId);
        if (relayed != null) {
            return relayed;
        }
        CosmeticLoadout shared = CosmeticApi.cached(playerId);
        return shared == null ? CosmeticLoadout.EMPTY : shared;
    }

    private static void tick(Minecraft client) {
        local = buildLocalLoadout();
        CosmeticApi.tick(client);

        if (client.player == null || client.getConnection() == null) {
            lastSent = null;
            return;
        }
        if (local.equals(lastSent)) {
            return;
        }
        if (ClientPlayNetworking.canSend(CosmeticPayloads.UPDATE_TYPE)) {
            ClientPlayNetworking.send(new CosmeticUpdatePayload(local.encode()));
        }
        lastSent = local;
    }

    private static CosmeticLoadout buildLocalLoadout() {
        List<Module> modules = ModuleManager.get().getModules();
        String catalogSelection = CosmeticCatalog.encodeSelection(modules.stream()
                .filter(CatalogCosmetic.class::isInstance)
                .map(CatalogCosmetic.class::cast)
                .filter(Module::isEnabled)
                .map(CatalogCosmetic::entry)
                .toList());

        Cape selectedCape = modules.stream()
                .filter(Cape.class::isInstance)
                .map(Cape.class::cast)
                .filter(Module::isEnabled)
                .findFirst()
                .orElse(null);
        String cape = selectedCape == null ? "" : selectedCape.fileName();
        String capeSettings = selectedCape == null ? "" : CapeSettings.from(selectedCape).encode();

        String bandana = modules.stream()
                .filter(Bandana.class::isInstance)
                .map(Bandana.class::cast)
                .filter(Module::isEnabled)
                .map(Bandana::getName)
                .findFirst()
                .orElse("");

        String wings = modules.stream()
                .filter(Wings.class::isInstance)
                .map(Wings.class::cast)
                .filter(Module::isEnabled)
                .map(Wings::designName)
                .findFirst()
                .orElse("");

        Accessory selectedAccessory = modules.stream()
                .filter(Accessory.class::isInstance)
                .map(Accessory.class::cast)
                .filter(Module::isEnabled)
                .findFirst()
                .orElse(null);
        String accessory = selectedAccessory == null ? "" : selectedAccessory.designName();
        String minimeSkinUser = selectedAccessory == null ? "" : selectedAccessory.minecraftUser();

        Hat hat = ModuleManager.get().byName("Hat", Hat.class).orElse(null);
        HatDesign hatDesign = modules.stream()
                .filter(HatDesign.class::isInstance)
                .map(HatDesign.class::cast)
                .filter(Module::isEnabled)
                .findFirst()
                .orElse(null);
        Halo halo = ModuleManager.get().byName("Halo", Halo.class).orElse(null);
        Aura aura = modules.stream()
                .filter(Aura.class::isInstance)
                .map(Aura.class::cast)
                .filter(Module::isEnabled)
                .findFirst()
                .orElse(null);

        return new CosmeticLoadout(
                bandana,
                wings,
                (hat != null && hat.isEnabled()) || hatDesign != null,
                hat == null ? 0 : hat.color.getValue(),
                halo != null && halo.isEnabled(),
                halo == null ? 0 : halo.color.getValue(),
                aura != null && aura.isEnabled(),
                aura == null ? 0 : aura.color.getValue(),
                aura == null ? CosmeticLoadout.DEFAULT_AURA_SHAPE : aura.designName(),
                cape,
                accessory,
                hatDesign == null ? "" : hatDesign.designName(),
                minimeSkinUser,
                catalogSelection,
                Nametags.isEnabledForSync(), Nametags.showHealthForSync(), Nametags.showDistanceForSync(),
                Nametags.colorForSync(), Nametags.animationForSync(), Nametags.animationHeightForSync(),
                capeSettings);
    }

    private static void updateRemote(CosmeticStatePayload payload) {
        if (payload.loadout().isEmpty()) {
            REMOTE.remove(payload.playerId());
            return;
        }
        CosmeticLoadout loadout = CosmeticLoadout.decode(payload.loadout());
        if (loadout.isEmpty()) {
            REMOTE.remove(payload.playerId());
        } else {
            REMOTE.put(payload.playerId(), loadout);
        }
    }
}
