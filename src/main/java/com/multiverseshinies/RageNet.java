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
 * Tells clients which Multiverse Infernapes really have a stat raised in their battle (the server's battle state).
 * The rage visuals start from the boost animation's molang variable, which is client-only and sticks to the entity:
 * this lets the client end the rage when the boosts are gone (battle over, Haze, lowered back to 0...).
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RageNet {
    /** client side: ids of the entities currently buffed in battle */
    public static final Set<Integer> BUFFED = ConcurrentHashMap.newKeySet();
    /** client side: ids the server has said were buffed at some point (only these can be ended by the server's word) */
    public static final Set<Integer> CONFIRMED = ConcurrentHashMap.newKeySet();

    public record BuffedPayload(int entityId, boolean buffed) implements CustomPacketPayload {
        public static final Type<BuffedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "rage_buffed"));
        public static final StreamCodec<ByteBuf, BuffedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BuffedPayload::entityId,
            ByteBufCodecs.BOOL, BuffedPayload::buffed,
            BuffedPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private RageNet() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(BuffedPayload.TYPE, BuffedPayload.CODEC, (payload, context) -> {
            if (payload.buffed()) { BUFFED.add(payload.entityId()); CONFIRMED.add(payload.entityId()); }
            else BUFFED.remove(payload.entityId());
        });
    }

    static void send(Entity entity, boolean buffed) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, new BuffedPayload(entity.getId(), buffed));
    }
}
