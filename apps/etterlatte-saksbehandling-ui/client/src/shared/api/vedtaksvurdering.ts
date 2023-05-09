import { apiClient, ApiResponse } from './apiClient'

export const hentVedtakSammendrag = async (behandlingsId: string): Promise<ApiResponse<any>> => {
  return apiClient.get(`vedtak/${behandlingsId}/sammendrag`)
}
