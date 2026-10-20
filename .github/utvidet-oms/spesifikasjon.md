# Spesifikasjon – søknad om utvidet omstillingsstønad

Fasit for hvordan søknaden skal oppføre seg (B6). Spesifikasjonen er skrevet ut fra Figma og notatene fra design.

- **Figma:** [Utvidet omstillingsstønad (forlengelse)](https://www.figma.com/design/mnsytPN6FESmWDi2kP2CqI/Utvidet-omstillingst%C3%B8nad--forlengelse-?node-id=292-11804)
- **Kontrakt:** `UtvidetOmstillingsstoenad` i `pensjon-etterlatte-felles/common/.../innsendtsoeknad/utvidetomstillingsstoenad/`
- **Eksempler:** `common/src/test/resources/soeknad/utvidet_omstillingsstoenad*.json` i felles

Når Figma endres, oppdateres denne filen først, deretter koden. Avvik og uavklarte punkter står i [beslutninger.md](beslutninger.md) og er merket med Å-nummer her.

## Felles regler

- **Layout:** Overskriften «Pengestøtte / Søknad om utvidet omstillingsstønad» med ikon, og språkvelger (nb/nn/en).
- **Fremdrift:** Fremdriftslinje, «Steg x av y» og «Vis alle steg». Steg 2–6 og oppsummeringen telles med. Introsiden og kvitteringen telles ikke med. Figma viser «Steg 4 av 4» på oppsummeringen, så tellingen må bekreftes med design.
- **Navigasjon:** `Forrige steg`, `Neste steg` og `Avbryt`. `Neste steg` validerer steget. Ved feil vises Aksel `ErrorSummary` øverst, med fokus og lenker til feltene.
- **Advarsler** (`Alert variant="warning"`) blokkerer aldri (B3).
- **Lagring:** Kladden lagres ved hvert stegbytte. Den slettes etter 72 timer uten aktivitet, slik det står på introsiden.
- **Avbryt:** Gir valget mellom å fortsette senere og å slette søknaden, som i de andre søknadene.
- **Tekster:** Alle tekster ligger i `locales/{nb,nn,en}.ts`. Kontrakten får spørsmålsteksten (`spoersmaal`) og svarteksten (`innhold`) på språket bruker har valgt.
- **Felt som skjules:** Når et betinget felt skjules, skal verdien fjernes fra state og ikke sendes med (`shouldUnregister` eller eksplisitt nullstilling).
- **Fødselsnummer:** Valideres med `@navikt/fnrvalidator` og godtar også d-nummer.
- **Beløp:** Bare heltall ≥ 0. Mellomrom er tillatt i input, men fjernes før innsending (som `RHFInntektInput` i `omstillingsstoenad-ui`).

---

## Steg 1 – Introside

**Forutsetning:** Bruker må ha en sak på omstillingsstønad (Å3). Dette sjekkes med `GET /api/api/sak/oms/har-loepende-sak`.

### Variant A – har sak
- **Veilederboble:** «Hei, {fornavn}!» med teksten om at stønaden sikrer inntekt i inntil 3 år etter dødsfallet og kan forlenges med inntil 2 år ved utdanning eller tiltak. Periodene må henge sammen. Boblen lenker til «om omstillingsstønad» i ny fane.
- **Her kan du søke om:** Søknaden gjelder forlengelse for bruker som skal ta utdanning eller delta i arbeidsrettet tiltak. Bruker må allerede ha omstillingsstønad. Teksten lenker til søknad om omstillingsstønad.
- **Før du søker:** Ved utdanning må bruker dokumentere navn på studiested, navn på studie, hvor mye hen skal studere og perioden. Dokumentasjonen må vise tydelig hvem den gjelder.
- **Accordions:** Informasjonen vi henter om deg · Hvordan vi behandler personopplysninger · Automatisk saksbehandling · Vi lagrer svar underveis. Fullstendig tekst står i Figma.
- **Lenke:** «Les mer om viktigheten av å gi riktige opplysninger»
- **Samtykke** (checkbox): «Jeg vil svare så godt jeg kan på spørsmålene i søknaden.» Feltet er **påkrevd** for å starte. Kontrakt: `harSamtykket`.
- **Knapp:** `Start søknad`

### Variant B – har ikke sak
- **Advarsel:** «Du har ikke omstillingsstønad» – «Du må ha omstillingsstønad fra før av for å kunne søke om utvidet stønad.»
- **Om omstillingsstønad:** Kort tekst og to lenkepaneler: «Les mer om omstillingsstønad» og «Søk om omstillingsstønad».
- Det finnes ingen startknapp.

**Feil i sjekken av sak:** Vis `SystemUtilgjengelig`. Ikke slipp bruker videre.

> `locales/nb.ts` på branchen har fortsatt forsidetekster fra OMS-søknaden. De må byttes ut.

---

## Steg 2 – Personopplysninger

Bare visning, ingen inndata. Hentes fra `BrukerContext` (`/api/person/innlogget`, se Å7).

| Felt | Kilde |
|------|-------|
| Navn, Fødselsnummer, Bostedsadresse, Sivilstatus, Statsborgerskap, Telefonnummer | Innlogget person (PDL/KRR) |

- **Info-alert:** «Er opplysningene feil?» – «Hvis opplysningene vi har om deg er feil, må du endre disse i Folkeregisteret.» Lenken «Endre opplysningene i Folkeregisteret» åpnes i ny fane.
- **Kontrakt:** `innsender` og `soeker` (`GjenlevendeUtvidetOMS`: fornavn, etternavn, foedselsnummer)

---

## Steg 3 – Situasjonen din i dag

Vi spør fordi ny samboer eller nytt ekteskap kan påvirke retten til stønad.

| Felt | Type | Vises når | Validering | Kontrakt |
|------|------|-----------|------------|----------|
| Har du ny samboer eller giftet deg på nytt? | Radio: Jeg har ny samboer / Jeg har giftet meg på nytt / Ingen av delene | Alltid | Påkrevd | `situasjonenDinIDag.nySivilstand.svar` → `NY_SAMBOER` / `GIFTET_PAA_NYTT` / `INGEN_AV_DELENE` |
| Samboers fornavn | Tekst | Ny samboer | Påkrevd | `nySivilstand.opplysning.fornavn` |
| Samboers etternavn | Tekst | Ny samboer | Påkrevd | `nySivilstand.opplysning.etternavn` |
| Samboers fødselsnummer | Tekst | Ny samboer | Påkrevd, gyldig fnr/d-nr, ikke eget fnr | `nySivilstand.opplysning.foedselsnummer` |
| Har eller har dere hatt felles barn, eller har dere tidligere vært gift? | Radio: Ja / Nei (Å2) | Ny samboer | Påkrevd | `nySivilstand.opplysning.fellesBarnEllertidligereGift` |

`Samboer.inntekt` sendes som `null` (Å2).

**Advarsler** (blokkerer ikke):
- Felles barn eller tidligere gift = Ja: «Ikke rett til omstillingsstønad» – «Du har ikke rett til omstillingsstønad hvis du er samboer med en person du har eller har hatt felles barn med, eller med en person du tidligere har vært gift med.»
- Giftet på nytt: «Ikke rett til omstillingsstønad» – «Du har ikke rett til omstillingsstønad når du har giftet deg på nytt.»

---

## Steg 4 – Utdanning og tiltak

For å få utvidet stønad må bruker studere eller delta i arbeidsrettet tiltak.

| Felt | Type | Vises når | Validering | Kontrakt |
|------|------|-----------|------------|----------|
| Hva gjelder for deg? | Radio: Jeg tar eller skal ta utdanning / Jeg deltar i eller skal delta i arbeidsrettet tiltak (beskrivelse: «Arbeidsrettet tiltak er et tiltak gjennom Nav som skal hjelpe deg med å komme i arbeid») / Ingen av disse gjelder for meg | Alltid | Påkrevd | `utdanningOgTiltak.aktivitet` → `UTDANNING` / `ARBEIDSRETTET_TILTAK` / `INGEN` |
| Hvilken skole studerer du ved? | Tekst | Utdanning | Påkrevd, maks 200 tegn | `utdanning.studiested` |
| Hva studerer du? | Tekst | Utdanning | Påkrevd, maks 200 tegn | `utdanning.studie` |
| Fra dato / Til dato | DatePicker (dd.mm.åååå) | Utdanning | Begge påkrevd, gyldig dato, fra ≤ til | `utdanning.startDato`, `utdanning.sluttDato` (ISO) |
| Studerer du på heltid eller deltid? | Radio: Heltid / Deltid | Utdanning | Påkrevd | `utdanning.studieform` (`StudieformType`) |
| Hva er studieprosenten din? | Tall | Utdanning og deltid | Påkrevd, heltall 1–100 | `utdanning.studieprosent` |
| Hvilket arbeidsrettet tiltak deltar du i? | Tekst | Tiltak | Påkrevd, maks 200 tegn | `arbeidsrettetTiltak.tiltak` |
| Gjennomfører du tiltakene på heltid eller deltid? | Radio: Heltid / Deltid | Tiltak | Påkrevd | `arbeidsrettetTiltak.omfang` (`TiltakOmfangType`) |
| Har du en aktivitetsplan i samarbeid med Nav? | Radio: Ja / Nei | Bare ved tiltak (Å1) | Påkrevd | `utdanningOgTiltak.aktivitetsplan` |

**Advarsler** (blokkerer ikke):
- Studieprosent < 50: «Ikke rett til omstillingsstønad» – «Du må studere minst 50 prosent for å ha rett til omstillingsstønad.»
- Ingen av disse: «Ikke rett til omstillingsstønad» – «Du må ta utdanning eller delta i arbeidsrettet tiltak for å få utvidet omstillingsstønad. Du kan fortsatt søke, men det kan hende søknaden blir avslått.»

---

## Steg 5 – Inntekt

Samme spørsmål som i OMS-søknaden (`ForventetInntektIAar`), men uten AFP. Gjenbruk eller kopier fra `omstillingsstoenad-ui/src/components/soknad/6-inntekten-din/fragmenter/ForventetInntektIAar.tsx` og tilhørende RHF-komponenter.

**Ingress:** «Oppgi hvor mye du forventer å tjene i år, fra januar til desember. Beløpet skal være før skatt og oppgis i norske kroner.»

| Felt | Type | Vises når | Validering | Kontrakt |
|------|------|-----------|------------|----------|
| Arbeidsinntekt og andre utbetalinger (med ReadMore) | Beløp | Alltid | Påkrevd | `inntekt.arbeidsinntekt` |
| Næringsinntekt (med ReadMore) | Beløp, standard 0 | Alltid | Påkrevd | `inntekt.naeringsinntekt.inntekt` |
| Inntekter fra utland («Inntektene skal oppgis i norske kroner.», med ReadMore) | Beløp, standard 0 | Alltid | Påkrevd | `inntekt.inntektFraUtland` |
| Har du andre inntekter? | Radio: Ja / Nei / Vet ikke | Alltid | Påkrevd | `inntekt.andreInntekter.valg` |
| Sum andre inntekter | Beløp | Andre inntekter = Ja | Påkrevd | `inntekt.andreInntekter.inntekt` |
| Hva slags inntekter? | Textarea | Andre inntekter = Ja | Påkrevd | `inntekt.andreInntekter.beskrivelse` |
| Er det noe du vet om i dag som kan påvirke inntekten din fremover? | Radio: Ja / Nei / Vet ikke | Alltid | Påkrevd | `inntekt.noeSomKanPaavirkeInntekten.valg` |
| Hva er grunnen til at inntekten din endrer seg? | Select (`GrunnTilPaavirkelseAvInntekt`) | Påvirke = Ja | Påkrevd | `noeSomKanPaavirkeInntekten.grunnTilPaavirkelseAvInntekt` |
| Beskrivelse | Textarea | Påvirke = Ja og grunn = annen | Som i OMS | `noeSomKanPaavirkeInntekten.beskrivelse` |

`inntekt.afpInntekt` sendes som `null`.

---

## Steg 6 – Dokumentasjon (WIP)

Tom side med tittel og navigasjon inntil Å4 er avklart. Ingen felt og ingen kontraktfelt.

---

## Steg 7 – Oppsummering

- **Veilederboble:** «Nå kan du se over at alt er riktig før du sender inn søknaden. Du kan endre opplysningene hvis de er feil. Når du har sendt inn søknaden kommer du til en kvitteringsside med informasjon om veien videre.»
- **Seksjoner** (Aksel `FormSummary`): Personopplysninger (lenke til Folkeregisteret, ikke «Endre svar») · Situasjonen din i dag · Utdanning og tiltak · Inntekt · Dokumenter. Hver seksjon unntatt Personopplysninger har «Endre svar», som fører til steget.
- **Visning:** Bare felt som er besvart vises. Spørsmålstekstene er de samme som i steget (Å6). Datoer vises som «Fra dd.mm.åååå / Til dd.mm.åååå», og beløp med tusenskille.
- **Under seksjonene:** «Sist lagret: {dato} kl. {tid}»
- **Knapper:** `Forrige steg`, `Send søknad`, `Fortsett senere` og `Slett søknaden`
- **Send søknad:** Frontend-state mappes til `UtvidetOmstillingsstoenad` og sendes med `POST /api/api/soeknad`. Ved suksess navigeres bruker til kvitteringen. Feil håndteres som i `omstillingsstoenad-ui`: bruker blir værende på siden og får en feilmelding.

---

## Steg 8 – Kvittering

- **Suksess-alert:** «Søknaden er sendt inn!» – «Vi har mottatt søknaden din om utvidet omstillingsstønad. Saken er til behandling hos Nav. Vi tar kontakt med deg hvis vi trenger mer informasjon eller dokumentasjon fra deg.»
- **Dette skjer videre:** Bruker får varsel på SMS eller e-post når saken er behandlet. Teksten lenker til «mitt Nav» og «forventet behandlingstid».
- **Du kan også ha rett til andre stønader:** «Når du er i utdanning eller tiltak, kan du ha rett til andre stønader.» Lenkepanelene er:
  - «Stønad til skolepenger» – «Stønaden kan dekke studieavgift, semesteravgift og eksamensgebyr.»
  - «Tilleggsstønader» – «Stønaden kan dekke utgifter til barnepass, læremidler, daglig reise, reise til samling, bolig og overnatting, flytting, og reise på grunn av oppstart, avslutning eller hjemreise.»
- **Info-alert:** «Husk å melde fra om endringer» lenker til «hvilke endringer du må melde i fra om for omstillingsstønad».
- **Knapp:** `Gå til Mitt Nav`
