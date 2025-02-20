import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Etteroppgjoer } from '~shared/types/Etteroppgjoer'

export const hentEtteroppgjoer = async (behandlingId: string): Promise<ApiResponse<Etteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/${behandlingId}`)
}
