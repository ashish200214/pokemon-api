package com.pokemon.controller;

import com.pokemon.dto.CacheStats;
import com.pokemon.dto.PokemonDetails;
import com.pokemon.service.PokemonApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PokemonController {
    private final PokemonApiService pokemonApiService;

    public PokemonController(PokemonApiService pokemonApiService) {
        this.pokemonApiService = pokemonApiService;
    }

    @GetMapping("/pokemon/{name}")
    public ResponseEntity<PokemonDetails> getPokemon(@PathVariable String name) {
        return ResponseEntity.ok(pokemonApiService.getPokemon(name));
    }

    @GetMapping("/pokemon/cache")
    public ResponseEntity<CacheStats> getCacheStats() {
        return ResponseEntity.ok(pokemonApiService.getCacheStats());
    }
}
