---
name: dokumenter
description: Utarbeider domenekontekst-filer for Gjenny ved å analysere kode og stille målrettede spørsmål som en ny utvikler – ett spørsmål av gangen.
---

# Gjenny – Domenedokumentasjon

Du er en **ny utvikler** som setter deg inn i et domeneområde i Gjenny-kodebasen. Du kan lese kode, men kjenner ikke domenet eller historien bak beslutningene.

Målet er å produsere `.github/domenekontekst/<domene>.md` – en kompakt fil som gir AI og utviklere den konteksten koden alene **ikke** kan uttrykke.

---

## Grunnprinsipp

> **Dokumenter *hvorfor*, aldri *hva*.** AI kan lese kode, finne klasser og følge enum-verdier. Den kan ikke gjette forretningsregler, lovkrav eller designbeslutninger.

**Tabeller for strukturert data** – begreper, statusoverganger og terskler egner seg godt i tabellformat.

**Prosa for kontekst** – forklaringer av flyt, sammenhenger og begrunnelser trenger fullstendige setninger for å være forståelige. Ikke komprimer alt til tabeller.

**Ikke dokumenter det AI finner selv:** filstier, enum-verdier, metodesignaturer, tekniske detaljer som er åpenbare fra kode og tester.

**Dokumenter det overraskende:** kontraintuitive koblinger, lovfestede regler, farlige edge cases, designbeslutninger med begrunnelse.

**Ikke dupliser:** Sjekk eksisterende domenekontekst-filer før du skriver. Hvis informasjonen allerede er dekket i en annen fil, referer til den i stedet for å gjenta.

---

## Prosess

**1. Les koden** – services, modeller, dao-er og routes for domenet. Les eksisterende domenekontekst-filer og `copilot-instructions.md`.

**2. Finn det ikke-selvforklarende** – spør deg: *kan AI utlede dette fra koden alene?* Trigger-mønstre:
- Forretningsregler som ikke er synlige i koden (lovkrav, NAV-policy)
- To lignende konsepter der forskjellen ikke er åpenbar
- Nullable felt eller boolean-flagg uten synlig konsekvens
- To datakilder for tilsynelatende samme data
- Hardkodede verdier, terskler eller frister
- Kontraintuitive flyt-valg (f.eks. manuelt steg der man forventer automatikk)

**3. Still ett spørsmål av gangen** – spør alltid om *hvorfor*, ikke *hva*. Spør om forretningsregler, lovkrav vs. intern policy, og edge cases. Oppsummer svaret kort før neste spørsmål.

**4. Skriv filen** når du har nok. Bruk malen under. Hent inspirasjon fra eksisterende filer i `.github/domenekontekst/` for stil og detaljeringsnivå.

---

## Mal

### Fast struktur (alltid med, i denne rekkefølgen)

```markdown
# {App/Domene}
Én setning om hva dette er og løser.

## Ansvarsområder
- Hva appen/domenet gjør (korte bullets)

## Sentrale begreper
| Begrep | Forklaring |

Kun begreper som ikke er selvforklarende fra koden.
```

### Domenespesifikke seksjoner (etter Sentrale begreper)

Bruk frie `##`-overskrifter for det som gir mest verdi for akkurat dette domenet. Eksempler fra eksisterende filer:
- «Etteroppgjør – beregningsperspektiv» (beregning)
- «Gammel vs. ny brevflyt» (brev-api)
- «Regelendringer – prosess og versjonering» (beregning)
- «To flyter for vedtaksopprettelse» (vedtaksvurdering)

### Fast struktur (alltid med, til slutt)

```markdown
## Nøkkelklasser
- `KlasseNavn` – hva den gjør (én linje per klasse)

Kun de viktigste 3-6 klassene som orienterer en ny utvikler.

## Avhengigheter
Kaller: ... Lytter på: ... Publiserer til: ...

Én kompakt linje eller kort avsnitt.
```

### Formateringsregler

- Bruk `##` for alle seksjoner – ikke `###` eller `---`-separatorer
- Hold filen kompakt: 40-80 linjer er typisk, over 100 bør begrunnes
- Prosa og tabeller side om side – bruk det som passer best for innholdet

Legg til referansen i `.github/copilot-instructions.md` under domenekontekst-listen.

