import { OppgaveDTOny, Oppgavestatus, Oppgavetype } from '~shared/api/oppgaverny'
import { isBefore } from 'date-fns'

export const SAKSBEHANDLERFILTER = {
  visAlle: 'Vis alle',
  Tildelt: 'Tildelt saksbehandler ',
  IkkeTildelt: 'Ikke tildelt saksbehandler',
}
export type SaksbehandlerFilterKeys = keyof typeof SAKSBEHANDLERFILTER

function filtrerSaksbehandler(saksbehandlerFilter: SaksbehandlerFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (saksbehandlerFilter === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => {
      if (saksbehandlerFilter === 'Tildelt') {
        return o.saksbehandler !== null
      } else if (saksbehandlerFilter === 'IkkeTildelt') {
        return o.saksbehandler === null
      }
    })
  }
}

export const ENHETFILTER = {
  visAlle: 'Vis alle',
  E4815: 'Ålesund - 4815',
  E4808: 'Porsgrunn - 4808',
  E4817: 'Steinkjer - 4817',
  E4862: 'Ålesund utland - 4862',
  E0001: 'Utland - 0001',
  E4883: 'Egne ansatte - 4883',
  E2103: 'Vikafossen - 2103',
}

export type EnhetFilterKeys = keyof typeof ENHETFILTER

function filtrerEnhet(enhetsFilter: EnhetFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (enhetsFilter === 'visAlle') {
    return oppgaver
  } else {
    const enhetUtenPrefixE = enhetsFilter.substring(1)
    return oppgaver.filter((o) => o.enhet === enhetUtenPrefixE)
  }
}

export const YTELSEFILTER = {
  visAlle: 'Vis alle',
  BARNEPENSJON: 'Barnepensjon',
  OMSTILLINGSSTOENAD: 'Omstillingsstønad',
}

export type YtelseFilterKeys = keyof typeof YTELSEFILTER

function filtrerYtelse(ytelseFilter: YtelseFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (ytelseFilter === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.sakType === ytelseFilter)
  }
}

type visAlle = 'visAlle'
export type OppgavestatusFilterKeys = Oppgavestatus | visAlle

export const OPPGAVESTATUSFILTER: Record<OppgavestatusFilterKeys, string> = {
  visAlle: 'Vis alle',
  NY: 'Ny',
  UNDER_BEHANDLING: 'Under arbeid',
  FERDIGSTILT: 'Ferdigstilt',
  FEILREGISTRERT: 'Feilregistrert',
  AVBRUTT: 'Avbrutt',
}

function filtrerOppgaveStatus(
  oppgavestatusFilterKeys: OppgavestatusFilterKeys,
  oppgaver: OppgaveDTOny[]
): OppgaveDTOny[] {
  if (oppgavestatusFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.status === oppgavestatusFilterKeys)
  }
}

export type OppgavetypeFilterKeys = Oppgavetype | visAlle
export const OPPGAVETYPEFILTER: Record<OppgavetypeFilterKeys, string> = {
  visAlle: 'Vis alle',
  FOERSTEGANGSBEHANDLING: 'Førstegangsbehandling',
  REVURDERING: 'Revurdering',
  HENDELSE: 'Hendelse',
  MANUELT_OPPHOER: 'Manuelt opphør',
  EKSTERN: 'Ekstern',
  ATTESTERING: 'Attestering',
}

function filtrerOppgaveType(oppgavetypeFilterKeys: OppgavetypeFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (oppgavetypeFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.type === oppgavetypeFilterKeys)
  }
}

function finnFnrIOppgaver(fnr: string, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (fnr && fnr.length > 0) {
    return oppgaver.filter((o) => o.fnr.includes(fnr))
  } else {
    return oppgaver
  }
}

export function filtrerOppgaver(
  enhetsFilter: EnhetFilterKeys,
  fristFilter: FristFilterKeys,
  saksbehandlerFilter: SaksbehandlerFilterKeys,
  ytelseFilter: YtelseFilterKeys,
  oppgavestatusFilter: OppgavestatusFilterKeys,
  oppgavetypeFilter: OppgavetypeFilterKeys,
  oppgaver: OppgaveDTOny[],
  fnr: string
): OppgaveDTOny[] {
  const enhetFiltrert = filtrerEnhet(enhetsFilter, oppgaver)
  const saksbehandlerFiltrert = filtrerSaksbehandler(saksbehandlerFilter, enhetFiltrert)
  const ytelseFiltrert = filtrerYtelse(ytelseFilter, saksbehandlerFiltrert)
  const oppgaveFiltrert = filtrerOppgaveStatus(oppgavestatusFilter, ytelseFiltrert)
  const oppgaveTypeFiltrert = filtrerOppgaveType(oppgavetypeFilter, oppgaveFiltrert)
  const fristFiltrert = filtrerFrist(fristFilter, oppgaveTypeFiltrert)

  return finnFnrIOppgaver(fnr, fristFiltrert)
}
export type FristFilterKeys = keyof typeof FRISTFILTER

export const FRISTFILTER = {
  visAlle: 'Vis alle',
  fristHarPassert: 'Frist har passert',
  manglerFrist: 'Mangler frist',
}

export function filtrerFrist(fristFilterKeys: FristFilterKeys, oppgaver: OppgaveDTOny[]) {
  if (fristFilterKeys === 'visAlle') return oppgaver
  else if (fristFilterKeys === 'manglerFrist') {
    const oppgaverUtenFrist = oppgaver.filter((o) => !o.frist)
    return oppgaverUtenFrist
  } else {
    const oppgaverMedFrist = oppgaver.filter((o) => o.frist)
    const sortertEtterFrist = oppgaverMedFrist.sort((a, b) => {
      return new Date(a.frist).valueOf() - new Date(b.frist).valueOf()
    })
    return sortertEtterFrist.filter((o) => isBefore(new Date(o.frist), new Date()))
  }
}
