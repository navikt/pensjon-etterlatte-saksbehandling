import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { SakType } from '~shared/types/sak'

export interface OppgaveDTOny {
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
export type OppgaveKilde = 'HENDELSE' | 'BEHANDLING' | 'EKSTERN'
export type Oppgavetype =
  | 'FOERSTEGANGSBEHANDLING'
  | 'REVURDERING'
  | 'MANUELT_OPPHOER'
  | 'VURDER_KONSEKVENS'
  | 'ATTESTERING'
  | 'UNDERKJENT'
  | 'GOSYS'

export const erOppgaveRedigerbar = (status: Oppgavestatus): boolean => ['NY', 'UNDER_BEHANDLING'].includes(status)

export const hentNyeOppgaver = async (): Promise<ApiResponse<OppgaveDTOny[]>> => apiClient.get('/nyeoppgaver')
export const hentGosysOppgaver = async (): Promise<ApiResponse<OppgaveDTOny[]>> => apiClient.get('/nyeoppgaver/gosys')

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
    return apiClient.post(`/nyeoppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/nyeoppgaver/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const byttSaksbehandlerApi = async (args: {
  oppgaveId: string
  type: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/nyeoppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/nyeoppgaver/${args.oppgaveId}/bytt-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const fjernSaksbehandlerApi = async (args: {
  oppgaveId: string
  sakId: number
  type: string
  versjon: string | null
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/nyeoppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, {
      saksbehandler: '',
      versjon: args.versjon,
    })
  } else {
    return apiClient.delete(`/nyeoppgaver/${args.oppgaveId}/saksbehandler`)
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
    return apiClient.post(`/nyeoppgaver/gosys/${args.oppgaveId}/endre-frist`, { ...args.redigerFristRequest })
  } else {
    return apiClient.put(`/nyeoppgaver/${args.oppgaveId}/frist`, { ...args.redigerFristRequest })
  }
}

export const hentOppgaveForBehandlingUnderBehandling = async (args: {
  behandlingId: string
}): Promise<ApiResponse<string>> => apiClient.get(`/nyeoppgaver/${args.behandlingId}/hentsaksbehandler`)
