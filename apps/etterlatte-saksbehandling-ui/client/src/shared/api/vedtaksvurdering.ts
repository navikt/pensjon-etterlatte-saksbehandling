import { apiClient, ApiResponse } from './apiClient'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { VedtaketKlagenGjelder } from '~shared/types/Klage'

export const hentVedtakSammendrag = async (behandlingId: string): Promise<ApiResponse<VedtakSammendrag>> => {
  return apiClient.get(`vedtak/${behandlingId}/sammendrag`)
}

export const hentIverksatteVedtakISak = async (sakId: number): Promise<ApiResponse<Array<VedtaketKlagenGjelder>>> => {
  return apiClient.get(`vedtak/sak/${sakId}/iverksatte`)
}
;+2
export const fattVedtak = async (behandlingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingId}/fattvedtak`, {})
}

export const upsertVedtak = async (behandlingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingId}/upsert`, {})
}

export const attesterVedtak = async (args: {
  behandlingId: string
  kommentar: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${args.behandlingId}/attester`, { kommentar: args.kommentar })
}

export const underkjennVedtak = async ({
  behandlingId,
  kommentar,
  valgtBegrunnelse,
}: {
  behandlingId: string
  kommentar: string
  valgtBegrunnelse: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingId}/underkjenn`, { kommentar, valgtBegrunnelse })
}
