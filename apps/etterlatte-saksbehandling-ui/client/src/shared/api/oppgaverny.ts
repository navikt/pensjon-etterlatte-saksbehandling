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
  sakType: Saktype | null
  fnr: string | null
  frist: string
}

export type Saktype = 'BARNEPENSJON' | 'OMSTILLINGSSTOENAD'

export type Oppgavestatus = 'NY' | 'UNDER_BEHANDLING' | 'FERDIGSTILT' | 'FEILREGISTRERT'
export type Oppgavetype =
  | 'FOERSTEGANGSBEHANDLING'
  | 'REVURDERING'
  | 'HENDELSE'
  | 'MANUELT_OPPHOER'
  | 'EKSTERN'
  | 'ATTESTERING'

export const hentNyeOppgaver = async (): Promise<ApiResponse<OppgaveDTOny[]>> => apiClient.get('/nyeoppgaver/hent')

export interface NySaksbehandlerDto {
  oppgaveId: string
  saksbehandler: string
}

export const tildelSaksbehandlerApi = async (nysaksbehandler: NySaksbehandlerDto): Promise<ApiResponse<void>> =>
  apiClient.post('/nyeoppgaver/tildel-saksbehandler', { ...nysaksbehandler })

export const fjernSaksbehandlerApi = async (oppgaveId: string): Promise<ApiResponse<void>> =>
  apiClient.post('/nyeoppgaver/fjern-saksbehandler', { oppgaveId })

export interface RedigerFristRequest {
  oppgaveId: string
  frist: Date
}
export const redigerFristApi = async (redigerFristRequest: RedigerFristRequest): Promise<ApiResponse<void>> =>
  apiClient.put('/nyeoppgaver/rediger-frist', { ...redigerFristRequest })
