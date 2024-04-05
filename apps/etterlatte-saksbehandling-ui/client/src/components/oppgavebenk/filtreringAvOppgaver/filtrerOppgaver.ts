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
import { OppgaveDTO } from '~shared/types/oppgave'

function filtrerEnhet(enhetsFilter: EnhetFilterKeys, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (enhetsFilter === 'visAlle') {
    return oppgaver
  } else {
    const enhetUtenPrefixE = enhetsFilter.substring(1)
    return oppgaver.filter((o) => o.enhet === enhetUtenPrefixE)
  }
}

function filtrerYtelse(ytelseFilter: YtelseFilterKeys, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (ytelseFilter === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.sakType === ytelseFilter)
  }
}

function filtrerSaksbehandler(saksbehandlerFilter: string, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (saksbehandlerFilter === SAKSBEHANDLERFILTER.visAlle) {
    return oppgaver
  } else {
    return oppgaver.filter((o) => {
      if (saksbehandlerFilter === SAKSBEHANDLERFILTER.Tildelt) {
        return !!o.saksbehandler?.ident
      } else if (saksbehandlerFilter === SAKSBEHANDLERFILTER.IkkeTildelt) {
        return !o.saksbehandler?.ident
      } else if (saksbehandlerFilter && saksbehandlerFilter !== '') {
        return o.saksbehandler?.ident === saksbehandlerFilter
      } else {
        return true
      }
    })
  }
}

export const konverterOppgavestatusFilterValuesTilKeys = (
  oppgavestatusFilter: Array<string>
): Array<OppgavestatusFilterKeys> => {
  return Object.entries(OPPGAVESTATUSFILTER)
    .filter(([, val]) => oppgavestatusFilter.includes(val))
    .map(([key]) => key as OppgavestatusFilterKeys)
}

export function filtrerOppgaveStatus(oppgavestatusFilter: Array<string>, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  const konverterteFiltre = konverterOppgavestatusFilterValuesTilKeys(oppgavestatusFilter)

  if (oppgavestatusFilter.includes(OPPGAVESTATUSFILTER.visAlle) || oppgavestatusFilter.length === 0) {
    return oppgaver
  } else {
    return oppgaver.filter((oppgave) => konverterteFiltre.includes(oppgave.status))
  }
}

const konverterOppgavetypeFilterTilKeys = (oppgavetypeFilter: Array<string>): Array<OppgavetypeFilterKeys> => {
  return Object.entries(OPPGAVETYPEFILTER)
    .filter(([, val]) => oppgavetypeFilter.includes(val))
    .map(([key]) => key as OppgavetypeFilterKeys)
}

export function filtrerOppgaveType(oppgavetypeFilter: Array<string>, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (oppgavetypeFilter.includes(OPPGAVESTATUSFILTER.visAlle) || oppgavetypeFilter.length === 0) {
    return oppgaver
  } else {
    return oppgaver.filter((o) => konverterOppgavetypeFilterTilKeys(oppgavetypeFilter).includes(o.type))
  }
}

function finnSakEllerFnrIOppgaver(sakEllerFnr: string, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (sakEllerFnr && sakEllerFnr.length > 0) {
    if (sakEllerFnr.length === 11) {
      return oppgaver.filter(({ fnr }) => fnr === sakEllerFnr.trim())
    } else {
      return oppgaver.filter(({ sakId }) => sakId?.toString() === sakEllerFnr.trim())
    }
  } else {
    return oppgaver
  }
}

export function filtrerFrist(fristFilterKeys: FristFilterKeys, oppgaver: OppgaveDTO[]) {
  if (fristFilterKeys === 'visAlle') return oppgaver
  else if (fristFilterKeys === 'manglerFrist') {
    return oppgaver.filter((o) => !o.frist)
  } else {
    const oppgaverMedFrist = oppgaver.filter((o) => o.frist)
    const sortertEtterFrist = oppgaverMedFrist.sort((a, b) => {
      return new Date(a.frist).valueOf() - new Date(b.frist).valueOf()
    })
    return sortertEtterFrist.filter((o) => isBefore(new Date(o.frist), new Date()))
  }
}

export function filtrerOppgaver(
  sakEllerFnrFilter: string,
  enhetsFilter: EnhetFilterKeys,
  fristFilter: FristFilterKeys,
  saksbehandlerFilter: string,
  ytelseFilter: YtelseFilterKeys,
  oppgavestatusFilter: Array<string>,
  oppgavetypeFilter: Array<string>,
  oppgaver: OppgaveDTO[]
): OppgaveDTO[] {
  const enhetFiltrert = filtrerEnhet(enhetsFilter, oppgaver)
  const saksbehandlerFiltrert = filtrerSaksbehandler(saksbehandlerFilter, enhetFiltrert)
  const ytelseFiltrert = filtrerYtelse(ytelseFilter, saksbehandlerFiltrert)
  const oppgaveFiltrert = filtrerOppgaveStatus(oppgavestatusFilter, ytelseFiltrert)
  const oppgaveTypeFiltrert = filtrerOppgaveType(oppgavetypeFilter, oppgaveFiltrert)
  const fristFiltrert = filtrerFrist(fristFilter, oppgaveTypeFiltrert)

  return finnSakEllerFnrIOppgaver(sakEllerFnrFilter, fristFiltrert)
}

export const initialFilter = (): Filter => {
  return {
    ...defaultFiltre,
    oppgavestatusFilter: [OPPGAVESTATUSFILTER.NY, OPPGAVESTATUSFILTER.UNDER_BEHANDLING],
  }
}

export const minOppgavelisteFiltre = (): Filter => {
  return {
    ...defaultFiltre,
    oppgavestatusFilter: [OPPGAVESTATUSFILTER.UNDER_BEHANDLING],
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
