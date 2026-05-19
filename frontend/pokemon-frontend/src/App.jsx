import { useMemo, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

const typeColors = {
  bug: '#91a119',
  dark: '#624d4e',
  dragon: '#5060e1',
  electric: '#fac000',
  fairy: '#ef70ef',
  fighting: '#ff8000',
  fire: '#e62829',
  flying: '#81b9ef',
  ghost: '#704170',
  grass: '#3fa129',
  ground: '#915121',
  ice: '#3dcef3',
  normal: '#9fa19f',
  poison: '#9141cb',
  psychic: '#ef4179',
  rock: '#afa981',
  steel: '#60a1b8',
  water: '#2980ef',
}

function App() {
  const [query, setQuery] = useState('pikachu')
  const [pokemon, setPokemon] = useState(null)
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')

  const dominantType = useMemo(
    () => pokemon?.types?.[0]?.toLowerCase() ?? 'normal',
    [pokemon],
  )

  async function searchPokemon(event) {
    event?.preventDefault()
    await searchPokemonByName(query)
  }

  async function searchPokemonByName(value) {
    const name = value.trim()

    if (!name) {
      setError('Enter a Pokemon name to search.')
      return
    }

    setStatus('loading')
    setError('')

    try {
      const response = await fetch(`${API_BASE_URL}/pokemon/${encodeURIComponent(name)}`)
      const payload = await response.json().catch(() => null)

      if (!response.ok) {
        throw new Error(payload?.message ?? 'Pokemon was not found.')
      }

      setPokemon(payload)
      setStatus('success')
    } catch (err) {
      setPokemon(null)
      setStatus('error')
      setError(err.message || 'Something went wrong while searching.')
    }
  }

  return (
    <main className="pokedex-shell">
      <section className="search-panel" aria-labelledby="pokedex-title">
        <div className="brand-mark">
          <span className="brand-dot" />
          <span>Pokedex</span>
        </div>

        <div className="intro">
          <p className="eyebrow">Local REST API + cached PokeAPI data</p>
          <h1 id="pokedex-title">Search any Pokemon by name.</h1>
          <p>
            Pull fast repeat results from the backend cache and inspect types,
            stats, abilities, species data, moves, and capture details.
          </p>
        </div>

        <form className="search-form" onSubmit={searchPokemon}>
          <label htmlFor="pokemon-search">Pokemon name</label>
          <div className="search-row">
            <input
              id="pokemon-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Try charizard, mew, eevee..."
              autoComplete="off"
            />
            <button type="submit" disabled={status === 'loading'}>
              {status === 'loading' ? 'Searching' : 'Search'}
            </button>
          </div>
        </form>

        {error && <p className="error-message">{error}</p>}

        <div className="quick-searches" aria-label="Quick searches">
          {['Bulbasaur', 'Gengar', 'Lucario', 'Greninja'].map((name) => (
            <button
              key={name}
              type="button"
              onClick={() => {
                setQuery(name)
                searchPokemonByName(name)
              }}
            >
              {name}
            </button>
          ))}
        </div>
      </section>

      <section
        className="result-stage"
        style={{ '--type-color': typeColors[dominantType] ?? typeColors.normal }}
      >
        {pokemon ? <PokemonProfile pokemon={pokemon} /> : <EmptyState status={status} />}
      </section>
    </main>
  )
}

function PokemonProfile({ pokemon }) {
  const totalStats = pokemon.stats.reduce((sum, stat) => sum + stat.value, 0)

  return (
    <article className="pokemon-profile">
      <div className="profile-hero">
        <div>
          <p className="dex-number">#{String(pokemon.id).padStart(4, '0')}</p>
          <h2>{pokemon.name}</h2>
          <p>{pokemon.species.flavorText}</p>
          <div className="type-list">
            {pokemon.types.map((type) => (
              <span key={type}>{type}</span>
            ))}
          </div>
        </div>
        <div className="art-orbit">
          {pokemon.imageUrl ? (
            <img src={pokemon.imageUrl} alt={pokemon.name} />
          ) : (
            <span className="missing-art">No image</span>
          )}
        </div>
      </div>

      <div className="metric-grid">
        <Metric label="Height" value={`${pokemon.heightMeters.toFixed(1)} m`} />
        <Metric label="Weight" value={`${pokemon.weightKg.toFixed(1)} kg`} />
        <Metric label="XP" value={pokemon.baseExperience} />
        <Metric label="Stats" value={totalStats} />
      </div>

      <div className="details-grid">
        <section className="panel stats-panel">
          <div className="panel-heading">
            <h3>Base Stats</h3>
            <span>{totalStats} total</span>
          </div>
          {pokemon.stats.map((stat) => (
            <div className="stat-row" key={stat.name}>
              <span>{stat.name}</span>
              <strong>{stat.value}</strong>
              <div className="stat-track">
                <span style={{ width: `${Math.min(stat.value, 160) / 1.6}%` }} />
              </div>
            </div>
          ))}
        </section>

        <section className="panel">
          <div className="panel-heading">
            <h3>Abilities</h3>
            <span>{pokemon.cached ? 'Cached' : 'Fresh'}</span>
          </div>
          <div className="ability-list">
            {pokemon.abilities.map((ability) => (
              <span key={ability.name}>
                {ability.name}
                {ability.hidden && <small>Hidden</small>}
              </span>
            ))}
          </div>
        </section>

        <section className="panel species-panel">
          <h3>Species</h3>
          <dl>
            <Info label="Genus" value={pokemon.species.genus} />
            <Info label="Habitat" value={pokemon.species.habitat} />
            <Info label="Growth" value={pokemon.species.growthRate} />
            <Info label="Generation" value={pokemon.species.generation} />
            <Info label="Capture rate" value={pokemon.species.captureRate} />
            <Info label="Happiness" value={pokemon.species.baseHappiness} />
          </dl>
        </section>

        <section className="panel moves-panel">
          <h3>Signature Moves</h3>
          <div className="move-list">
            {pokemon.moves.map((move) => (
              <span key={move}>{move}</span>
            ))}
          </div>
        </section>
      </div>
    </article>
  )
}

function EmptyState({ status }) {
  return (
    <div className="empty-state">
      <div className="pokeball" />
      <h2>{status === 'loading' ? 'Scanning the tall grass...' : 'Your Pokemon dossier will appear here.'}</h2>
      <p>Search by name to load a cached, locally served Pokedex profile.</p>
    </div>
  )
}

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function Info({ label, value }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value || 'Unknown'}</dd>
    </>
  )
}

export default App
