import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { TilbakekrevingBehandling, TilbakekrevingPeriode, TilbakekrevingVurdering } from '~shared/types/Tilbakekreving'

export function hentTilbakekreving(tilbakekrevingId: string): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.get(`/tilbakekreving/${tilbakekrevingId}`)
}

export function hentTilbakekrevingerISak(sakId: number): Promise<ApiResponse<Array<TilbakekrevingBehandling>>> {
  return apiClient.get(`/tilbakekreving/sak/${sakId}`)
}

export function lagreTilbakekrevingsvurdering(args: {
  tilbakekrevingId: string
  vurdering: TilbakekrevingVurdering
}): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.put(`/tilbakekreving/${args.tilbakekrevingId}/vurdering`, { ...args.vurdering })
}

export function lagreSkalSendeBrev(args: {
  tilbakekrevingId: string
  skalSendeBrev: boolean
}): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.put(`/tilbakekreving/${args.tilbakekrevingId}/skal-sende-brev`, {
    skalSendeBrev: args.skalSendeBrev,
  })
}

export function lagreTilbakekrevingsperioder(args: {
  tilbakekrevingId: string
  perioder: TilbakekrevingPeriode[]
}): Promise<ApiResponse<TilbakekrevingBehandling>> {
  return apiClient.put(`/tilbakekreving/${args.tilbakekrevingId}/perioder`, { perioder: args.perioder })
}

export const validerTilbakekreving = async (
  tilbakekrevingId: string
): Promise<ApiResponse<TilbakekrevingBehandling>> => {
  return apiClient.post(`/tilbakekreving/${tilbakekrevingId}/valider`, {})
}

export const opprettVedtak = async (tilbakekrevingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${tilbakekrevingId}/vedtak/opprett`, {})
}

export const fattVedtak = async (tilbakekrevingId: string): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${tilbakekrevingId}/vedtak/fatt`, {})
}

export const attesterVedtak = async (args: {
  tilbakekrevingId: string
  kommentar: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/tilbakekreving/${args.tilbakekrevingId}/vedtak/attester`, { kommentar: args.kommentar })
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
