import { apiClient, ApiResponse } from './apiClient'

export interface OppgaveDTO {
  sakId: number
  behandlingId: string
  regdato: Date
  fristdato: Date
  fnr: string
  soeknadType: string
  oppgaveType: string
  oppgaveStatus: string
  saksbehandler: string
  handling: string
  merknad?: string
}
export interface OppgaveResponse {
  oppgaver: ReadonlyArray<OppgaveDTO>
}

export const hentOppgaver = async (): Promise<ApiResponse<OppgaveResponse>> =>
  apiClient.get<OppgaveResponse>('/oppgaver')
