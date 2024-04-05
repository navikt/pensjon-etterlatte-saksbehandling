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

  // GOSYS-spesifikt
  beskrivelse: string | null
  journalpostId: string | null
  gjelder: string | null
  versjon: number | null
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
  GOSYS = 'GOSYS',
  KRAVPAKKE_UTLAND = 'KRAVPAKKE_UTLAND',
  KLAGE = 'KLAGE',
  TILBAKEKREVING = 'TILBAKEKREVING',
  OMGJOERING = 'OMGJOERING',
  JOURNALFOERING = 'JOURNALFOERING',
  GJENOPPRETTING_ALDERSOVERGANG = 'GJENOPPRETTING_ALDERSOVERGANG',
  AKTIVITETSPLIKT = 'AKTIVITETSPLIKT',
}

export const erOppgaveRedigerbar = (status: Oppgavestatus): boolean => {
  switch (status) {
    case Oppgavestatus.NY:
    case Oppgavestatus.UNDER_BEHANDLING:
    case Oppgavestatus.ATTESTERING:
    case Oppgavestatus.UNDERKJENT:
    case Oppgavestatus.PAA_VENT:
      return true
    default:
      return false
  }
}
