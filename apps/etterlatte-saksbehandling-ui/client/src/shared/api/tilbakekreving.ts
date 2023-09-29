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
