import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { TilbakekrevingBehandling, TilbakekrevingPeriode, TilbakekrevingVurdering } from '~shared/types/Tilbakekreving'

export function hentTilbakekreving(tilbakekrevingId: string): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.get(`/tilbakekreving/${tilbakekrevingId}`)
}

export function lagreTilbakekrevingsvurdering(args: {
  behandlingsId: string
  vurdering: TilbakekrevingVurdering
}): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.put(`/tilbakekreving/${args.behandlingsId}/vurdering`, { ...args.vurdering })
}

export function lagreTilbakekrevingsperioder(args: {
  behandlingsId: string
  perioder: TilbakekrevingPeriode[]
}): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.put(`/tilbakekreving/${args.behandlingsId}/perioder`, { perioder: args.perioder })
}

export const opprettVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${behandlingsId}/vedtak/opprett`, {})
}

export const fattVedtak = async (behandlingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${behandlingsId}/vedtak/fatt`, {})
}

export const attesterVedtak = async (args: {
  behandlingsId: string
  kommentar: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${args.behandlingsId}/vedtak/attester`, { kommentar: args.kommentar })
}

export const underkjennVedtak = async (args: {
  behandlingsId: string
  kommentar: string
  valgtBegrunnelse: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${args.behandlingsId}/vedtak/underkjenn`, {
    kommentar: args.kommentar,
    valgtBegrunnelse: args.valgtBegrunnelse,
  })
}
