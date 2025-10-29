import { Oppgavestatus, Oppgavetype } from '~shared/types/oppgave'
import { GosysTema } from '~shared/types/Gosys'

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

export type TemaFilterKeys = GosysTema
export const GOSYS_TEMA_FILTER: Record<TemaFilterKeys, string> = {
  PEN: 'Pensjon',
  EYO: 'Omstillingsstønad',
  EYB: 'Barnepensjon',
}

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
  ATTESTERING: 'Attestering',
  UNDERKJENT: 'Underkjent',
  PAA_VENT: 'På vent',
  FERDIGSTILT: 'Ferdigstilt',
  FEILREGISTRERT: 'Feilregistrert',
  AVBRUTT: 'Avbrutt',
}

export type OppgavetypeFilterKeys = Oppgavetype | visAlle
export const OPPGAVETYPEFILTER: Record<OppgavetypeFilterKeys, string> = {
  visAlle: 'Vis alle',
  FOERSTEGANGSBEHANDLING: 'Førstegangsbehandling',
  REVURDERING: 'Revurdering',
  VURDER_KONSEKVENS: 'Hendelse',
  MANGLER_SOEKNAD: 'Mangler søknad',
  KRAVPAKKE_UTLAND: 'Kravpakke utland',
  KLAGE: 'Klage',
  KLAGE_SVAR_KABAL: 'Klage svar fra KA',
  TILBAKEKREVING: 'Tilbakekreving',
  OMGJOERING: 'Omgjøring',
  JOURNALFOERING: 'Journalføring',
  TILLEGGSINFORMASJON: 'Tilleggsinformasjon',
  GJENOPPRETTING_ALDERSOVERGANG: 'Gjenoppretting',
  AKTIVITETSPLIKT: 'Aktivitetsplikt 6 måneder',
  AKTIVITETSPLIKT_12MND: 'Aktivitetsplikt 12 måneder',
  AKTIVITETSPLIKT_REVURDERING: 'Aktivitetsplikt revurdering',
  AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK: 'Aktivitetsplikt informasjon - varig unntak',
  GENERELL_OPPGAVE: 'Generell oppgave',
  MANUELL_UTSENDING_BREV: 'Manuell brevutsending',
  AARLIG_INNTEKTSJUSTERING: 'Årlig inntektsjustering',
  INNTEKTSOPPLYSNING: 'Inntektsopplysning',
  OPPFOELGING: 'Oppfølging av sak',
  MELDT_INN_ENDRING: 'Meldt inn endring',
  ETTEROPPGJOER: 'Etteroppgjør',
  ETTEROPPGJOER_SVARFRIST_UTLOEPT: 'Etteroppgjør svarfrist utløpt',
} as const

export const FRISTFILTER = {
  visAlle: 'Vis alle',
  fristHarPassert: 'Frist har passert',
  manglerFrist: 'Mangler frist',
}
export type FristFilterKeys = keyof typeof FRISTFILTER

export interface Filter {
  sakEllerFnrFilter: string
  enhetsFilter: EnhetFilterKeys
  fristFilter: FristFilterKeys
  saksbehandlerFilter: string
  ytelseFilter: YtelseFilterKeys
  oppgavestatusFilter: Array<string>
  oppgavetypeFilter: Array<string>
}

export enum GosysOppgaveValg {
  ALLE_OPPGAVER = 'ALLE_OPPGAVER',
  MINE_OPPGAVER = 'MINE_OPPGAVER',
  IKKE_TILDELTE = 'IKKE_TILDELTE',
}

export interface GosysFilter {
  enhetFilter?: EnhetFilterKeys
  saksbehandlerFilter?: string
  temaFilter?: TemaFilterKeys
  harTildelingFilter?: boolean
}
