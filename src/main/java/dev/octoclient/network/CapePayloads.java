package dev.octoclient.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class CapePayloads {
    private static final Identifier UPDATE_ID = Identifier.fromNamespaceAndPath("octoclient", "cape_update");
    private static final Identifier STATE_ID = Identifier.fromNamespaceAndPath("octoclient", "cape_state");

    public static final CustomPacketPayload.Type<CapeUpdatePayload> UPDATE_TYPE =
            new CustomPacketPayload.Type<>(UPDATE_ID);
    public static final CustomPacketPayload.Type<CapeStatePayload> STATE_TYPE =
            new CustomPacketPayload.Type<>(STATE_ID);

    private static void writeSettings(RegistryFriendlyByteBuf buf, CapeSettings settings) {
        buf.writeUtf((settings == null ? CapeSettings.DEFAULT : settings).encode(), 128);
    }

    private static CapeSettings readSettings(RegistryFriendlyByteBuf buf) {
        return CapeSettings.decode(buf.readUtf(128));
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, CapeUpdatePayload> UPDATE_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.capeId(), 64);
                        writeSettings(buf, payload.settings());
                    },
                    buf -> new CapeUpdatePayload(buf.readUtf(64), readSettings(buf)));
    private static final StreamCodec<RegistryFriendlyByteBuf, CapeStatePayload> STATE_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.playerId());
                        buf.writeUtf(payload.capeId(), 64);
                        writeSettings(buf, payload.settings());
                    },
                    buf -> new CapeStatePayload(buf.readUUID(), buf.readUtf(64), readSettings(buf)));

    private CapePayloads() {
    }

    public static void registerTypes() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay()
                .register(UPDATE_TYPE, UPDATE_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay()
                .register(STATE_TYPE, STATE_CODEC);
    }

    public record CapeUpdatePayload(String capeId, CapeSettings settings) implements CustomPacketPayload {
        public CapeUpdatePayload(String capeId) {
            this(capeId, CapeSettings.DEFAULT);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return UPDATE_TYPE;
        }
    }

    public record CapeStatePayload(UUID playerId, String capeId, CapeSettings settings) implements CustomPacketPayload {
        public CapeStatePayload(UUID playerId, String capeId) {
            this(playerId, capeId, CapeSettings.DEFAULT);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return STATE_TYPE;
        }
    }
}
