import { Oppgavestatus, Oppgavetype } from '~shared/api/oppgaver'

type visAlle = 'visAlle'

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

export const YTELSEFILTER = {
  visAlle: 'Vis alle',
  BARNEPENSJON: 'Barnepensjon',
  OMSTILLINGSSTOENAD: 'Omstillingsstønad',
}
export type YtelseFilterKeys = keyof typeof YTELSEFILTER

export const SAKSBEHANDLERFILTER = {
  visAlle: 'Vis alle',
  Tildelt: 'Tildelt oppgave',
  IkkeTildelt: 'Ikke tildelt oppgave',
}

export type OppgavestatusFilterKeys = Oppgavestatus | visAlle
export const OPPGAVESTATUSFILTER: Record<OppgavestatusFilterKeys, string> = {
  visAlle: 'Vis alle',
  NY: 'Ny',
  UNDER_BEHANDLING: 'Under behandling',
  PAA_VENT: 'På Vent',
  FERDIGSTILT: 'Ferdigstilt',
  FEILREGISTRERT: 'Feilregistrert',
  AVBRUTT: 'Avbrutt',
}

export type OppgavetypeFilterKeys = Oppgavetype | visAlle
export const OPPGAVETYPEFILTER: Record<OppgavetypeFilterKeys, string> = {
  visAlle: 'Vis alle',
  FOERSTEGANGSBEHANDLING: 'Førstegangsbehandling',
  REVURDERING: 'Revurdering',
  ATTESTERING: 'Attestering',
  VURDER_KONSEKVENS: 'Vurder konsekvense for hendelse',
  UNDERKJENT: 'Underkjent behandling',
  GOSYS: 'Gosys',
  KRAVPAKKE_UTLAND: 'Kravpakke utland',
  KLAGE: 'Klage',
  TILBAKEKREVING: 'Tilbakekreving',
  OMGJOERING: 'Omgjøring',
  JOURNALFOERING: 'Journalføring',
  GJENOPPRETTING_ALDERSOVERGANG: 'Gjenoppretting',
} as const

export const FRISTFILTER = {
  visAlle: 'Vis alle',
  fristHarPassert: 'Frist har passert',
  manglerFrist: 'Mangler frist',
}
export type FristFilterKeys = keyof typeof FRISTFILTER

export interface Filter {
  sakidFilter: string
  enhetsFilter: EnhetFilterKeys
  fristFilter: FristFilterKeys
  saksbehandlerFilter: string
  ytelseFilter: YtelseFilterKeys
  oppgavestatusFilter: Array<string>
  oppgavetypeFilter: Array<string>
  fnrFilter: string
}
