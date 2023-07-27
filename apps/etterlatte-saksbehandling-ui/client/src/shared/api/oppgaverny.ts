import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface OppgaveDTOny {
  id: string
  status: Oppgavestatus
  enhet: string
  sakId: number
  type: Oppgavetype
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
export type Oppgavetype =
  | 'FOERSTEGANGSBEHANDLING'
  | 'REVURDERING'
  | 'HENDELSE'
  | 'MANUELT_OPPHOER'
  | 'EKSTERN'
  | 'ATTESTERING'

export const hentNyeOppgaver = async (): Promise<ApiResponse<OppgaveDTOny[]>> => apiClient.get('/nyeoppgaver/hent')

export interface SaksbehandlerEndringDto {
  oppgaveId: string
  saksbehandler: string
}

export const tildelSaksbehandlerApi = async (args: {
  nysaksbehandler: SaksbehandlerEndringDto
  sakId: number
}): Promise<ApiResponse<void>> =>
  apiClient.post(`/nyeoppgaver/tildel-saksbehandler/${args.sakId}`, { ...args.nysaksbehandler })

export const byttSaksbehandlerApi = async (args: {
  nysaksbehandler: SaksbehandlerEndringDto
  sakId: number
}): Promise<ApiResponse<void>> =>
  apiClient.post(`/nyeoppgaver/bytt-saksbehandler/${args.sakId}`, { ...args.nysaksbehandler })

export const fjernSaksbehandlerApi = async (args: { oppgaveId: string; sakId: number }): Promise<ApiResponse<void>> =>
  apiClient.post(`/nyeoppgaver/fjern-saksbehandler/${args.sakId}`, { oppgaveId: args.oppgaveId })

export interface RedigerFristRequest {
  oppgaveId: string
  frist: Date
}
export const redigerFristApi = async (args: {
  redigerFristRequest: RedigerFristRequest
  sakId: number
}): Promise<ApiResponse<void>> =>
  apiClient.put(`/nyeoppgaver/rediger-frist/${args.sakId}`, { ...args.redigerFristRequest })
