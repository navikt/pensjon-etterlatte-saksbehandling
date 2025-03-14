import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Etteroppgjoer, FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { OppgaveDTO } from '~shared/types/oppgave'

interface EtteroppgjoerOgOppgave {
  etteroppgjoerBehandling: Etteroppgjoer
  oppgave: OppgaveDTO
}

export const hentEtteroppgjoer = async (behandlingId: string): Promise<ApiResponse<Etteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/${behandlingId}`)
}

export const opprettEtteroppgjoerBrev = async (behandlingId: string): Promise<ApiResponse<Etteroppgjoer>> => {
  return apiClient.post(`/etteroppgjoer/${behandlingId}/opprett-brev`, {})
}

export const opprettEtteroppgjoerIDev = async (sakId: number): Promise<ApiResponse<EtteroppgjoerOgOppgave>> => {
  return apiClient.post(`/etteroppgjoer/kundev/${sakId}`, {})
}

export const lagreFaktiskInntekt = async (args: {
  forbehandlingId: string
  faktiskInntekt: FaktiskInntekt
}): Promise<ApiResponse<any>> => {
  return apiClient.post(`/etteroppgjoer/${args.forbehandlingId}/beregn_faktisk_inntekt`, {
    ...args.faktiskInntekt,
  })
}
