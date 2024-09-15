import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IAvkorting, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'

export const hentAvkorting = async (behandlingId: string): Promise<ApiResponse<IAvkorting>> => {
  return apiClient.get(`/beregning/avkorting/${behandlingId}`)
}

export const lagreAvkortingGrunnlag = async (args: {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlagLagre
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(`/beregning/avkorting/${args.behandlingId}`, { ...args.avkortingGrunnlag })
