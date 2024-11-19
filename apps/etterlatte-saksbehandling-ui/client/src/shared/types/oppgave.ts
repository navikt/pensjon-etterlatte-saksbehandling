import { SakType } from '~shared/types/sak'

export interface OppgaveDTO {
  id: string
  status: Oppgavestatus
  enhet: string
  sakId: number
  type: Oppgavetype
  kilde: OppgaveKilde
  referanse: string | null
  merknad?: string
  opprettet: string
  sakType: SakType
  fnr: string | null
  frist: string
  saksbehandler: OppgaveSaksbehandler | null
}

export interface GenerellEndringshendelse {
  tidspunkt: string
  saksbehandler?: string
  endringer: EndringLinje[]
}

export interface GenerellOppgaveDto {
  merknad: string
  sakIds: number[]
  type: Oppgavetype
  kilde: OppgaveKilde
}

export interface EndringLinje {
  tittel: string
  beskrivelse?: string
}

export interface OppgaveSaksbehandler {
  ident: string
  navn?: string
}

export interface NyOppgaveDto {
  oppgaveKilde?: OppgaveKilde
  oppgaveType: Oppgavetype
  merknad?: string
  referanse?: string
  saksbehandler?: string
}

export enum Oppgavestatus {
  NY = 'NY',
  UNDER_BEHANDLING = 'UNDER_BEHANDLING',
  ATTESTERING = 'ATTESTERING',
  UNDERKJENT = 'UNDERKJENT',
  PAA_VENT = 'PAA_VENT',
  FERDIGSTILT = 'FERDIGSTILT',
  FEILREGISTRERT = 'FEILREGISTRERT',
  AVBRUTT = 'AVBRUTT',
}

export enum OppgaveKilde {
  HENDELSE = 'HENDELSE',
  DOEDSHENDELSE = 'DOEDSHENDELSE',
  BEHANDLING = 'BEHANDLING',
  EKSTERN = 'EKSTERN',
  GENERELL_BEHANDLING = 'GENERELL_BEHANDLING',
  TILBAKEKREVING = 'TILBAKEKREVING',
  SAKSBEHANDLER = 'SAKSBEHANDLER',
}

export enum Oppgavetype {
  FOERSTEGANGSBEHANDLING = 'FOERSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
  VURDER_KONSEKVENS = 'VURDER_KONSEKVENS',
  MANGLER_SOEKNAD = 'MANGLER_SOEKNAD',
  KRAVPAKKE_UTLAND = 'KRAVPAKKE_UTLAND',
  KLAGE = 'KLAGE',
  TILBAKEKREVING = 'TILBAKEKREVING',
  OMGJOERING = 'OMGJOERING',
  JOURNALFOERING = 'JOURNALFOERING',
  TILLEGGSINFORMASJON = 'TILLEGGSINFORMASJON',
  GJENOPPRETTING_ALDERSOVERGANG = 'GJENOPPRETTING_ALDERSOVERGANG',
  AKTIVITETSPLIKT = 'AKTIVITETSPLIKT',
  AKTIVITETSPLIKT_12MND = 'AKTIVITETSPLIKT_12MND',
  AKTIVITETSPLIKT_REVURDERING = 'AKTIVITETSPLIKT_REVURDERING',
  AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK = 'AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK',
  GENERELL_OPPGAVE = 'GENERELL_OPPGAVE',
  AARLIG_INNTEKTSJUSTERING = 'AARLIG_INNTEKTSJUSTERING',
  MOTTATT_INNTEKTSJUSTERING = 'MOTTATT_INNTEKTSJUSTERING',
  MANUELL_UTSENDING_BREV = 'MANUELL_UTSENDING_BREV',
}

export const oppgavestatuserForRedigerbarOppgave: Array<Oppgavestatus> = [
  Oppgavestatus.NY,
  Oppgavestatus.UNDER_BEHANDLING,
  Oppgavestatus.ATTESTERING,
  Oppgavestatus.UNDERKJENT,
  Oppgavestatus.PAA_VENT,
]

export const erOppgaveRedigerbar = (status: Oppgavestatus): boolean => {
  return oppgavestatuserForRedigerbarOppgave.includes(status)
}
