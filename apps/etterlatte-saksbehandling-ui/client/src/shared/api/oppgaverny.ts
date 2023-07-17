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
  sakType?: string
  fnr?: string
  frist: string
}

type Status = 'NY' | 'UNDER_BEHANDLING' | 'FERDIGSTILT'
type OppgaveType = 'FOERSTEGANGSBEHANDLING' | 'REVUDERING' | 'HENDELSE' | 'MANUELT_OPPHOER' | 'EKSTERN'

export const hentNyeOppgaver = async (): Promise<ApiResponse<ReadonlyArray<OppgaveDTOny>>> =>
  apiClient.get('/nyeoppgaver/hent')

export interface NySaksbehandlerDto {
  oppgaveId: string
  saksbehandler: string
}

export const tildelSaksbehandlerApi = async (nysaksbehandler: NySaksbehandlerDto): Promise<ApiResponse<unknown>> =>
  apiClient.post('/nyeoppgaver/tildel-saksbehandler', { ...nysaksbehandler })
