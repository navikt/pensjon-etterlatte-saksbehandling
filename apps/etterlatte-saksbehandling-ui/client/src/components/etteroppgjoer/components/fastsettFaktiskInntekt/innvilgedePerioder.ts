import { addMonths, format, parseISO } from 'date-fns'
import { EtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { IAvkortetYtelse } from '~shared/types/IAvkorting'

export function finnInnvilgedePerioder(
  ytelser: Pick<IAvkortetYtelse, 'fom' | 'tom'>[],
  oppgjoersperiode: EtteroppgjoerForbehandling['innvilgetPeriode']
): EtteroppgjoerForbehandling['innvilgetPeriode'][] {
  const perioder = ytelser
    .map(({ fom, tom }) => ({
      fom: fom > oppgjoersperiode.fom ? fom : oppgjoersperiode.fom,
      tom: !tom || tom > oppgjoersperiode.tom ? oppgjoersperiode.tom : tom,
    }))
    .filter(({ fom, tom }) => fom <= tom)
    .sort((a, b) => a.fom.localeCompare(b.fom))

  const sammenhengende: EtteroppgjoerForbehandling['innvilgetPeriode'][] = []
  for (const periode of perioder) {
    const forrige = sammenhengende.at(-1)
    if (forrige && periode.fom <= format(addMonths(parseISO(forrige.tom), 1), 'yyyy-MM')) {
      forrige.tom = periode.tom > forrige.tom ? periode.tom : forrige.tom
    } else {
      sammenhengende.push({ ...periode })
    }
  }
  return sammenhengende
}
