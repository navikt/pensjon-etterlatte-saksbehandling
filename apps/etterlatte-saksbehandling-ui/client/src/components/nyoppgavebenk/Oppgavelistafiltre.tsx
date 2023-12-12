import { OppgaveDTO, OppgaveKilde, Oppgavestatus, Oppgavetype } from '~shared/api/oppgaver'
import { isBefore } from 'date-fns'

export const SAKSBEHANDLERFILTER = {
  visAlle: 'Vis alle',
  Tildelt: 'Tildelt oppgave',
  IkkeTildelt: 'Ikke tildelt oppgave',
}

function filtrerSaksbehandler(saksbehandlerFilter: string, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (saksbehandlerFilter === SAKSBEHANDLERFILTER.visAlle) {
    return oppgaver
  } else {
    return oppgaver.filter((o) => {
      if (saksbehandlerFilter === SAKSBEHANDLERFILTER.Tildelt) {
        return o.saksbehandler !== null
      } else if (saksbehandlerFilter === SAKSBEHANDLERFILTER.IkkeTildelt) {
        return o.saksbehandler === null
      } else if (saksbehandlerFilter && saksbehandlerFilter !== '') {
        return o.saksbehandler === saksbehandlerFilter
      } else {
        return true
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

function filtrerEnhet(enhetsFilter: EnhetFilterKeys, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
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

function filtrerYtelse(ytelseFilter: YtelseFilterKeys, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
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

export function filtrerOppgaveStatus(
  oppgavestatusFilterKeys: OppgavestatusFilterKeys,
  oppgaver: OppgaveDTO[]
): OppgaveDTO[] {
  if (oppgavestatusFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.status === oppgavestatusFilterKeys)
  }
}

export type OppgavetypeFilterKeys = Oppgavetype | visAlle
const OPPGAVETYPEFILTER: Record<OppgavetypeFilterKeys, string> = {
  visAlle: 'Vis alle',
  FOERSTEGANGSBEHANDLING: 'Førstegangsbehandling',
  REVURDERING: 'Revurdering',
  MANUELT_OPPHOER: 'Manuelt opphør',
  ATTESTERING: 'Attestering',
  VURDER_KONSEKVENS: 'Vurder konsekvense for hendelse',
  UNDERKJENT: 'Underkjent behandling',
  GOSYS: 'Gosys',
  KRAVPAKKE_UTLAND: 'Kravpakke utland',
  KLAGE: 'Klage',
  TILBAKEKREVING: 'Tilbakekreving',
  OMGJOERING: 'Omgjøring',
  JOURNALFOERING: 'Journalføring',
} as const

export const oppgavetypefilter = (kanBrukeKlage: boolean): Array<[OppgavetypeFilterKeys, string]> => {
  const entries = Object.entries(OPPGAVETYPEFILTER) as Array<[OppgavetypeFilterKeys, string]>
  if (!kanBrukeKlage) {
    return entries.filter(([key]) => key !== 'KLAGE' && key !== 'OMGJOERING')
  }
  return entries
}

export function filtrerOppgaveType(oppgavetypeFilterKeys: OppgavetypeFilterKeys, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (oppgavetypeFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.type === oppgavetypeFilterKeys)
  }
}

export type OppgaveKildeFilterKeys = OppgaveKilde | visAlle
export const OPPGAVEKILDEFILTER: Record<OppgaveKildeFilterKeys, string> = {
  visAlle: 'Vis alle',
  HENDELSE: 'Hendelse',
  BEHANDLING: 'Behandling',
  EKSTERN: 'Ekstern',
  GENERELL_BEHANDLING: 'Generell behandling',
  TILBAKEKREVING: 'Tilbakekreving',
}

function filtrerOppgavekilde(oppgaveKildeFilterKeys: OppgaveKildeFilterKeys, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (oppgaveKildeFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.kilde === oppgaveKildeFilterKeys)
  }
}

function finnFnrIOppgaver(fnr: string, oppgaver: OppgaveDTO[]): OppgaveDTO[] {
  if (fnr && fnr.length > 0) {
    return oppgaver.filter((o) => o.fnr.includes(fnr.trim()))
  } else {
    return oppgaver
  }
}

export type FristFilterKeys = keyof typeof FRISTFILTER

export const FRISTFILTER = {
  visAlle: 'Vis alle',
  fristHarPassert: 'Frist har passert',
  manglerFrist: 'Mangler frist',
}

export function filtrerFrist(fristFilterKeys: FristFilterKeys, oppgaver: OppgaveDTO[]) {
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

export function filtrerOppgaver(
  enhetsFilter: EnhetFilterKeys,
  fristFilter: FristFilterKeys,
  saksbehandlerFilter: string,
  ytelseFilter: YtelseFilterKeys,
  oppgavestatusFilter: OppgavestatusFilterKeys,
  oppgavetypeFilter: OppgavetypeFilterKeys,
  oppgaveKildeFilterKeys: OppgaveKildeFilterKeys,
  oppgaver: OppgaveDTO[],
  fnr: string
): OppgaveDTO[] {
  const enhetFiltrert = filtrerEnhet(enhetsFilter, oppgaver)
  const saksbehandlerFiltrert = filtrerSaksbehandler(saksbehandlerFilter, enhetFiltrert)
  const ytelseFiltrert = filtrerYtelse(ytelseFilter, saksbehandlerFiltrert)
  const oppgaveFiltrert = filtrerOppgaveStatus(oppgavestatusFilter, ytelseFiltrert)
  const oppgaveTypeFiltrert = filtrerOppgaveType(oppgavetypeFilter, oppgaveFiltrert)
  const oppgaveKildeFiltrert = filtrerOppgavekilde(oppgaveKildeFilterKeys, oppgaveTypeFiltrert)
  const fristFiltrert = filtrerFrist(fristFilter, oppgaveKildeFiltrert)

  return finnFnrIOppgaver(fnr, fristFiltrert)
}

export interface Filter {
  enhetsFilter: EnhetFilterKeys
  fristFilter: FristFilterKeys
  saksbehandlerFilter: string
  ytelseFilter: YtelseFilterKeys
  oppgavestatusFilter: OppgavestatusFilterKeys
  oppgavetypeFilter: OppgavetypeFilterKeys
  oppgavekildeFilter: OppgaveKildeFilterKeys
  fnrFilter: string
}

export const initialFilter = (): Filter => {
  return {
    enhetsFilter: 'visAlle',
    fristFilter: 'visAlle',
    saksbehandlerFilter: SAKSBEHANDLERFILTER.IkkeTildelt,
    ytelseFilter: 'visAlle',
    oppgavestatusFilter: 'visAlle',
    oppgavetypeFilter: 'visAlle',
    oppgavekildeFilter: 'visAlle',
    fnrFilter: '',
  }
}
