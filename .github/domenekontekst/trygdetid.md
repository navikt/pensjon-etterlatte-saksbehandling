# etterlatte-trygdetid

Håndterer og beregner trygdetid for avdøde – dvs. perioder med opptjening i Norge og utlandet som avgjør størrelsen på ytelsen.

## Ansvarsområder

- Registrere og validere trygdetidsperioder (manuelt eller importert fra Pesys)
- Beregne faktisk og fremtidig trygdetid
- Håndtere utenlandsavtaler som teller med i trygdetiden
- Sjekke avvik mot Pesys (bakgrunnsjobb)

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `Trygdetid` | Aggregert resultat med alle perioder for én behandling |
| `TrygdetidGrunnlag` | Én enkelt periode (fra/til-dato, land, kilde) |
| `DetaljertBeregnetTrygdetid` | Ferdig beregnet trygdetid som sendes videre til beregning |
| `Avtale` | Utenlandsavtale som gir opptjening utenfor Norge |

## Nøkkelklasser

- `TrygdetidService` – hoved-API for opprettelse, henting og oppdatering
- `TrygdetidBeregningService` – beregner total trygdetid fra grunnlag
- `AvtaleService` – håndterer utenlandsavtaler
- `SjekkAvvikJobb` – bakgrunnsjobb som sammenligner med Pesys

## Avhengigheter

Kaller: `etterlatte-behandling` (behandlingskontekst, tilgangskontroll og grunnlag inkl. persondata for avdøde), Pesys (kun lesing – henter trygdetid for avdøde med uføretrygd eller alderspensjon fra legacy-systemet), `etterlatte-vedtaksvurdering` (kobling mot vedtak)
