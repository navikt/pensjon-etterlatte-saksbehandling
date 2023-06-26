import { apiClient, ApiResponse } from './apiClient'
import { VedtakSammendrag } from '~components/vedtak/typer'

export const hentVedtakSammendrag = async (behandlingsId: string): Promise<ApiResponse<VedtakSammendrag>> => {
  return apiClient.get(`vedtak/${behandlingsId}/sammendrag`)
}
