import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Tilbakekreving, TilbakekrevingPeriode, TilbakekrevingVurdering } from '~shared/types/Tilbakekreving'

export function hentTilbakekreving(tilbakekrevingId: string): Promise<ApiResponse<Tilbakekreving>> {
  return apiClient.get(`/tilbakekreving/${tilbakekrevingId}`)
}

export function lagreTilbakekrevingsvurdering(args: {
  tilbakekrevingsId: string
  vurdering: TilbakekrevingVurdering
}): Promise<ApiResponse<Tilbakekreving>> {
  return apiClient.put(`/tilbakekreving/${args.tilbakekrevingsId}/vurdering`, { ...args.vurdering })
}

export function lagreTilbakekrevingsperioder(args: {
  tilbakekrevingsId: string
  perioder: TilbakekrevingPeriode[]
}): Promise<ApiResponse<Tilbakekreving>> {
  return apiClient.put(`/tilbakekreving/${args.tilbakekrevingsId}/perioder`, { perioder: args.perioder })
}

export const opprettVedtak = async (tilbakekrevingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${tilbakekrevingsId}/vedtak/opprett`, {})
}

export const fattVedtak = async (tilbakekrevingsId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${tilbakekrevingsId}/vedtak/fatt`, {})
}

export const attesterVedtak = async (args: {
  tilbakekrevingsId: string
  kommentar: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${args.tilbakekrevingsId}/vedtak/attester`, { kommentar: args.kommentar })
}

export const underkjennVedtak = async (args: {
  tilbakekrevingId: string
  kommentar: string
  valgtBegrunnelse: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${args.tilbakekrevingId}/vedtak/underkjenn`, {
    kommentar: args.kommentar,
    valgtBegrunnelse: args.valgtBegrunnelse,
  })
}
