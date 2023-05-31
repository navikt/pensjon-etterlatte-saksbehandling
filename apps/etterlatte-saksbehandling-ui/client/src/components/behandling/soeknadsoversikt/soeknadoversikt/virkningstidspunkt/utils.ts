import { addMonths, isBefore, subYears } from 'date-fns'
import { Hjemmel } from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'

export function hentMinimumsVirkningstidspunkt(avdoedDoedsdato: string | undefined, soeknadMottattDato: string): Date {
  const doedsdato = new Date(avdoedDoedsdato ?? '')

  const treArFoerSoknad = subYears(new Date(soeknadMottattDato), 3)
  const manedEtterDoedsdato = addMonths(doedsdato, 1)

  return isBefore(doedsdato, treArFoerSoknad) ? treArFoerSoknad : manedEtterDoedsdato
}

export const BP_FOERSTEGANGSBEHANDLING_HJEMLER: Array<Hjemmel> = [
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12', tittel: 'Folketrygdloven § 22-12 første ledd' },
  { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13', tittel: '§ 22-13 fjerde ledd' },
]

export const BP_REVURDERING_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12',
    tittel: 'Folketrygdloven § 22-12 fjerde, femte og sjette ledd',
  },
]

export const BP_OPPHOER_HJEMLER: Array<Hjemmel> = [
  {
    lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12',
    tittel: 'Folketrygdloven § 22-12 sjette ledd',
  },
]

export const BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE =
  'Barnepensjon kan tidligst innvilges fra og med den første i måneden etter dødsfallet og den kan gis for opptil tre år før søknaden er mottatt.'
export const BP_REVURDERING_BESKRIVELSE =
  'Barnepensjonen blir satt opp fra og med den måneden vilkårene for dette er oppfylt, og satt ned fra og med måneden etter den måneden vilkårene for dette er oppfylt. Barnepensjonen stanses ved utgangen av den måneden retten til ytelsen faller bort.'
export const BP_OPPHOER_BESKRIVELSE =
  'Barnepensjonen stanses ved utgangen av den måneden retten til ytelsen faller bort.'

export const OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE =
  'Omstillingsstønad kan innvilges fra og med den første i måneden etter dødsfallet, men kan som hovedregel ikke gis for mer enn tre måneder før søknaden er mottatt hos NAV.'
