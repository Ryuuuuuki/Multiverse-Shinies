package com.multiverseshinies;

import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server side: while a transforming Multiverse Pokemon (RageSpecies) is in battle with any stat raised (the rage), its
 * aura pushes away the players and creatures around it, strongest near it and fading towards the edge of the aura.
 * Pokemon that are themselves in battle are left alone so the fight's positions are not disturbed. It starts when the
 * rage animation bursts (RageSpecies.burstTicks after the buff), with a one-off shockwave that throws everything around
 * back. Golden species also get RageSpecies.GOLDEN_ASPECT while transformed (golden fur), taken away when it ends.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID)
public final class RageAuraPush {
    private static final double REACH = 1.7;      // radius, in Infernape heights (plus one block)
    private static final double STRENGTH = 0.11;  // velocity added per tick right next to him
    private static final double BLAST = 1.1;      // one-off shockwave when he bursts into the rage
    private static final double BLAST_REACH = 2.6;
    /** per dimension (each level ticks on its own): entity id -> game time it became buffed */
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Map<Integer, Long>> BUFFED_SINCE = new HashMap<>();
    /** buffed Infernapes clients were told about (RageNet), to tell them again when it's over */
    private static final Map<Integer, PokemonEntity> TOLD = new HashMap<>();

    private RageAuraPush() {}

    /** true when this entity is a transforming Multiverse Pokemon in battle with at least one stat raised */
    public static boolean isBuffed(PokemonEntity pe) {
        try {
            if (!pe.isBattling() || pe.getBattleId() == null) return false;
            if (RageSpecies.of(pe) == null) return false;
            PokemonBattle battle = BattleRegistry.getBattle(pe.getBattleId());
            if (battle == null) return false;
            for (ActiveBattlePokemon active : battle.getActivePokemon()) {
                BattlePokemon bp = active.getBattlePokemon();
                if (bp == null || bp.getEntity() != pe) continue;
                // BattlePokemon.statChanges is never updated by Cobblemon (it's only the battle's starting state);
                // the boosts live in the context manager: one BOOST / UNBOOST context per stage, id = the stat
                Map<String, Integer> net = new HashMap<>();
                var ctx = bp.getContextManager();
                var ups = ctx.get(BattleContext.Type.BOOST);
                var downs = ctx.get(BattleContext.Type.UNBOOST);
                if (ups != null) for (BattleContext c : ups) net.merge(c.getId(), 1, Integer::sum);
                if (downs != null) for (BattleContext c : downs) net.merge(c.getId(), -1, Integer::sum);
                for (int stage : net.values()) {
                    if (stage > 0) return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<PokemonEntity> raging = new ArrayList<>();
        List<PokemonEntity> bursting = new ArrayList<>();
        long now = level.getGameTime();
        Set<Integer> seen = new HashSet<>();
        Map<Integer, Long> buffedSince = BUFFED_SINCE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof PokemonEntity pe)) continue;
            if (!isBuffed(pe)) {
                // battle over, recalled and sent out again, Haze...
                if (hasAspect(pe, RageSpecies.GOLDEN_ASPECT)) setAspect(pe, RageSpecies.GOLDEN_ASPECT, false);
                if (hasAspect(pe, RageSpecies.RAGING_ASPECT)) setAspect(pe, RageSpecies.RAGING_ASPECT, false);
                continue;
            }
            RageSpecies sp = RageSpecies.of(pe);
            // already transformed: boost.json won't play the transformation again (a move raising two stats sends
            // two boosts; the second one would tap/roar and play the sound a second time)
            if (!hasAspect(pe, RageSpecies.RAGING_ASPECT)) setAspect(pe, RageSpecies.RAGING_ASPECT, true);
            seen.add(pe.getId());
            long since = buffedSince.computeIfAbsent(pe.getId(), k -> now);
            if (TOLD.put(pe.getId(), pe) == null || (now - since) % 20 == 0) RageNet.send(pe, true);
            if (now - since == sp.burstTicks) bursting.add(pe);
            if (now - since >= sp.burstTicks) {
                raging.add(pe);
                if (sp.golden && !hasAspect(pe, RageSpecies.GOLDEN_ASPECT)) setAspect(pe, RageSpecies.GOLDEN_ASPECT, true);
            }
        }
        buffedSince.keySet().removeIf(id -> !seen.contains(id));
        TOLD.entrySet().removeIf(en -> {
            if (seen.contains(en.getKey()) || en.getValue().level() != level) return false;
            if (!en.getValue().isRemoved()) RageNet.send(en.getValue(), false);
            return true;
        });
        for (PokemonEntity pe : bursting) blast(level, pe);
        for (PokemonEntity pe : raging) {
            double h = Math.max(0.5, pe.getBbHeight());
            double reach = h * REACH + 1.0;
            Vec3 c = pe.position();
            AABB box = new AABB(c.x - reach, c.y - 1, c.z - reach, c.x + reach, c.y + h * 2, c.z + reach);
            for (Entity target : level.getEntities(pe, box)) {
                if (!(target instanceof LivingEntity) || target.isSpectator()) continue;
                if (target instanceof PokemonEntity other && other.isBattling()) continue;
                if (target instanceof Player p && (p.isCreative() && p.getAbilities().flying)) continue;
                Vec3 d = target.position().subtract(c);
                double dist = Math.sqrt(d.x * d.x + d.z * d.z);
                if (dist > reach) continue;
                double k = STRENGTH * (1.0 - dist / reach);
                double nx = dist < 1e-3 ? 1 : d.x / dist, nz = dist < 1e-3 ? 0 : d.z / dist;
                target.push(nx * k, 0.01 * k / STRENGTH, nz * k);
                target.hurtMarked = true;                     // sync the new velocity to players
            }
        }
    }

    private static boolean hasAspect(PokemonEntity pe, String aspect) {
        try {
            return pe.getPokemon().getForcedAspects().contains(aspect);
        } catch (Throwable t) {
            return false;
        }
    }

    /** a temporary forced aspect on/off (golden fur for the Monferno resolver; "already raging" for boost.json) */
    private static void setAspect(PokemonEntity pe, String aspect, boolean on) {
        try {
            var pokemon = pe.getPokemon();
            Set<String> forced = new HashSet<>(pokemon.getForcedAspects());
            if (on ? forced.add(aspect) : forced.remove(aspect)) {
                pokemon.setForcedAspects(forced);
                pokemon.updateAspects();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void blast(ServerLevel level, PokemonEntity pe) {
        double h = Math.max(0.5, pe.getBbHeight());
        double reach = h * BLAST_REACH + 2.0;
        Vec3 c = pe.position();
        AABB box = new AABB(c.x - reach, c.y - 1, c.z - reach, c.x + reach, c.y + h * 2.5, c.z + reach);
        for (Entity target : level.getEntities(pe, box)) {
            if (!(target instanceof LivingEntity) || target.isSpectator()) continue;
            if (target instanceof PokemonEntity other && other.isBattling()) continue;
            if (target instanceof Player p && (p.isCreative() && p.getAbilities().flying)) continue;
            Vec3 d = target.position().subtract(c);
            double dist = Math.sqrt(d.x * d.x + d.z * d.z);
            if (dist > reach) continue;
            double k = BLAST * (1.0 - 0.6 * dist / reach);
            double nx = dist < 1e-3 ? 1 : d.x / dist, nz = dist < 1e-3 ? 0 : d.z / dist;
            target.push(nx * k, 0.35 * k, nz * k);
            target.hurtMarked = true;
        }
    }
}
