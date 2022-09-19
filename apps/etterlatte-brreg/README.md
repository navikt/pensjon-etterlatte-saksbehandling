# etterlatte-brreg

Enkelt API for å hente data fra enhetsregisteret. Bygget med Kotlin / [Ktor](https://ktor.io/). 

# Kom i gang

## Kjøre lokalt

1. Legg til `BRREG_URL=https://data.brreg.no` som environment variable. 
2. Kjør `Application.kt`



## Endepunkter

| Miljø    | URL                                        |
|----------|--------------------------------------------|
| **Prod** | https://etterlatte-brreg.intern.nav.no     |
| **Dev**  | https://etterlatte-brreg.dev.intern.nav.no |



#### Søke på navn

`/enheter?navn=<navn på bedrift>`

Eksempelrespons (liste med selskap som matcher): 

```
[
    {
        "organisasjonsnummer": "123456789",
        "navn": "SNEKKER 1 AS",
        "organisasjonsform": {
            "kode": "AS",
            "beskrivelse": "Aksjeselskap"
        }
    },
    { ... },
    { ... },
    { ... }
]
```

#### Søke på orgnr

**OBS:** Orgnr. _må_ være nøyaktig 9 siffer.

`/enheter/<orgnr>`

Eksempelrespons:

```
{
    "organisasjonsnummer": "123456789",
    "navn": "SNEKKER 1 AS",
    "organisasjonsform": {
        "kode": "AS",
        "beskrivelse": "Aksjeselskap"
    }
}
```



# Bygg og deploy

En app bygges og deployes automatisk når en endring legges til i `main`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.


## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
