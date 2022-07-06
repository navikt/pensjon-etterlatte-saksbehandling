# etterlatte-brreg

App med API for å hente data fra enhetsregisteret.

# Kom i gang

## Endepunkter

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

## Miljøer

**Prod:**\
https://etterlatte-brreg.intern.nav.no

**Dev:**\
https://etterlatte-brreg.dev.intern.nav.no



# Bygg og deploy

En app bygges og deployes automatisk når en endring legges til i `main`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.


## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
