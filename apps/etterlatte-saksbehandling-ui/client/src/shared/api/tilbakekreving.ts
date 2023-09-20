import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'

export function hentTilbakekreving(tilbakekrevingId: string): Promise<ApiResponse<Tilbakekreving>> {
  return apiClient.get(`/tilbakekreving/${tilbakekrevingId}`)
}
