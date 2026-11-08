package com.multiverseshinies.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sounds of the Infernape SSJ4 rage that the animation can't play by itself: a looping aura hum while the rage aura is
 * up (starting once the transformation has burst) and a power-down when it ends with him still out.
 * The charge and the burst are sound keyframes of animation.multiverse_infernape.rage.
 * All four events live in assets/multiverseshinies/sounds.json (vanilla sounds by default; a resource pack can swap them).
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class RageSounds {
    private static final SoundEvent AURA_LOOP = event("rage.aura_loop");
    private static final SoundEvent CALM = event("rage.calm");
    private static final int LOOP_DELAY = 34;     // ticks after the burst: where the transformation sound runs into its aura part
    private static final float LOOP_VOLUME = 0.8f;

    private static final class Track {
        int ticks;
        AuraLoop loop;
    }

    private static final Map<Integer, Track> TRACKS = new HashMap<>();

    private RageSounds() {}

    private static SoundEvent event(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, name));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { TRACKS.clear(); return; }
        if (mc.isPaused()) return;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pe) || !RageAuraRenderer.isRaging(pe)) continue;
            Track t = TRACKS.computeIfAbsent(pe.getId(), k -> new Track());
            t.ticks++;
            if (t.ticks >= LOOP_DELAY && (t.loop == null || t.loop.isStopped())) {
                t.loop = new AuraLoop(pe);
                mc.getSoundManager().play(t.loop);
            }
        }

        for (Iterator<Map.Entry<Integer, Track>> it = TRACKS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Track> e = it.next();
            Entity entity = level.getEntity(e.getKey());
            if (entity instanceof PokemonEntity pe && RageAuraRenderer.isRaging(pe)) continue;
            // rage over: power down if he's still standing there (not when he's recalled or gone)
            if (entity instanceof PokemonEntity pe && pe.isAlive() && !pe.isRemoved() && e.getValue().ticks >= LOOP_DELAY) {
                level.playLocalSound(pe.getX(), pe.getY(), pe.getZ(), CALM, SoundSource.NEUTRAL, 1.0f, 1.0f, false);
            }
            it.remove();   // the loop fades itself out
        }
    }

    /** aura hum that follows him and fades in/out with the rage */
    private static final class AuraLoop extends AbstractTickableSoundInstance {
        private final PokemonEntity pe;
        private float fade;

        AuraLoop(PokemonEntity pe) {
            super(AURA_LOOP, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.pe = pe;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = pe.getX();
            this.y = pe.getY();
            this.z = pe.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (pe.isRemoved()) { stop(); return; }
            boolean on = RageAuraRenderer.isRaging(pe);
            fade = Math.max(0f, Math.min(1f, fade + (on ? 0.08f : -0.1f)));
            if (!on && fade <= 0f) { stop(); return; }
            volume = Math.max(0.01f, LOOP_VOLUME * fade);
            x = pe.getX();
            y = pe.getY() + pe.getBbHeight() * 0.5;
            z = pe.getZ();
        }
    }
}
