package one.fayaz;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil; // IMPORT THIS
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class LockoutNetworking {

    public static final CustomPacketPayload.Type<LockoutSyncPayload> SYNC_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("deathlockout", "sync"));

    public record LockoutSyncPayload(int goal, int s1, int s2, UUID p1, UUID p2) implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, LockoutSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, LockoutSyncPayload::goal,
                ByteBufCodecs.INT, LockoutSyncPayload::s1,
                ByteBufCodecs.INT, LockoutSyncPayload::s2,
                UUIDUtil.STREAM_CODEC, LockoutSyncPayload::p1,
                UUIDUtil.STREAM_CODEC, LockoutSyncPayload::p2,
                LockoutSyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SYNC_TYPE;
        }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(SYNC_TYPE, LockoutSyncPayload.CODEC);
    }

    public static void broadcastState(MinecraftServer server, int goal, int s1, int s2, UUID p1, UUID p2) {
        if (server == null) return;
        // Default to random UUIDs if null (e.g. on reset), to prevent crashes
        UUID safeP1 = p1 == null ? UUID.randomUUID() : p1;
        UUID safeP2 = p2 == null ? UUID.randomUUID() : p2;

        LockoutSyncPayload payload = new LockoutSyncPayload(goal, s1, s2, safeP1, safeP2);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}