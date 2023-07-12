import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface OppgaveDTOny {
  id: string
  status: Status
  enhet: string
  sakId: number
  type: OppgaveType
  saksbehandler?: string
  referanse?: string
  merknad?: string
  opprettet: string
}

type Status = 'NY' | 'UNDER_BEHANDLING' | 'FERDIGSTILT'
type OppgaveType = 'FOERSTEGANGSBEHANDLING' | 'REVUDERING' | 'HENDELSE' | 'MANUELT_OPPHOER' | 'EKSTERN'

export interface OppgaveResponse {
  oppgaver: ReadonlyArray<OppgaveDTOny>
}
export const hentNyeOppgaver = async (): Promise<ApiResponse<OppgaveResponse>> =>
  apiClient.get<OppgaveResponse>('/nyeoppgaver/hent')
