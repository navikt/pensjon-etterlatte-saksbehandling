import { addMonths, isBefore, subYears } from 'date-fns'
import { Hjemmel } from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'

export function hentMinimumsVirkningstidspunkt(avdoedDoedsdato: string | undefined, soeknadMottattDato: Date): Date {
  const doedsdato = new Date(avdoedDoedsdato ?? '')
  //Pga bug i ds-react-month picker så må det være første i måneden.
  const treAarFoerSoeknad = subYears(soeknadMottattDato, 3)
  const maanedEtterDoedsdato = addMonths(doedsdato, 1)
  return isBefore(doedsdato, treAarFoerSoeknad) ? treAarFoerSoeknad : maanedEtterDoedsdato
}

// FELLES

export const FELLES_REVURDERING_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12',
    tittel: 'Folketrygdloven § 22-12 fjerde, femte og sjette ledd',
  },
]
export const FELLES_SLUTTBEHANDLING_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§22-12',
    tittel: 'Folketrygdloven § 22-12 første ledd',
  },
  {
    lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§22-13',
    tittel: 'Folketrygdloven § 22-13 fjerde ledd',
  },
  {
    lenke: 'https://lovdata.no/pro/#document/NLX3/eu/32004r0883/a4',
    tittel: 'EØS forordning 883/2004',
  },
  {
    lenke: 'https://lovdata.no/pro/#document/NLX3/eu/32009r0987',
    tittel: 'EØS-forordning 987/2009',
  },
]

export const FELLES_SLUTTBEHANDLING_BESKRIVELSE =
  'Ved sluttbehandling skal virkningstidspunktet settes lik virkningstidspunkt på førstegangsbehandlingen av ytelsen.'

// BARNEPENSJON

export const BP_FOERSTEGANGSBEHANDLING_HJEMLER: Array<Hjemmel> = [
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12', tittel: 'Folketrygdloven § 22-12 første ledd' },
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13', tittel: '§ 22-13 fjerde ledd' },
]

export const BP_INSTITUSJONSOPPHOLD_HJEMLER: Array<Hjemmel> = [
  {
    tittel: '§ 18-8.Barnepensjon under opphold i institusjon',
    lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-6#%C2%A718-8',
  },
]

export const BP_OPPHOER_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12',
    tittel: 'Folketrygdloven § 22-12 sjette ledd',
  },
]

export const BP_REVURDERING_YRKESSKADE_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13',
    tittel: 'Folketrygdloven § 22-13',
  },
]

export const BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER: Array<Hjemmel> = BP_FOERSTEGANGSBEHANDLING_HJEMLER.concat(
  [
    {
      lenke: 'https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_81',
      tittel: 'EØS forordning 883/2004 art 81',
    },
  ]
)

export const BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE =
  'Barnepensjon kan tidligst innvilges fra og med den første i måneden etter dødsfallet og den kan gis for opptil tre år før den måneden søknaden er satt fram. \n\nHvis bosatt utland: bruk kravdato i bostedslandet, eventuelt landet søker sist var medlem.'

export const BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE =
  'Barnepensjon kan tidligst innvilges fra og med den første i måneden etter dødsfallet og den kan gis for opptil tre år før den måneden søknaden er satt fram.'

export const BP_FORELDRELOES_PAA_GAMMELT_REGELVERK_BEHANDLES_I_PESYS_BESKRIVELSE =
  '\n\nGammelt regelverk for foreldreløs kan ikke behandles i Gjenny. Skal det innvilges barnepensjon før 01.01.2024, skal saken først behandles i Pesys frem til og med 31.12.2023. Så innvilges i Gjenny fra 01.01.2024.'

export const BP_REVURDERING_BESKRIVELSE =
  'Barnepensjonen blir satt opp fra og med den måneden vilkårene for dette er oppfylt, og satt ned fra og med måneden etter den måneden vilkårene for dette er oppfylt. Barnepensjonen stanses ved utgangen av den måneden retten til ytelsen faller bort.'

export const BP_REVURDERING_YRKESSKADE_BESKRIVELSE =
  'Når dødsfall skyldes en godkjent yrkes-skade/sykdom, skal dette med i beregningen fra innvilgelsestidspunkt av ytelsen. Dette er fordi man tar utgangspunkt i kravet om ytelsen og ikke selve yrkesskade-fordelen.'

export const BP_OPPHOER_BESKRIVELSE =
  'Barnepensjonen stanses ved utgangen av den måneden retten til ytelsen faller bort.'

export const BP_INSTITUSJONSOPPHOLD_BESKRIVELSE =
  'Barnepensjon gis uten reduksjon i innleggelsesmåneden og de tre påfølgende månedene. Deretter kan stønaden bli redusert. Blir man innlagt igjen innen tre måneder etter utskrivelsen skal stønaden reduseres fra måneden etter innleggelse. Barnepensjon gis uten reduksjon for institusjonsopphold f.o.m. utskrivingsmåneden.'

// OMSTILLINGSSTØNAD

export const OMS_FOERSTEGANGSBEHANDLING_HJEMLER: Array<Hjemmel> = [
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12', tittel: 'Folketrygdloven § 22-12 første ledd' },
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13', tittel: '§ 22-13 tredje ledd' },
]

export const OMS_OPPHOER_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12',
    tittel: 'Folketrygdloven § 22-12 sjette ledd',
  },
]

export const OMS_ALDERSOVERGANG_HJEMLER: Array<Hjemmel> = [
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§17-11', tittel: 'Folketrygdloven § 17-11' },
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12', tittel: 'Folketrygdloven § 22-12 sjette ledd' },
]

export const OMS_INST_HJEMLER_VIRK: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§17-13',
    tittel: 'Folketrygdloven § 17-3',
  },
]

export const OMS_INNTEKTSENDRING_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/pro/#document/NL/lov/1997-02-28-19/§17-9',
    tittel: 'Folketrygdloven § 17-9, jf. utkast til forskrift § 10',
  },
]

export const OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER: Array<Hjemmel> =
  OMS_FOERSTEGANGSBEHANDLING_HJEMLER.concat([
    { lenke: 'https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_81', tittel: 'EØS forordning 883/2004 art 81' },
  ])

export const OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE =
  'Omstillingsstønad kan tidligst innvilges fra og med den første i måneden etter dødsfallet, og kan gis for opptil tre måneder før måneden kravet ble satt frem. I noen tilfeller kan man gå lenger tilbake, jf. folketrygdloven § 22-13 syvende ledd.\n\nHvis bosatt utland: bruk kravdato i bostedslandet, eventuelt landet søker sist var medlem.'

export const OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE =
  'Omstillingsstønad kan tidligst innvilges fra og med den første i måneden etter dødsfallet, og kan gis for opptil tre måneder før måneden kravet ble satt frem. I noen tilfeller kan man gå lenger tilbake, jf. folketrygdloven § 22-13 syvende ledd.'

export const OMS_REVURDERING_BESKRIVELSE =
  'Omstillingsstønad blir satt opp fra og med den måneden vilkårene for dette er oppfylt, og satt ned fra og med måneden etter den måneden vilkårene for dette er oppfylt. Omstillingsstønad stanses ved utgangen av den måneden retten til ytelsen faller bort.'

export const OMS_OPPHOER_BESKRIVELSE =
  'Omstillingsstønaden stanses ved utgangen av den måneden retten til ytelsen faller bort.'

export const OMS_ALDERSOVERGANG_BESKRIVELSE =
  'Retten til omstillingsstønad faller bort når bruker fyller 67 år. Den stanses ved utgangen av den måneden retten til ytelsen faller bort.'

export const OMS_INNTEKTSENDRING_BESKRIVELSE =
  'Alle inntektsendringer utenom etteroppgjør skal gjøres fra måneden etter bruker har meldt fra om endringen.'

export const OMS_INST_VIRK_BESKRIVELSE =
  'Omstillingsstønad gis uten reduksjon i innleggelsesmåneden og de tre påfølgende månedene. Deretter kan stønaden bli redusert. Blir man innlagt igjen innen tre måneder etter utskrivelsen skal stønaden reduseres fra måneden etter innleggelse. Omstillingsstønad gis uten reduksjon for institusjonsopphold f.o.m. utskrivingsmåneden.'
