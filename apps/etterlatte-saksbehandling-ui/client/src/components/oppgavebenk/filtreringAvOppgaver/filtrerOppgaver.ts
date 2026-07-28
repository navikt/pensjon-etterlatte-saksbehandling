import { isBefore } from 'date-fns'
import {
  EnhetFilterKeys,
  Filter,
  FristFilterKeys,
  OPPGAVESTATUSFILTER,
  OppgavestatusFilterKeys,
  OPPGAVETYPEFILTER,
  OppgavetypeFilterKeys,
  SAKSBEHANDLERFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { OppgaveDTO, OppgaveSoekRequest } from '~shared/types/oppgave'
import { OppgaveSortering } from '~components/oppgavebenk/utils/oppgaveSortering'

export const konverterOppgavestatusFilterValuesTilKeys = (
  oppgavestatusFilter: Array<string>
): Array<OppgavestatusFilterKeys> => {
  return Object.entries(OPPGAVESTATUSFILTER)
    .filter(([, val]) => oppgavestatusFilter.includes(val))
    .map(([key]) => key as OppgavestatusFilterKeys)
}

export const initialFilter = (): Filter => {
  return {
    ...defaultFiltre,
    oppgavestatusFilter: [OPPGAVESTATUSFILTER.NY, OPPGAVESTATUSFILTER.UNDER_BEHANDLING],
  }
}

export const initialMinOppgavelisteFiltre = (): Filter => {
  return {
    ...defaultFiltre,
    oppgavestatusFilter: [
      OPPGAVESTATUSFILTER.NY,
      OPPGAVESTATUSFILTER.UNDER_BEHANDLING,
      OPPGAVESTATUSFILTER.ATTESTERING,
      OPPGAVESTATUSFILTER.UNDERKJENT,
      OPPGAVESTATUSFILTER.PAA_VENT,
    ],
  }
}

export const defaultFiltre: Filter = {
  sakEllerFnrFilter: '',
  enhetsFilter: 'visAlle',
  fristFilter: 'visAlle',
  saksbehandlerFilter: SAKSBEHANDLERFILTER.visAlle,
  ytelseFilter: 'visAlle',
  oppgavestatusFilter: [OPPGAVESTATUSFILTER.visAlle],
  oppgavetypeFilter: [OPPGAVETYPEFILTER.visAlle],
}

function sorteringTilOrderBy(sortering: OppgaveSortering): {
  orderBy: OppgaveSoekRequest['orderBy']
  orderAsc: boolean
} {
  if (sortering.registreringsdatoSortering !== 'none') {
    return { orderBy: 'OPPRETTET', orderAsc: sortering.registreringsdatoSortering === 'ascending' }
  }
  if (sortering.fristSortering !== 'none') {
    return { orderBy: 'FRIST', orderAsc: sortering.fristSortering === 'ascending' }
  }
  if (sortering.fnrSortering !== 'none') {
    return { orderBy: 'FNR', orderAsc: sortering.fnrSortering === 'ascending' }
  }
  return { orderBy: 'OPPRETTET', orderAsc: false }
}

function fristFilterTilOppgaveFristFilter(fristFilter: FristFilterKeys): OppgaveSoekRequest['fristFilter'] {
  switch (fristFilter) {
    case 'fristHarPassert':
      return 'HAR_PASSERT'
    case 'manglerFrist':
      return 'MANGLER_FRIST'
    default:
      return 'ALLE'
  }
}

function konverterOppgavetypeFilterValuesTilKeys(typer: Array<string>): Array<OppgavetypeFilterKeys> {
  return Object.entries(OPPGAVETYPEFILTER)
    .filter(([, val]) => typer.includes(val))
    .map(([key]) => key as OppgavetypeFilterKeys)
}

export function byggOppgaveSoekRequest(
  filter: Filter,
  sortering: OppgaveSortering,
  side: number,
  antall: number,
  kunInnloggetBruker: boolean
): OppgaveSoekRequest {
  const statuser =
    filter.oppgavestatusFilter.length === 0 || filter.oppgavestatusFilter.includes(OPPGAVESTATUSFILTER.visAlle)
      ? []
      : (konverterOppgavestatusFilterValuesTilKeys(filter.oppgavestatusFilter) as string[])

  const typer =
    filter.oppgavetypeFilter.length === 0 || filter.oppgavetypeFilter.includes(OPPGAVETYPEFILTER.visAlle)
      ? []
      : (konverterOppgavetypeFilterValuesTilKeys(filter.oppgavetypeFilter) as string[])

  let saksbehandlerFilter: OppgaveSoekRequest['saksbehandlerFilter'] = 'ALLE'
  let saksbehandlerIdent: string | undefined = undefined

  if (filter.saksbehandlerFilter === SAKSBEHANDLERFILTER.Tildelt) {
    saksbehandlerFilter = 'TILDELT'
  } else if (filter.saksbehandlerFilter === SAKSBEHANDLERFILTER.IkkeTildelt) {
    saksbehandlerFilter = 'IKKE_TILDELT'
  } else if (
    filter.saksbehandlerFilter &&
    filter.saksbehandlerFilter !== SAKSBEHANDLERFILTER.visAlle &&
    filter.saksbehandlerFilter !== ''
  ) {
    saksbehandlerIdent = filter.saksbehandlerFilter
  }

  const enhet = filter.enhetsFilter === 'visAlle' ? undefined : filter.enhetsFilter.substring(1) // strip 'E' prefix

  const sakType = filter.ytelseFilter === 'visAlle' ? undefined : (filter.ytelseFilter as string)

  const { orderBy, orderAsc } = sorteringTilOrderBy(sortering)

  return {
    statuser,
    typer,
    saksbehandlerFilter,
    saksbehandlerIdent,
    kunInnloggetBruker,
    sakType,
    enhet,
    fristFilter: fristFilterTilOppgaveFristFilter(filter.fristFilter),
    sakEllerFnr: filter.sakEllerFnrFilter || undefined,
    side,
    antall,
    orderBy,
    orderAsc,
  }
}
