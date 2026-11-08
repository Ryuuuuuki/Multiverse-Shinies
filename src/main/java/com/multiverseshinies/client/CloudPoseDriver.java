package com.multiverseshinies.client;

import com.bedrockk.molang.runtime.value.DoubleValue;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.multiverseshinies.MultiverseShinies;
import com.multiverseshinies.NimbusNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Cobblemon only reports "sprinting" for ridden Pokemon, so the Infernape's nimbus poser can't tell walking from
 * running by itself. Each client tick this measures how fast every Multiverse Infernape is moving and writes
 * v.multiverse_running (0/1, with hysteresis so the pose doesn't flicker) into its posable runtime.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class CloudPoseDriver {
    private static final double RUN_ON = 0.22;   // blocks per tick (smoothed) to start running
    private static final double RUN_OFF = 0.16;  // ... and to go back to walking
    private static final Map<Integer, double[]> STATE = new HashMap<>(); // id -> {smoothed speed, running}

    private CloudPoseDriver() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { STATE.clear(); NimbusNet.FLYING.clear(); com.multiverseshinies.RageNet.BUFFED.clear(); com.multiverseshinies.RageNet.CONFIRMED.clear(); return; }
        if (mc.isPaused()) return;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pe)) continue;
            try {
                if (!pe.getAspects().contains("multiverseshinies")) continue;
                if (!"cobblemon:infernape".equals(pe.getPokemon().getSpecies().getResourceIdentifier().toString())) continue;
                if (!(pe.getDelegate() instanceof PosableState state)) continue;
                double dx = pe.getX() - pe.xo, dz = pe.getZ() - pe.zo;
                double speed = Math.sqrt(dx * dx + dz * dz);
                double[] s = STATE.computeIfAbsent(pe.getId(), k -> new double[2]);
                s[0] += (speed - s[0]) * 0.25;
                if (s[1] == 0 && s[0] > RUN_ON) s[1] = 1;
                else if (s[1] == 1 && s[0] < RUN_OFF) s[1] = 0;
                state.getRuntime().getEnvironment().setSimpleVariable("multiverse_running", new DoubleValue(s[1]));
                // NimbusFlight (server) says through NimbusNet when it flies after its trainer
                state.getRuntime().getEnvironment().setSimpleVariable("multiverse_flying",
                    new DoubleValue(NimbusNet.FLYING.contains(pe.getId()) && !pe.isBattling() ? 1.0 : 0.0));
                // red hair: only from the burst of the rage animation on (RageAuraRenderer delays it)
                state.getRuntime().getEnvironment().setSimpleVariable("multiverse_rage_on",
                    new DoubleValue(RageAuraRenderer.isRaging(pe) ? 1.0 : 0.0));
            } catch (Throwable ignored) {
            }
        }
        if (STATE.size() > 256) STATE.clear();
        NimbusNet.FLYING.removeIf(id -> !(level.getEntity(id) instanceof PokemonEntity));
        com.multiverseshinies.RageNet.BUFFED.removeIf(id -> !(level.getEntity(id) instanceof PokemonEntity));
        com.multiverseshinies.RageNet.CONFIRMED.removeIf(id -> !(level.getEntity(id) instanceof PokemonEntity));
    }
}
