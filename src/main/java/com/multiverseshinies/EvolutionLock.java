package com.multiverseshinies;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps an evolving Multiverse Pokemon where it started evolving for the whole Shenlong cutscene: Cobblemon stops its
 * navigation once when the evolution starts, but following the owner would drag it away from the dragon circling it.
 * While locked: no movement speed, navigation stopped, and put back on its spot every tick.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID)
public final class EvolutionLock {
    /** the cutscene lasts ~13.2 s from the start of the evolution; a little margin */
    private static final int LOCK_TICKS = 275;
    private static final ResourceLocation FREEZE = ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "evolution_lock");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("multiverseshinies/evolution_lock");

    private static final class Lock {
        final Vec3 pos;
        final float yaw;
        final long until;
        boolean released;
        Lock(Vec3 pos, float yaw, long until) { this.pos = pos; this.yaw = yaw; this.until = until; }
    }

    private static final Map<UUID, Lock> LOCKS = new HashMap<>();

    private EvolutionLock() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();

        // arm: a Multiverse Pokemon that just started evolving
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof PokemonEntity pe) || LOCKS.containsKey(pe.getUUID())) continue;
            boolean evolving;
            try { evolving = pe.isEvolving(); } catch (Throwable t) { continue; }
            if (!evolving || !pe.getAspects().contains("multiverseshinies")) continue;
            LOCKS.put(pe.getUUID(), new Lock(pe.position(), pe.getYRot(), now + LOCK_TICKS));
            LOG.info("Holding {} in place for the evolution cutscene at {}", pe.getPokemon().getSpecies().getName(), pe.position());
        }

        for (Iterator<Map.Entry<UUID, Lock>> it = LOCKS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Lock> en = it.next();
            Lock lock = en.getValue();
            Entity e = level.getEntity(en.getKey());
            if (!(e instanceof PokemonEntity pe)) {
                if (now > lock.until + 200) it.remove();      // gone (recalled, other dimension...)
                continue;
            }
            if (now > lock.until) {
                if (!lock.released) { release(pe); lock.released = true; }
                boolean evolving;
                try { evolving = pe.isEvolving(); } catch (Throwable t) { evolving = false; }
                if (!evolving) it.remove();                    // re-arm only once this evolution is over
                continue;
            }
            hold(pe, lock);
        }
    }

    private static void hold(PokemonEntity pe, Lock lock) {
        AttributeInstance speed = pe.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(FREEZE)) {
            speed.addTransientModifier(new AttributeModifier(FREEZE, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        pe.getNavigation().stop();
        pe.setDeltaMovement(Vec3.ZERO);
        if (pe.position().distanceToSqr(lock.pos) > 1.0e-4) {
            pe.teleportTo(lock.pos.x, lock.pos.y, lock.pos.z);
        }
        pe.setYRot(lock.yaw);
        pe.setYBodyRot(lock.yaw);
        pe.setYHeadRot(lock.yaw);
    }

    private static void release(PokemonEntity pe) {
        AttributeInstance speed = pe.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FREEZE);
    }
}
