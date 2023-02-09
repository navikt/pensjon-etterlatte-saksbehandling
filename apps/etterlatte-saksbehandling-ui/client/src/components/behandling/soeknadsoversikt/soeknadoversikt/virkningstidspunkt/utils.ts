import { addMonths, isBefore, subYears } from 'date-fns'

export function hentMinimumsVirkningstidspunkt(avdoedDoedsdato: string | undefined, soeknadMottattDato: string): Date {
  const doedsdato = new Date(avdoedDoedsdato ?? '')

  const treArFoerSoknad = subYears(new Date(soeknadMottattDato), 3)
  const manedEtterDoedsdato = addMonths(doedsdato, 1)

  return isBefore(doedsdato, treArFoerSoknad) ? treArFoerSoknad : manedEtterDoedsdato
}
