package com.pokemon.dto;

public record CacheStats(int entries, int maxEntries, long ttlSeconds) {
}
