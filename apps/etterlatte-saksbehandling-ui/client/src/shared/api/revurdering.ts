import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { RevurderingInfo } from '~shared/types/RevurderingInfo'

export const lagreRevurderingInfo = ({
  behandlingId,
  begrunnelse,
  revurderingInfo,
}: {
  behandlingId: string
  begrunnelse?: string
  revurderingInfo: RevurderingInfo
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/revurdering/${behandlingId}/revurderinginfo`, {
    begrunnelse: begrunnelse ? begrunnelse : null,
    info: revurderingInfo,
  })
}
