# etterlatte-trygdetid

Tjeneste for å opprette trygdetidsgrunnlag og beregne total trygdetid for avdød.

## Kom i gang

### Hvordan kjøre lokalt (auth fra dev-gcp + db lokalt)

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.
```
TRYGDETID_API_URL=http://host.docker.internal:8088
```
