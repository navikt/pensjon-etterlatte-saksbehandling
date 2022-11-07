import { apiClient, ApiResponse } from './apiClient'

//ikke sikkert vi trenger denne etter mapping til oppgaver, lar stå enn så lenge
export const hentSaker = async (): Promise<ApiResponse<unknown>> => {
  return apiClient.get('/api/saker')
}

export interface OppgaveDTO {
  sakId: number
  behandlingsId: string
  regdato: Date
  fristdato: Date
  fnr: string
  soeknadType: string
  behandlingType: string
  beskrivelse: string
  oppgaveStatus: string
  saksbehandler: string
  handling: string
  antallSoesken: number | null
}
export interface OppgaveResponse {
  oppgaver: ReadonlyArray<OppgaveDTO>
}

export const hentOppgaver = async (): Promise<ApiResponse<OppgaveResponse>> =>
  apiClient.get<OppgaveResponse>('/oppgaver')
