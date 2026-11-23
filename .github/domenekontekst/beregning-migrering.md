# Migrering av beregning til behandling

Dette er midlertidig migreringsstatus og historikk, ikke en beskrivelse av ferdig arkitektur.
For domenelogikken, se [beregning](beregning.md).
Oppdater dokumentet når et migreringssteg merges, aktiveres eller rulles tilbake.
Ikke likestill kopiert kode, merget kode, deployet kode og aktivert funksjonalitet.

## Obligatorisk kontroll av endringer i den frittstående appen

Før hver endring eller gjennomgang av migreringen, og før merge eller aktivering, skal agenten
alltid kontrollere om `apps/etterlatte-beregning` har fått endringer som også må inn i
behandling sin kopierte beregningsmodul. Dette gjelder så lenge begge implementasjonene finnes.
Statusdatoen i dette dokumentet er ikke bevis på at implementasjonene er avstemt.

1. Hent oppdatert `main` og gjennomgå kildehistorikken siden siste dokumenterte avstemming,
   også for berørte delte biblioteker. Hvis siste avstemte kildecommit er ukjent, finn
   kopieringsgrunnlaget og sammenlign derfra. Ikke anta at arbeidsgrenen er oppdatert.
2. Kontroller databaseendringer, inkludert Flyway-skript, tabeller, kolonner, indekser og
   repository-spørringer. Kontroller også beregningsregler, tjenester, validering,
   modeller/DTO-er, JSON-kontrakter, toggles, konfigurasjon og tilhørende tester.
3. Vurder hver relevant kildeendring mot `apps/etterlatte-behandling/src/main/kotlin/modules/beregning`,
   behandling sine databasemigreringer og oppkoblingen rundt modulen. Overfør nødvendige
   endringer og tester uten å gjeninnføre HTTP-klienter eller andre avhengigheter som bevisst
   er erstattet. Bruk nye Flyway-migreringer ved behov, aldri endre allerede kjørte skript.
4. Dokumenter hvilken kildecommit som er kontrollert, hvilke endringer som er overført,
   og hvorfor eventuelle endringer ikke gjelder den kopierte modulen. Oppgi uavklarte avvik
   eksplisitt. Ikke erklær migreringen klar for merge eller aktivering før disse er avklart.

## Nåværende tilstand

Sist kontrollert 23. november 2026 mot `origin/main` på commit
[`fa81b829e`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/fa81b829e).
PR-status er kontrollert på GitHub. Miljøverdiene nedenfor kommer fra manifestene,
ikke fra en live-avlesning av prod eller Unleash.

Med produksjonsoppsettet i `main` kjører beregningslogikken fortsatt i
`etterlatte-beregning`. UI-ens BFF og behandling sine eksisterende `BeregningKlient`-brukere
kaller denne appen over HTTP. Andre konsumenter, blant annet beregning-kafka, brev-api
og statistikk, bruker også fortsatt beregningsappen.

Databasen `beregning`, eid av `etterlatte-beregning`, er fortsatt autoritativ for
beregningsmodulens data. `sakogbehandlinger`, eid av `etterlatte-behandling`, har fått
kopiert og oppdatert tabellstruktur, men det er ikke det samme som å flytte data eller
bytte autoritativ database. Ingen av migreringsstegene nedenfor kopierer produksjonsdata.

### Brytere som er merget til main

| App | Miljøvariabel | Dev | Prod | Hva den styrer |
| --- | --- | --- | --- | --- |
| `etterlatte-beregning` | `BRUK_INTERN_BEREGNING` | `true` | `false` | Registrering av CRUD-rutene under `/api/beregning/intern`. De ordinære beregningsrutene påvirkes ikke. |
| UI-ens BFF | `BRUK_INTERN_BEREGNING` | `false` | `false` | Backend og token-scope for `/api/beregning`: `false` velger beregning, `true` velger behandling. |
| `etterlatte-behandling` | `BRUK_INTERN_BEREGNING` | `false` | `false` | Aktivering av den flyttede beregningsmodulen og rutene under `/api/beregning`. |
| `etterlatte-behandling` | `BRUK_EGEN_DATABASE_FOR_BEREGNING` | `false` | `false` | Datakilde for alle seks repositories: `false` velger CRUD i beregning, `true` velger lokal JDBC. |

Begge er miljøvariabler som leses ved oppstart, ikke Unleash-toggles.
Samme navn betyr ikke en felles bryter på tvers av apper. Endring krever deployment/omstart.
CRUD-rutene i prod er altså fortsatt deaktivert, og UI er ikke flyttet i noen av miljøene.

Behandling sine migreringsbrytere ble merget i
[#9069](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9069) 23. november.
Begge er fortsatt deaktivert i manifestene, som beskrevet under «Forberedt, men ikke aktivert».

Unleash-bryterne for selve beregningsreglene er separate fra ruting og databasevalg.
Unleash bruker appnavnet fra `NAIS_APP_NAME` og eventuell brukerident.
Når regelkjøringen flyttes fra beregning til behandling, må appnavnbegrensninger og
brukerbaserte strategier fortsatt gi tilsiktet resultat. Verdiene i Unleash er ikke
dokumentert som kontrollert her.

## Mergede steg

Historikken gjelder flyttingen av beregningsmodulen og relevant toggle-støtte.
PostgreSQL-versjonsoppgraderinger er ikke flytting av data fra `beregning` til `sakogbehandlinger`.

### 11. august 2026: opprett tabellstrukturen i behandling

[#8824](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/8824),
commit [`859072b0f`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/859072b0f).

La til `V354__beregning_schema.sql` i behandling med tabeller og indekser fra beregningsdatabasen.
Dette forberedte lagring i `sakogbehandlinger`. Flyway endret databasestrukturen ved kjøring,
men migreringen flyttet ingen data og endret ikke hvilken app som beregner eller lagrer aktive beregninger.

### 21. oktober 2026: brukerfiltrerte beregningsbrytere

[#8973](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/8973),
commit [`98da15e06`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/98da15e06).

La til brukerident i Unleash-konteksten i beregning og flere andre apper, slik behandling allerede hadde.
Formålet var å kunne prøve nye beregningsregler per Gjenny-bruker.
Dette påvirket runtime-evalueringen av brukerbaserte toggles, men flyttet ingen beregningskall eller data til behandling.

### 11. november 2026: kopier repositories og dataklasser

[#9041](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9041),
commit [`a82e4b26a`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/a82e4b26a).

Kopierte repository-laget og dataklasser til `no.nav.etterlatte.modules.beregning`, med
domenepakker og tilsvarende teststruktur. `V362__oppdatere_beregning_schema.sql` oppdaterte
tabeller med blant annet vedtaksreferanse, innvilgede måneder og `vedtaksperioder`.
Dette gjorde behandling klar til å eie persisteringen senere. Databasestrukturen ble endret,
men eksisterende kall og beregningslogikken i beregningsappen ble beholdt.

### 12. november 2026: kopier tjenester, regler og API-ruter

[#9042](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9042),
commit [`df0355435`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/df0355435).

Kopierte tjenester, regler, validering, mappere, HTTP-klienter og tester til beregningsmodulen
i behandling. De seks rutefamiliene beholdt HTTP-metoder og URL-stier.
Formålet var å flytte koden før avhengigheter og kallere ble lagt om.
Rutene og tjenestene ble ikke koblet inn i applikasjonen, så dette flyttet ingen runtime-trafikk.

### 12. november 2026: tilby CRUD fra beregningsappen

[#9043](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9043),
commit [`6eb468916`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/6eb468916).

La til 27 interne endpoints under `/api/beregning/intern`. De kaller eksisterende repositories
uten regelkjøring, og slipper bare inn systemtoken fra behandling. Jackson-tilpasninger
bevarer modellene og JSON-kontrakten over HTTP.
Dette lar behandling kjøre logikken før databasen flyttes.
CRUD-rutene ble aktivert i dev og deaktivert i prod med miljøvariabelen
`BRUK_INTERN_BEREGNING`. Eksisterende endpoints og kallere ble beholdt; ingen data ble flyttet.

### 17. november 2026: forbered UI-ruting til behandling

[#9046](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9046),
commit [`8c919ac6e`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/8c919ac6e).

La til valg av backend og token-scope i BFF-en via `hentBeregningApiConfig`.
Formålet var å kunne flytte UI-trafikken uten å endre frontendens API-kontrakt.
`BRUK_INTERN_BEREGNING` ble satt til `false` i både dev og prod.
UI-trafikken fortsatte derfor til beregning; dette registrerte ikke beregningsruter i behandling.

### 17. november 2026: erstatt HTTP-avhengigheter inne i den kopierte modulen

[#9047](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9047),
commit [`cccaadf6b`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/cccaadf6b).

Fjernet den kopierte modulens klienter for behandling, grunnlag, trygdetid, vedtaksvurdering
og vilkårsvurdering. De kopierte tjenestene bruker i stedet behandling sine egne tjenester.
Tilpasset transaksjonsgrenser, tilgangssjekker og statusfeil, og validerte at behandlinger
i etteroppgjør tilhører samme sak.
Dette forberedte intern kjøring, men aktiverte ikke modulen.

PR-en erstattet **ikke** behandling sin eksisterende `behandling.klienter.BeregningKlientImpl`.
Etteroppgjør, vedtaksbehandling og øvrige brukere av dette grensesnittet fortsatte å kalle
beregningsappen. Tittelen på PR-en betyr derfor ikke at alle beregningskall allerede er interne.

## Forberedt, men ikke aktivert

### Merget til main

Tabellstruktur, repositories, tjenester, regler, API-ruter, CRUD-endpoints og BFF-ruting
finnes i `main`. Den kopierte beregningsmodulen er koblet inn i behandling sin
runtime, men aktiveringsbryteren er av. UI-bryteren er også av, og CRUD er bare aktivert i dev.
En klasse eller rute i `modules/beregning` er ikke bevis på at produksjon bruker den.

### Merget PR #9069

[#9069](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9069) ble merget
23. november 2026 med commit
[`fa81b829e`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/fa81b829e).
Den inneholder følgende forberedende commits:

| Commit | Endring og hensikt |
| --- | --- |
| [`ce92dab44`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/ce92dab44) | Innfører grensesnitt for de seks repositories, slik at tjenestene ikke trenger å kjenne datakilden. |
| [`6cde828eb`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/6cde828eb) | Implementerer grensesnittene i `BeregningCrudKlient`, med systemtoken mot beregningsappen og bevart JSON-kontrakt. |
| [`41a806325`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/41a806325) | Innfører `BeregningRepositoryModule` og ett felles databasevalg for alle seks repositories. Standardvalget er CRUD, ikke behandling sin database. |
| [`e1c28aed0`](https://github.com/navikt/pensjon-etterlatte-saksbehandling/commit/e1c28aed0) | Kobler sammen tjenester og ruter i `BeregningModule` bak en deaktivert aktiveringsbryter, med tilgangssjekker og nødvendige transaksjoner. |

PR-en setter følgende verdier i behandling, både i dev og prod:

| Miljøvariabel | Verdi | Virkning |
| --- | --- | --- |
| `BRUK_INTERN_BEREGNING` | `false` | De flyttede API-rutene registreres ikke, og beregningstjenestene aktiveres ikke. Med `true` registreres rutene i behandling. |
| `BRUK_EGEN_DATABASE_FOR_BEREGNING` | `false` | Ved aktivering bruker alle seks repositories CRUD-API-et i beregning. Med `true` bruker de lokal JDBC mot `sakogbehandlinger`. |

Begge har standardverdi `false` og leses ved oppstart. Å aktivere modulen betyr derfor
ikke at databasen også flyttes. Mellomsteget er:

```text
behandling/modules.beregning -> BeregningCrudKlient -> beregning/intern -> databasen beregning
```

Ingen app skal koble seg direkte til den andre appens database.
Det finnes ingen automatisk synkronisering, dobbeltskriving eller fallback mellom databasene.

### Denne endringen: behandling sine kallere

Arbeidet i [#9070](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/9070)
bygger på den mergede #9069 og er ennå ikke merget.
Det legger til `BeregningKlientIntern` for alle de 16 operasjonene i `BeregningKlient`.
`KlientModule` velger den interne adapteren når behandling sin
`BRUK_INTERN_BEREGNING=true`; ellers beholdes `BeregningKlientImpl` over HTTP.
Avhengighetene initialiseres ved behov for å unngå sirkler mellom behandlingstjenestene og beregningsmodulen.

Dette omfatter blant annet etteroppgjør, vedtaksbehandling, brevdata, inntektsjustering,
automatisk revurdering og sanksjonsoppslag. `VedtakOgBeregningSammenligner` har ikke selv
en klient; den får beregning og avkorting fra `VedtakBehandlingService`, som bruker det samme grensesnittet.
Tilgangssjekker beholdes. Feil fra interne tjenester sendes videre, uten automatisk HTTP-retry.
Manifestverdiene endres ikke, så endringen aktiverer ikke den nye stien.

### Avstemming ved merge av main 23. november

Endringene i beregningsappen og berørte delte biblioteker siden `e9408d1ca` er kontrollert
mot `fa81b829e`. Den kopierte modulen har fått regelen som gir månedsinntekt lik null når
ingen måneder er innvilget, med regelversjon `1.4` og tilhørende test. Den har også fått
valg av beregningsmetode fra overstyrte beregningsperioder i brevdata, uten å gjeninnføre
HTTP-klienter. `FORLENGET_OMS` kommer fra det delte modellbiblioteket og trenger ingen kopi.
Det er ingen database- eller Flyway-endringer i beregningsappen siden denne avstemmingen.

## Gjenstående steg

1. **Merge og aktivering.** Merge klientomleggingen uten å likestille dette med aktivering.
   Prøv flytene i dev først, også med begge datakilder og relevante testdata.
   Når CRUD brukes, må beregningsappen ha CRUD-rutene aktivert før behandling aktiveres.
   Aktiver behandling før UI sendes dit. Kontroller faktisk deployet versjon, tilgangskontroll,
   Unleash-kontekst og flytene for BP, OMS, etteroppgjør og vedtak.
2. **Øvrige konsumenter.** Flytt `etterlatte-beregning-kafka`, `etterlatte-brev-api` og
   `etterlatte-statistikk`, samt `etterlatte-testdata-behandler` i dev. Kontroller alle brukte
   endpoints, inkludert vedlikehold og sletting. Oppdater URL, token-scope og Nais-tilganger.
   Behandling sin bryter endrer ikke konfigurasjonen i andre apper.
3. **Dataflytting.** Planlegg og gjennomfør flytting fra `beregning` til `sakogbehandlinger`,
   med avklart skrivepunkt, eventuell skrivepause/siste synk og kontroll av data og referanser.
   Avstem historikk, regeltrær, avkorting og etteroppgjør før databasevalget endres.
   Ikke slå på lokal lagring mens andre skrivere fortsatt bruker gammel database uten en
   koordinert overgang. Eksisterende Flyway-skript skal ikke endres for å gjøre dette.
4. **Opprydding.** Når alle konsumenter og data er flyttet og rollback-behovet er avklart,
   fjern HTTP-fallbacken, midlertidige adaptere, CRUD-endpoints og migreringsbrytere.
   Avvikle den gamle REST-appen og databaseinfrastrukturen først når ingen lenger bruker dem.
   Fjern utdaterte URL-er, scopes og tilganger. Oppdater permanent domenedokumentasjon og
   arkiver eller fjern denne migreringsstatusen.

Ved rollback av rutingen må UI først flyttes tilbake til beregning før rutene i behandling
deaktiveres. Et databasebytte kan ikke rulles tilbake bare ved å endre en bryter dersom
databasene har fått ulike data.

## Kilder i koden

- [Beregning sine manifester](../../apps/etterlatte-beregning/.nais) og
  [CRUD-beskrivelsen](../../apps/etterlatte-beregning/README.md).
- [UI sine manifester](../../apps/etterlatte-saksbehandling-ui/.nais) og
  [BFF-ens rutingsvalg](../../apps/etterlatte-saksbehandling-ui/server/src/config/beregning.ts).
- [Behandling sine manifester](../../apps/etterlatte-behandling/.nais) og
  [aktivering og databasevalg](../../apps/etterlatte-behandling/README.md).
- [Opprinnelig tabellstruktur](../../apps/etterlatte-behandling/src/main/resources/db/migration/V354__beregning_schema.sql)
  og [schemaoppdateringen](../../apps/etterlatte-behandling/src/main/resources/db/migration/V362__oppdatere_beregning_schema.sql).

Relative kildelenker viser filene i grenen du leser. Bruk commit- og PR-lenkene i historikken
for å skille endringer i arbeidsgrenen fra det som faktisk er merget til `main`.
