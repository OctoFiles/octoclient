package dev.octoclient.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Payloads that let OctoClient users see each other's cosmetics. */
public final class CosmeticPayloads {
    private static final Identifier UPDATE_ID =
            Identifier.fromNamespaceAndPath("octoclient", "cosmetic_update");
    private static final Identifier STATE_ID =
            Identifier.fromNamespaceAndPath("octoclient", "cosmetic_state");
    private static final int MAX_LENGTH = 1024;

    public static final CustomPacketPayload.Type<CosmeticUpdatePayload> UPDATE_TYPE =
            new CustomPacketPayload.Type<>(UPDATE_ID);
    public static final CustomPacketPayload.Type<CosmeticStatePayload> STATE_TYPE =
            new CustomPacketPayload.Type<>(STATE_ID);

    private static final StreamCodec<RegistryFriendlyByteBuf, CosmeticUpdatePayload> UPDATE_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.loadout(), MAX_LENGTH),
                    buf -> new CosmeticUpdatePayload(buf.readUtf(MAX_LENGTH)));
    private static final StreamCodec<RegistryFriendlyByteBuf, CosmeticStatePayload> STATE_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.playerId());
                        buf.writeUtf(payload.loadout(), MAX_LENGTH);
                    },
                    buf -> new CosmeticStatePayload(buf.readUUID(), buf.readUtf(MAX_LENGTH)));

    private CosmeticPayloads() {
    }

    public static void registerTypes() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay()
                .register(UPDATE_TYPE, UPDATE_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay()
                .register(STATE_TYPE, STATE_CODEC);
    }

    public record CosmeticUpdatePayload(String loadout) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return UPDATE_TYPE;
        }
    }

    public record CosmeticStatePayload(UUID playerId, String loadout) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return STATE_TYPE;
        }
    }
}
