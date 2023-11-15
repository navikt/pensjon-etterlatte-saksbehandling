# Samordning-vedtak

Omstillingsstønad er en samordningspliktig ytelse. Applikasjonen tilbyr tjenester for å understøtte informasjonsbehovet til tjenestepensjonsleverandørene (KLP, SPK, m.fl.), slik at de kan gjøre sine vurderinger.

## Autentisering

Tjenesten krever token utstedt av Maskinporten med scope _nav:etterlatteytelser:vedtaksinformasjon.read_

### Autorisasjon

Det må foreligge et tjenestepensjonsforhold i Tjenestepensjonsregisteret som gjelder kallende leverandør, personen vedtaket gjelder og på vedtakets virkningsdato. Ved oppslag med _vedtakId_ så sjekkes det også om det finnes en løpende TP-ytelse gjeldende på vedtakets fra-og-med dato.

## API

| Endepunkt                      | Headers        | Responstype         | Beskrivelse                                                                                                                         |
|:-------------------------------|----------------|---------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| /api/vedtak?fomDato=YYYY-MM-DD | fnr <br/> tpnr | Samordningsvedtak[] | Henter ut vedtaksinformasjon for gitt person fra og med gitt dato.                                                                  |
| /api/vedtak?virkFom=YYYY-MM-DD | fnr <br/> tpnr | Samordningsvedtak[] | **DEPRECATED** Henter ut vedtaksinformasjon for gitt person fra og med gitt dato.                                                   |
| /api/vedtak/{nav-vedtak-id}    | tpnr           | Samordningsvedtak   | Henter ut informasjon om et spesifikt vedtak. VedtaksIDen kommer fra samordningskøen hvor det varsles løpende om vedtak som gjøres. |

| Header | Beskrivelse                      |
|--------|----------------------------------|
| tpnr   | kallende tjenestepensjonsordning |
| fnr    | fødselsnummer til aktuell person |



#### Informasjonsmodell

##### Samordningsvedtak

| Felt             | Type        | Beskrivelse               |
|:-----------------|:------------|:--------------------------|
| vedtakId         | int         | Vedtakets unike id        |
| sakstype         | string      | OMS                       |
| virkningsdato    | YYYY-MM-DD  | Dato vedtaket gjelder fra |
| opphoersdato     | YYYY-MM-DD  | Eventuell sluttdato       |
| type             | string      | START/ENDRING/OPPHOER     |
| arsak*           | string      | INNTEKT/ANNET             |
| anvendtTrygdetid | int         | Avdødes trygdetid         |
| perioder         | Periode[]   | Periodiserte beløp        |

##### Periode

| Felt                     | Type       | Beskrivelse                               |
|:-------------------------|:-----------|:------------------------------------------|
| fom                      | yyyy-MM-dd | Periodens start                           |
| tom                      | yyyy-MM-dd | Periodens slutt (ikke påkrevd)            |
| omstillingsstoenadBrutto | int        | Omstillingsstønad før inntektsavkorting   |
| omstillingsstoenadNetto  | int        | Omstillingsstønad etter inntektsavkorting |

## Integrasjon

### API Gateway (beta, men vil etterhvert bli gjeldende for eksterne)

| Miljø | Ingress                                  | Path               |
|:------|:-----------------------------------------|--------------------|
| dev   | https://etterlatte-gw.ekstern.dev.nav.no | /samordning/vedtak |
| prod  | https://etterlatte-gw.nav.no             | /samordning/vedtak |

### Direkte (vil etterhvert skrus av for eksterne)

| Miljø | Ingress                                                 |
|:------|:--------------------------------------------------------|
| dev   | https://etterlatte-samordning-vedtak.ekstern.dev.nav.no |
| prod  | https://etterlatte-samordning-vedtak.nav.no             |    

## Feilkoder

| Kode                    | HTTP-statuskode | Beskrivelse                                              |
|-------------------------|-----------------|----------------------------------------------------------|
| GE-MASKINPORTEN-SCOPE   | 401             | Manglende/feil scope                                     |
| 001-TPNR-MANGLER        | 400             | Ikke angitt header med gjeldende ordningsnummer          |
| 002-FNR-MANGLER         | 400             | Ikke angitt header med fødselsnummer det skal spørres på |
| 003-VIRKFOM-MANGLER     | 400             | Ikke angitt virkFom som query parameter                  |
| 004-FEIL_SAKSTYPE       | 400             | Etterspurt vedtak som ikke angår omstillingsstønad       |
| 010-TP-TILGANG          | 403             | Ikke tilgang til TP/etterspurt data                      |
| 011-TP-FORESPOERSEL     | 400             | Feil ved spørring mot TP                                 |
| 012-TP-IKKE-FUNNET      | 404             | Kunne ikke finne TP-ressurs                              |
| 020-VEDTAK-TILGANG      | 403             | Ikke tilgang til vedtak/etterspurt data                  |
| 021-VEDTAK-FORESPOERSEL | 400             | Feil ved spørring mot vedtak                             |
| 022-VEDTAK-IKKE-FUNNET  | 404             | Kunne ikke finne vedtaksressurs                          |

### Eksempel-payload ved feil

```
{
    "status": 401,
    "detail": "Har ikke påkrevd scope",
    "code": "GE-MASKINPORTEN-SCOPE",
    "meta": {
        "correlation-id": "30e9a443-f620-4c9e-9547-1bd77d4c86ca",
        "tidspunkt": "2023-11-06T09:19:35.748618Z"
    }
}
```


## Kom i gang

### Hvordan kjøre lokalt mot dev-gcp

Les [README](../../README.md) på rot i prosjektet.


## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-samordning-vedtak/**`.

- Deployes både til dev og prod automatisk ved merge til main.
- For å trigge manuell deploy (miljø kan velges) kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
