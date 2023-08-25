import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { RevurderingInfo } from '~shared/types/RevurderingInfo'

export const lagreRevurderingInfo = (args: {
  behandlingId: string
  begrunnelse: string
  revurderingInfo: RevurderingInfo
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/revurdering/${args.behandlingId}/revurderinginfo`, {
    begrunnelse: !!args.begrunnelse ? args.begrunnelse : null,
    info: args.revurderingInfo,
  })
}
