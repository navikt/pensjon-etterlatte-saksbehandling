# etterlatte-brev-api

Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.

Brev genereres ved hjelp av [Pensjonbrev (Brevbakeren)](https://github.com/navikt/pensjonsbrev).

## Kom i gang

### Hvordan kjøre lokalt

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
BREV_API_URL=http://host.docker.internal:8084
```

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-brev-api/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`
