import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Etteroppgjoer } from '~shared/types/Etteroppgjoer'
import { OppgaveDTO } from '~shared/types/oppgave'

interface EtteroppgjoerOgOppgave {
  etteroppgjoerBehandling: Etteroppgjoer
  oppgave: OppgaveDTO
}

export const hentEtteroppgjoer = async (behandlingId: string): Promise<ApiResponse<Etteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/${behandlingId}`)
}

export const opprettEtteroppgjoerIDev = async (sakId: number): Promise<ApiResponse<EtteroppgjoerOgOppgave>> => {
  return apiClient.post(`/etteroppgjoer/kundev/${sakId}`, {})
}
