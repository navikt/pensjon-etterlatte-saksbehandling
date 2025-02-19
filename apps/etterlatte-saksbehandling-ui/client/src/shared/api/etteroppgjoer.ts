import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IEtteroppgjoer } from '~shared/types/Etteroppgjoer'

export const hentEtteroppgjoer = async (behandlingId: string): Promise<ApiResponse<IEtteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/${behandlingId}`)
}
