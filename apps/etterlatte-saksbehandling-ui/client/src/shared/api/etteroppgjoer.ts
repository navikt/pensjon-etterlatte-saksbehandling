import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IEtteroppgjoerBehandling } from '~shared/types/Etteroppgjoer'

export const hentEtteroppgjoer = async (behandlingId: string): Promise<ApiResponse<IEtteroppgjoerBehandling>> => {
  return apiClient.get(`/etteroppgjoer/${behandlingId}`)
}
