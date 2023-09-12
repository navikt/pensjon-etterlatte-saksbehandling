import { apiClient, ApiResponse } from './apiClient'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { VedtaketKlagenGjelder } from '~shared/types/Klage'

export const hentVedtakSammendrag = async (behandlingsId: string): Promise<ApiResponse<VedtakSammendrag>> => {
  return apiClient.get(`vedtak/${behandlingsId}/sammendrag`)
}

export const hentIverksatteVedtakISak = async (sakId: number): Promise<ApiResponse<Array<VedtaketKlagenGjelder>>> => {
  return apiClient.get(`vedtak/sak/${sakId}/iverksatte`)
}
