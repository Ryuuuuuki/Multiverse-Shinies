package com.multiverseshinies;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

/**
 * The Multiverse Pokemon that transform when they get a stat boost in battle, and how:
 *  - Infernape (Super Saiyan 4 cosplay): red aura, ground effects, bursts 2.62 s into its rage animation (the roar);
 *  - Monferno (Goku's gi): golden aura with lightning, no ground effects, its fur turns golden; bursts 0.5 s into its
 *    animation, when the Power Pole hits the ground.
 */
public enum RageSpecies {
    INFERNAPE("cobblemon:infernape", 52, false),
    MONFERNO("cobblemon:monferno", 10, true);

    /** aspect a golden Pokemon gets while transformed: its resolver swaps in the golden fur texture */
    public static final String GOLDEN_ASPECT = "multiverse_ssj";
    /** aspect it gets as soon as the boost lands: boost.json skips the transformation animation while it's there */
    public static final String RAGING_ASPECT = "multiverse_raging";

    public final String species;
    /** ticks from the boost (the rage animation starting) to the burst */
    public final int burstTicks;
    /** golden Super Saiyan: gold aura, lightning, golden fur, no rocks/dust */
    public final boolean golden;

    RageSpecies(String species, int burstTicks, boolean golden) {
        this.species = species;
        this.burstTicks = burstTicks;
        this.golden = golden;
    }

    /** which transforming Multiverse Pokemon this is, or null */
    public static RageSpecies of(PokemonEntity pe) {
        try {
            if (!pe.getAspects().contains("multiverseshinies")) return null;
            String id = pe.getPokemon().getSpecies().getResourceIdentifier().toString();
            for (RageSpecies s : values()) {
                if (s.species.equals(id)) return s;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
