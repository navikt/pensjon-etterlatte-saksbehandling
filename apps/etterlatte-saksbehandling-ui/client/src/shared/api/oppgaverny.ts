import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface OppgaveDTOny {
  id: string
  status: Status
  enhet: string
  sakId: number
  type: OppgaveType
  saksbehandler: string | null
  referanse: string | null
  merknad: string | null
  opprettet: string
  sakType: string | null
  fnr: string | null
  frist: string
}

type Status = 'NY' | 'UNDER_BEHANDLING' | 'FERDIGSTILT'
type OppgaveType = 'FOERSTEGANGSBEHANDLING' | 'REVUDERING' | 'HENDELSE' | 'MANUELT_OPPHOER' | 'EKSTERN'

export const hentNyeOppgaver = async (): Promise<ApiResponse<OppgaveDTOny[]>> => apiClient.get('/nyeoppgaver/hent')

export interface NySaksbehandlerDto {
  oppgaveId: string
  saksbehandler: string
}

export const tildelSaksbehandlerApi = async (nysaksbehandler: NySaksbehandlerDto): Promise<ApiResponse<void>> =>
  apiClient.post('/nyeoppgaver/tildel-saksbehandler', { ...nysaksbehandler })

export const fjernSaksbehandlerApi = async (oppgaveId: string): Promise<ApiResponse<void>> =>
  apiClient.post('/nyeoppgaver/fjern-saksbehandler', { oppgaveId })
