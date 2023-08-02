import { apiClient, ApiResponse } from '~shared/api/apiClient'

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
  sakType: Saktype
  fnr: string
  frist: string
}

export type Saktype = 'BARNEPENSJON' | 'OMSTILLINGSSTOENAD'

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

export interface SaksbehandlerEndringDto {
  saksbehandler: string
}

export const tildelSaksbehandlerApi = async (args: {
  oppgaveId: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<void>> =>
  apiClient.post(`/nyeoppgaver/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })

export const byttSaksbehandlerApi = async (args: {
  oppgaveId: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<void>> =>
  apiClient.post(`/nyeoppgaver/${args.oppgaveId}/bytt-saksbehandler`, { ...args.nysaksbehandler })

export const fjernSaksbehandlerApi = async (args: { oppgaveId: string; sakId: number }): Promise<ApiResponse<void>> =>
  apiClient.delete(`/nyeoppgaver/${args.oppgaveId}/saksbehandler`)

export interface RedigerFristRequest {
  frist: Date
}
export const redigerFristApi = async (args: {
  oppgaveId: string
  redigerFristRequest: RedigerFristRequest
}): Promise<ApiResponse<void>> => apiClient.put(`/nyeoppgaver/${args.oppgaveId}/frist`, { ...args.redigerFristRequest })
