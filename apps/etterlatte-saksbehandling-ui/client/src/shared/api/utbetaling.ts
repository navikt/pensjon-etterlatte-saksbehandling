import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { SimulertBeregning } from '~shared/types/Utbetaling'

export const hentSimulertUtbetaling = async (behandlingId: string): Promise<ApiResponse<SimulertBeregning | null>> => {
  return apiClient.get(`/utbetaling/behandling/${behandlingId}/simulering`)
}

export const simulerUtbetaling = async (behandlingId: string): Promise<ApiResponse<SimulertBeregning>> => {
  return apiClient.post(`/utbetaling/behandling/${behandlingId}/simulering`, {})
}
