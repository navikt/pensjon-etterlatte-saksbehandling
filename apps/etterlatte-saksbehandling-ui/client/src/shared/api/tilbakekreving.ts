import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Tilbakekreving, TilbakekrevingPeriode } from '~shared/types/Tilbakekreving'

export function hentTilbakekreving(tilbakekrevingId: string): Promise<ApiResponse<Tilbakekreving>> {
  return apiClient.get(`/tilbakekreving/${tilbakekrevingId}`)
}

export function lagreTilbakekrevingsperioder(args: {
  tilbakekrevingsId: string
  perioder: TilbakekrevingPeriode[]
}): Promise<ApiResponse<Tilbakekreving>> {
  return apiClient.post(`/tilbakekreving/${args.tilbakekrevingsId}/lagre`, { perioder: args.perioder })
}
