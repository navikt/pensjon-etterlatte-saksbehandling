# Beslutninger – utvidet omstillingsstønad

Logg over hvorfor ting er løst som de er. Når et åpent spørsmål (Å) er avklart, flyttes det til «Besluttet» med neste B-nummer. Skriv også hvem som avklarte det og når. Numrene gjenbrukes ikke.

Format:

```
### Bn – Kort tittel
- **Beslutning:** hva vi gjør
- **Hvorfor:** begrunnelse
- **Konsekvens:** hva dette påvirker (repo/arbeidspakke)
- **Avklart:** dato, hvem/hvor
```

---

## Besluttet

### B1 – Egen frontend-app for søknaden
- **Beslutning:** Søknaden bygges som en ny app, `apps/utvidet-omstillingsstoenad-ui` i pensjon-etterlatte. Den blir ikke en ny flyt i `omstillingsstoenad-ui`.
- **Hvorfor:** Søknaden er mye mindre enn OMS-søknaden og har andre spørsmål. En egen app gir bare det den trenger og gjør det enklere å endre uten å påvirke OMS-søknaden.
- **Konsekvens:** Noen komponenter (RHF-komponenter, inntekt og oppsummering) må kopieres fra `omstillingsstoenad-ui`, fordi det ikke finnes et delt bibliotek. Kopier bare det som trengs.
- **Avklart:** 2026-10, teamet

### B2 – Søknaden inneholder bare søker
- **Beslutning:** Kontrakten har bare `soeker` (`GjenlevendeUtvidetOMS`) og innsender, ikke avdød eller barn.
- **Hvorfor:** Bruker har allerede en OMS-sak der persongalleriet er etablert.
- **Konsekvens:** Gjenny kan ikke lage persongalleri eller opplysninger fra søknaden. Behandlingen må bygge på eksisterende sak og behandling.
- **Avklart:** 2026-10, kontrakt i felles

### B3 – Advarsler blokkerer ikke innsending
- **Beslutning:** Ny samboer med felles barn eller tidligere ekteskap, giftet på nytt, studier under 50 % og verken utdanning eller tiltak gir en advarsel. Bruker kan likevel gå videre og sende inn.
- **Hvorfor:** Det er saksbehandler som avgjør retten til stønad. Vi skal informere, ikke stoppe.
- **Konsekvens:** Advarsler er ikke valideringsfeil og skal ikke hindre `Neste steg`.
- **Avklart:** 2026-10, Figma-notater

### B4 – Midlertidig mottak i Gjenny: journalføring og oppgave
- **Beslutning:** `NySoeknadRiver` finner eller oppretter en OMS-sak og journalfører med tittelen «Søknad om utvidet omstillingsstønad». Deretter oppretter den en `TILLEGGSINFORMASJON`-oppgave og svarer `soeknadFordelt = false`, slik at det ikke opprettes behandling.
- **Hvorfor:** Behandlingsløpet er ikke avklart (Å8). Vi vil kunne ta imot søknader uten å låse oss til en løsning.
- **Konsekvens:** Saksbehandler følger opp manuelt. Dette erstattes når Å8 er avklart.
- **Avklart:** 2026-10, branch `nav-31200-ta-imot-soknad-utvidet-oms`

### B5 – Veikartet ligger i saksbehandling, status ligger i Favro
- **Beslutning:** Veikart, beslutninger, spesifikasjon og agent ligger i `pensjon-etterlatte-saksbehandling/.github/`. Favro er fasit for status, og veikartet lenker til kortene.
- **Hvorfor:** Repoet har allerede agentoppsett og domenekontekst. Status som vedlikeholdes to steder blir fort utdatert.
- **Konsekvens:** I de andre repoene laster man inn agenten med `/add-dir ../pensjon-etterlatte-saksbehandling`. Endringer i veikartet går til main via PR.
- **Avklart:** 2026-10-20

### B6 – Tekster og regler i spesifikasjonen er fasit
- **Beslutning:** `spesifikasjon.md` beskriver felt, tekster, når felt vises, validering og kontraktfelt per steg. Den oppdateres før koden når Figma endres.
- **Hvorfor:** Figma er ikke tilgjengelig for agenten, og skjermbilder sier ikke noe om validering eller kontrakt.
- **Avklart:** 2026-10-20

---

## Åpne spørsmål

### Å1 – Aktivitetsplan ved utdanning?
Figma-notatet sier at vi bare spør om aktivitetsplan ved arbeidsrettet tiltak. Eksempel-JSON-en `utvidet_omstillingsstoenad.json` i felles har likevel `aktivitetsplan` ved `UTDANNING`. **Forslag:** Bare ved tiltak. Rett eksempelet.

### Å2 – Ja/Nei eller `JaNeiVetIkke` for felles barn/tidligere gift
Figma viser Ja/Nei. Kontrakten gjenbruker `Samboer`, som har `fellesBarnEllertidligereGift: Opplysning<EnumSvar<JaNeiVetIkke>>` og et påkrevd, nullbart felt `inntekt`. **Forslag:** Gjenbruk `Samboer`, send bare `JA`/`NEI` og `inntekt = null`.

### Å3 – Hvilken sak kreves for å søke?
Figma-notatet sier at det må finnes «en sak på omstillingsstønad». Appen kaller i dag `har-loepende-sak`. Hva med brukere med opphørt sak, for eksempel etter 3 år, som søker rett etter opphør? **Avklares med fag.**

### Å4 – Dokumentasjon og vedlegg
Steg 6 er ikke kartlagt. Det er ikke støtte for vedlegg i kontrakten eller i innsendt-soeknad. Oppsummeringen i Figma viser «Studieplan.pdf». **Foreløpig:** tom side.

### Å5 – Utbetalingsinformasjon
Dette er ikke med i Figma. Kontrakten har `utbetalingsInformasjon` som valgfri. **Forslag:** Send `null`, siden bruker allerede har utbetaling.

### Å6 – Avvik i spørsmålstekster mellom steg og oppsummering
Oppsummeringen bruker «Hva stemmer med din situasjon i dag?», mens steg 3 bruker «Har du ny samboer eller giftet deg på nytt?». Oppsummeringen sier også «Hvor studerer du?», mens steget sier «Hvilken skole studerer du ved?». **Forslag:** Bruk samme tekst som i steget. Sjekk med innholdsdesigner.

### Å7 – `soeknadType` ved oppslag av innlogget person
`hentInnloggetPerson` sender `soeknadType=OMSTILLINGSSTOENAD`. Sjekk om innsendt-soeknad trenger `UTVIDET_OMSTILLINGSSTOENAD` her, for eksempel for adressebeskyttelse eller aldersregler.

### Å8 – Behandlingsløp i Gjenny
Teamet heller mot at søknaden skal opprette en revurdering basert på forrige behandling, men det er ikke avklart. Det er også uavklart om dette skal skje automatisk eller via oppgaven (B4), og hvilken revurderingsårsak som skal brukes.

### Å9 – Lansering
Det mangler avklaring av feature toggle, prod-nais, inngang fra nav.no eller Min side, og varsling (etterlatte-notifikasjoner).
