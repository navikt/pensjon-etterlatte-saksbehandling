import { apiClient, ApiResponse } from './apiClient'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { VedtaketKlagenGjelder } from '~shared/types/Klage'

export const hentVedtakSammendrag = async (behandlingsId: string): Promise<ApiResponse<VedtakSammendrag>> => {
  return apiClient.get(`vedtak/${behandlingsId}/sammendrag`)
}

export const hentIverksatteVedtakISak = async (sakId: number): Promise<ApiResponse<Array<VedtaketKlagenGjelder>>> => {
  return apiClient.get(`vedtak/sak/${sakId}/iverksatte`)
}

export const fattVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingsId}/fattvedtak`, {})
}

export const upsertVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/vedtak/${behandlingsId}/upsert`, {})
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
