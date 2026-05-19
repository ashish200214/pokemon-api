package com.pokemon.service;

import com.pokemon.dto.CacheStats;
import com.pokemon.dto.PokemonDetails;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PokemonApiService {
    private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache;
    private final int maxCacheEntries;
    private final Duration ttl;

    public PokemonApiService(
            @Value("${pokemon.cache.max-entries:100}") int maxCacheEntries,
            @Value("${pokemon.cache.ttl-seconds:600}") long ttlSeconds) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.maxCacheEntries = Math.max(1, maxCacheEntries);
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

    public PokemonDetails getPokemon(String rawName) {
        String name = normalizeName(rawName);
        CacheEntry cached = getCached(name);

        if (cached != null) {
            return cached.details().withCacheStatus(true, cached.expiresAt());
        }

        PokemonDetails details = fetchPokemonDetails(name);
        putCached(name, details);
        return details.withCacheStatus(false, Instant.now().plus(ttl));
    }

    public synchronized CacheStats getCacheStats() {
        evictExpiredEntries();
        return new CacheStats(cache.size(), maxCacheEntries, ttl.toSeconds());
    }

    private synchronized CacheEntry getCached(String name) {
        CacheEntry entry = cache.get(name);
        if (entry == null) {
            return null;
        }

        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(name);
            return null;
        }

        return entry;
    }

    private synchronized void putCached(String name, PokemonDetails details) {
        evictExpiredEntries();
        cache.put(name, new CacheEntry(details, Instant.now().plus(ttl)));

        while (cache.size() > maxCacheEntries) {
            String eldestKey = cache.keySet().iterator().next();
            cache.remove(eldestKey);
        }
    }

    private void evictExpiredEntries() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private PokemonDetails fetchPokemonDetails(String name) {
        JsonNode pokemon = fetchJson("/pokemon/" + name);

        if (pokemon == null || pokemon.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pokemon not found");
        }

        JsonNode species = fetchSpecies(pokemon.path("species").path("url").asText());
        return mapPokemon(pokemon, species);
    }

    private JsonNode fetchSpecies(String speciesUrl) {
        if (speciesUrl == null || speciesUrl.isBlank()) {
            return null;
        }

        String path = speciesUrl.replace(POKEAPI_BASE_URL, "");
        try {
            return fetchJson(path);
        } catch (ResponseStatusException ex) {
            return null;
        }
    }

    private JsonNode fetchJson(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(POKEAPI_BASE_URL + path))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pokemon not found");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "PokeAPI request failed with status " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read PokeAPI response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pokemon request was interrupted", ex);
        }
    }

    private PokemonDetails mapPokemon(JsonNode pokemon, JsonNode species) {
        List<String> types = readNames(pokemon.path("types"), "type");
        List<PokemonDetails.Ability> abilities = new ArrayList<>();
        pokemon.path("abilities").forEach(ability -> abilities.add(new PokemonDetails.Ability(
                titleCase(ability.path("ability").path("name").asText()),
                ability.path("is_hidden").asBoolean(false))));

        List<PokemonDetails.Stat> stats = new ArrayList<>();
        pokemon.path("stats").forEach(stat -> stats.add(new PokemonDetails.Stat(
                titleCase(stat.path("stat").path("name").asText()),
                stat.path("base_stat").asInt())));

        List<String> allMoves = new ArrayList<>();
        pokemon.path("moves").forEach(move -> allMoves.add(titleCase(move.path("move").path("name").asText())));
        List<String> moves = allMoves.stream().limit(8).toList();

        List<String> eggGroups = species == null ? List.of() : readNames(species.path("egg_groups"), null);
        PokemonDetails.SpeciesDetails speciesDetails = new PokemonDetails.SpeciesDetails(
                readLocalizedText(species, "genera", "genus").orElse("Unknown Pokemon"),
                readFlavorText(species).orElse("No Pokedex entry was available for this Pokemon."),
                titleCase(readNestedName(species, "habitat").orElse("unknown")),
                titleCase(readNestedName(species, "growth_rate").orElse("unknown")),
                titleCase(readNestedName(species, "generation").orElse("unknown")),
                species == null ? 0 : species.path("capture_rate").asInt(0),
                species == null ? 0 : species.path("base_happiness").asInt(0),
                eggGroups.stream().map(this::titleCase).toList());

        return new PokemonDetails(
                pokemon.path("id").asInt(),
                titleCase(pokemon.path("name").asText()),
                pokemon.path("base_experience").asInt(),
                pokemon.path("height").asDouble() / 10,
                pokemon.path("weight").asDouble() / 10,
                pokemon.path("sprites").path("other").path("official-artwork").path("front_default").asText(null),
                types.stream().map(this::titleCase).toList(),
                abilities,
                stats,
                moves,
                speciesDetails,
                false,
                Instant.now().plus(ttl));
    }

    private String normalizeName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pokemon name is required");
        }

        return rawName.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private List<String> readNames(JsonNode array, String nestedKey) {
        List<String> values = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return values;
        }

        array.forEach(item -> {
            JsonNode nameNode = nestedKey == null ? item.path("name") : item.path(nestedKey).path("name");
            if (!nameNode.asText("").isBlank()) {
                values.add(nameNode.asText());
            }
        });
        return values;
    }

    private Optional<String> readNestedName(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return Optional.empty();
        }
        return Optional.ofNullable(node.path(field).path("name").asText(null));
    }

    private Optional<String> readLocalizedText(JsonNode node, String arrayField, String textField) {
        if (node == null || !node.path(arrayField).isArray()) {
            return Optional.empty();
        }

        for (JsonNode item : node.path(arrayField)) {
            if ("en".equals(item.path("language").path("name").asText())) {
                return Optional.ofNullable(item.path(textField).asText(null));
            }
        }
        return Optional.empty();
    }

    private Optional<String> readFlavorText(JsonNode species) {
        return readLocalizedText(species, "flavor_text_entries", "flavor_text")
                .map(text -> text.replace('\n', ' ').replace('\f', ' '))
                .map(text -> text.replaceAll("\\s+", " ").trim());
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] words = value.replace('-', ' ').split(" ");
        List<String> titled = new ArrayList<>();
        for (String word : words) {
            if (!word.isBlank()) {
                titled.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return String.join(" ", titled);
    }

    private record CacheEntry(PokemonDetails details, Instant expiresAt) {
    }
}
