import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IAvkorting, IAvkortingGrunnlagForm, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'

export const hentAvkorting = async (behandlingId: string): Promise<ApiResponse<IAvkorting>> => {
  return apiClient.get(`/beregning/avkorting/${behandlingId}`)
}

export const hentAvkortingGrunnlag = async (args: {
  behandlingId: string
  aar: number
}): Promise<ApiResponse<IAvkortingGrunnlagForm>> => {
  return apiClient.get(`/beregning/avkorting/${args.behandlingId}/aarsinntekt/${args.aar}`)
}

export const lagreAvkortingGrunnlag = async (args: {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlagLagre
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(`/beregning/avkorting/${args.behandlingId}`, { ...args.avkortingGrunnlag })
