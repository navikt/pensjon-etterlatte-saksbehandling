import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { SakType } from '~shared/types/sak'

export interface OppgaveDTO {
  id: string
  status: Oppgavestatus
  enhet: string
  sakId: number
  type: Oppgavetype
  kilde: OppgaveKilde
  saksbehandler: string | null
  referanse: string | null
  merknad: string | null
  opprettet: string
  sakType: SakType
  fnr: string
  frist: string

  // GOSYS-spesifikt
  beskrivelse: string | null
  gjelder: string | null
  versjon: string | null
}

export type Oppgavestatus = 'NY' | 'UNDER_BEHANDLING' | 'FERDIGSTILT' | 'FEILREGISTRERT' | 'AVBRUTT'
export type OppgaveKilde = 'HENDELSE' | 'BEHANDLING' | 'EKSTERN' | 'GENERELL_BEHANDLING' | 'TILBAKEKREVING'
export type Oppgavetype =
  | 'FOERSTEGANGSBEHANDLING'
  | 'REVURDERING'
  | 'MANUELT_OPPHOER'
  | 'VURDER_KONSEKVENS'
  | 'ATTESTERING'
  | 'UNDERKJENT'
  | 'GOSYS'
  | 'KRAVPAKKE_UTLAND'
  | 'KLAGE'
  | 'TILBAKEKREVING'
  | 'OMGJOERING'
  | 'MANUELL_JOURNALFOERING'

export const erOppgaveRedigerbar = (status: Oppgavestatus): boolean => ['NY', 'UNDER_BEHANDLING'].includes(status)

export const hentOppgaver = async (): Promise<ApiResponse<OppgaveDTO[]>> => apiClient.get('/oppgaver')
export const hentGosysOppgaver = async (): Promise<ApiResponse<OppgaveDTO[]>> => apiClient.get('/oppgaver/gosys')
export const hentGosysOppgave = async (id: number): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.get(`/oppgaver/gosys/${id}`)

export interface SaksbehandlerEndringDto {
  saksbehandler: string
  versjon: string | null
}

export const tildelSaksbehandlerApi = async (args: {
  oppgaveId: string
  type: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/oppgaver/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const byttSaksbehandlerApi = async (args: {
  oppgaveId: string
  type: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/oppgaver/${args.oppgaveId}/bytt-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const fjernSaksbehandlerApi = async (args: {
  oppgaveId: string
  sakId: number
  type: string
  versjon: string | null
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, {
      saksbehandler: '',
      versjon: args.versjon,
    })
  } else {
    return apiClient.delete(`/oppgaver/${args.oppgaveId}/saksbehandler`)
  }
}

export interface RedigerFristRequest {
  frist: Date
  versjon: string | null
}
export const redigerFristApi = async (args: {
  oppgaveId: string
  type: string
  redigerFristRequest: RedigerFristRequest
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/endre-frist`, { ...args.redigerFristRequest })
  } else {
    return apiClient.put(`/oppgaver/${args.oppgaveId}/frist`, { ...args.redigerFristRequest })
  }
}

export const hentOppgaveForBehandlingUnderBehandlingIkkeattestert = async (args: {
  behandlingId: string
}): Promise<ApiResponse<string>> => apiClient.get(`/oppgaver/behandling/${args.behandlingId}/hentsaksbehandler`)

export const hentSaksbehandlerForOppgaveUnderArbeid = async (args: {
  behandlingId: string
}): Promise<ApiResponse<string | null>> => apiClient.get(`/oppgaver/behandling/${args.behandlingId}/oppgaveunderarbeid`)
