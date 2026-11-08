package com.multiverseshinies;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells clients which Multiverse Infernapes are flying on the nimbus. The no-gravity flag can't be used for this:
 * Cobblemon resets it on the client every time the entity's pose type changes (walk -> stand), which made the
 * poser drop the cloud while he hovered.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NimbusNet {
    /** client side: ids of the entities currently flying (only touched by the payload handler and the pose driver) */
    public static final Set<Integer> FLYING = ConcurrentHashMap.newKeySet();

    public record FlyingPayload(int entityId, boolean flying) implements CustomPacketPayload {
        public static final Type<FlyingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "nimbus_flying"));
        public static final StreamCodec<ByteBuf, FlyingPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlyingPayload::entityId,
            ByteBufCodecs.BOOL, FlyingPayload::flying,
            FlyingPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private NimbusNet() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(FlyingPayload.TYPE, FlyingPayload.CODEC, (payload, context) -> {
            if (payload.flying()) FLYING.add(payload.entityId());
            else FLYING.remove(payload.entityId());
        });
    }

    static void send(Entity entity, boolean flying) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, new FlyingPayload(entity.getId(), flying));
    }
}
