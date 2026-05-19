package com.pokemon.dto;

import java.time.Instant;
import java.util.List;

public record PokemonDetails(
        int id,
        String name,
        int baseExperience,
        double heightMeters,
        double weightKg,
        String imageUrl,
        List<String> types,
        List<Ability> abilities,
        List<Stat> stats,
        List<String> moves,
        SpeciesDetails species,
        boolean cached,
        Instant cacheExpiresAt) {

    public PokemonDetails withCacheStatus(boolean cached, Instant cacheExpiresAt) {
        return new PokemonDetails(
                id,
                name,
                baseExperience,
                heightMeters,
                weightKg,
                imageUrl,
                types,
                abilities,
                stats,
                moves,
                species,
                cached,
                cacheExpiresAt);
    }

    public record Ability(String name, boolean hidden) {
    }

    public record Stat(String name, int value) {
    }

    public record SpeciesDetails(
            String genus,
            String flavorText,
            String habitat,
            String growthRate,
            String generation,
            int captureRate,
            int baseHappiness,
            List<String> eggGroups) {
    }
}
