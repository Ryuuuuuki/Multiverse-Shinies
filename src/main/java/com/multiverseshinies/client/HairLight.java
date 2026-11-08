package com.multiverseshinies.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Infernape SSJ4's burning hair lights up its surroundings like a torch. Minecraft has no moving light, so each
 * client tick an invisible vanilla light block is put in the air at his head, in the client's copy of the world
 * only (nothing is sent to the server or saved), and taken back when he moves on, is recalled or leaves.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class HairLight {
    private static final int LEVEL = 13;
    private static final int RAGE_LEVEL = 15;
    private static final double MAX_DIST_SQ = 64 * 64;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final Map<Integer, BlockPos> PLACED = new HashMap<>();
    private static ClientLevel lastLevel;

    private HairLight() {}

    private static boolean isOurInfernape(PokemonEntity pe) {
        try {
            return pe.getAspects().contains("multiverseshinies")
                && "cobblemon:infernape".equals(pe.getPokemon().getSpecies().getResourceIdentifier().toString());
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != lastLevel) { PLACED.clear(); lastLevel = level; }   // the old level's blocks went with it
        if (level == null || mc.player == null) return;

        Map<Integer, BlockPos> want = new HashMap<>();
        Map<Integer, Integer> wantLevel = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pe) || pe.isRemoved() || pe.isInvisible()) continue;
            if (pe.distanceToSqr(mc.player) > MAX_DIST_SQ || !isOurInfernape(pe)) continue;
            BlockPos head = BlockPos.containing(pe.getX(), pe.getY() + pe.getBbHeight() * 0.85, pe.getZ());
            BlockPos pos = canHold(level, head, pe.getId()) ? head
                : canHold(level, head.below(), pe.getId()) ? head.below() : null;
            if (pos == null) continue;
            want.put(pe.getId(), pos);
            wantLevel.put(pe.getId(), RageAuraRenderer.isRaging(pe) ? RAGE_LEVEL : LEVEL);
        }

        // take back lights that moved or whose Infernape is gone
        for (Iterator<Map.Entry<Integer, BlockPos>> it = PLACED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, BlockPos> e = it.next();
            if (!e.getValue().equals(want.get(e.getKey()))) {
                clear(level, e.getValue());
                it.remove();
            }
        }
        for (Map.Entry<Integer, BlockPos> e : want.entrySet()) {
            BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, wantLevel.get(e.getKey()));
            if (!level.getBlockState(e.getValue()).equals(light)) level.setBlock(e.getValue(), light, FLAGS);
            PLACED.put(e.getKey(), e.getValue());
        }
    }

    /** air, or the light this same Infernape already put there */
    private static boolean canHold(ClientLevel level, BlockPos pos, int id) {
        if (!level.isLoaded(pos)) return false;
        BlockState s = level.getBlockState(pos);
        return s.isAir() || (s.is(Blocks.LIGHT) && pos.equals(PLACED.get(id)));
    }

    private static void clear(ClientLevel level, BlockPos pos) {
        // only if it's still our light (the server may have put a real block there meanwhile)
        if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }
}
