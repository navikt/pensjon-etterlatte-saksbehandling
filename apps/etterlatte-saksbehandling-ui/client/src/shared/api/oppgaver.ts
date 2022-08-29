import { apiClient, ApiResponse } from './apiClient'
import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

//ikke sikkert vi trenger denne etter mapping til oppgaver, lar stå enn så lenge
export const hentSaker = async (): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/saker`)
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
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
