# Bestemmelse for design i Gjenny

## Bruk av Tags

#### For Gosys/Ytelse tema 
- `~shared/tags/GosysTemaTag` --> `<GosysTemaTag tema={GosysTema.EYO} />`

#### For oppgavestatus
- `~shared/tags/OppgavestatusTag` --> `<OppgavestatusTag oppgavestatus={Oppgavestatus.NY} />`

#### For oppgavetype
- `~shared/tags/OppgavetypeTag` --> `<OppgavetypeTag oppgavetype={Oppgavetype.FOERSTEGANGSBEHANDLING} />`

#### For type sak
- `~shared/tags/SakTypeTag` --> `<SakTypeTag sakType={SakType.BARNEPENSJON} />`

#### For utenlandstilknytning
- `~shared/tags/UtenlandstilknytningTypeTag` --> `<UtenlandstilknytningTypeTag utenlandstilknytningType={UtenlandstilknytningType.NASJONAL} />`

## Bruk av Tabeller
Siden det er mange måter å lage tabeller med å bruke Aksel, så blir det klønete å lage ett eget komponent for det, men her er litt guidelines:

- Hver celle skal kun innholde en type dato, f.eks "Periode" burde skilles ut til "Fra" og "Til"
- `shadeOnHover` skal være satt til `true` (default er true)
- `zebraStripes` skal være satt til `true` hvis man forventer over 4 rader i tabellen, dette er for å gjøre lesbarheten enklere
- `size` skal være satt til `small` hvor det er hensiktsmessig
