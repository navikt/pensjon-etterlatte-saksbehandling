# Samordning-vedtak

Omstillingsstønad er en samordningspliktig ytelse. Applikasjonen tilbyr tjenester for å understøtte informasjonsbehovet til tjenestepensjonsleverandørene (KLP, SPK, m.fl.), slik at de kan gjøre sine vurderinger.

## Autentisering

Tjenesten krever token utstedt av Maskinporten med scope _nav:etterlatteytelser:vedtaksinformasjon.read_

### Autorisasjon

Det må foreligge et tjenestepensjonsforhold og -ytelse i Tjenestepensjonsregisteret som gjelder kallende leverandør, personen vedtaket gjelder og på vedtakets virkningsdato.

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
| arsak*           | string      | INNTEKT/REGULERING/ANNET  |
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

| Miljø | Ingress                                         |
|:------|:------------------------------------------------|
| dev   | etterlatte-samordning-vedtak.ekstern.dev.nav.no |
| prod  | etterlatte-samordning-vedtak.nav.no             |    

## Feilkoder

| Kode                    | HTTP-statuskode | Beskrivelse                                              |
|-------------------------|-----------------|----------------------------------------------------------|
| GE-MASKINPORTEN-SCOPE   | 401             | Manglende/feil scope                                     |
| 001-TPNR-MANGLER        | 400             | Ikke angitt header med gjeldende ordningsnummer          |
| 002-FNR-MANGLER         | 400             | Ikke angitt header med fødselsnummer det skal spørres på |
| 003-FOMDATO-MANGLER     | 400             | Ikke angitt fomDato som query parameter                  |
| 004-FEIL_SAKSTYPE       | 400             | Etterspurt vedtak som ikke angår omstillingsstønad       |
| 005-PAADATO-MANGLER     | 400             | Ikke angitt paaDato som query parameter                  |
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

## Internt i NAV

Endepunktene som er nevnt over finnes også til bruk for NAV-interne systemer, men da på `/api/pensjon/vedtak` osv.

### Løpende omstillingsstønad
Her finnes i tillegg et endepunkt som svarer ja/nei på dette på en spesifikk dato. Dersom ytelsen slutter dagen før angitt dato, eller starter måneden etterpå så vil svaret være _nei_. **NB!** Merk at denne tjenesten _ikke gjør noe tolkning av faktisk utbetaling_ for å gi svaret, kun om ytelsen er innvilget. Så for eksempel om ytelsen er fullstendig avkortet, så vil svaret likevel være ja. 
- `GET /api/pensjon/vedtak/har-loepende-oms?paaDato=YYYY-MM-DD` 
  - fnr i header
  - svarformat: 
     ```
     {
       "omstillingsstoenad": true
     }
    ```
    
### Løpende barnepensjon
Som for omstillingsstønad.
- `GET /api/barnepensjon/har-loepende-bp?paaDato=YYYY-MM-DD`
  - fnr i header
  - svarformat:
     ```
     {
       "barnepensjon": true
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
