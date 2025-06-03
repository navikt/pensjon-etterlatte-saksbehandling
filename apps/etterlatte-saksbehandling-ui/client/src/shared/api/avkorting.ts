import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IAvkorting, IAvkortingGrunnlagLagre, IAvkortingSkalHaInntektNesteAar } from '~shared/types/IAvkorting'

export const hentAvkorting = async (behandlingId: string): Promise<ApiResponse<IAvkorting>> => {
  return apiClient.get(`/beregning/avkorting/${behandlingId}`)
}

export const avkortingSkalHaToInntekter = async (
  behandlingId: string
): Promise<ApiResponse<IAvkortingSkalHaInntektNesteAar>> => {
  return apiClient.get(`/beregning/avkorting/${behandlingId}/skalHaInntektNesteAar`)
}

export const lagreAvkortingGrunnlag = async (args: {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlagLagre
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(`/beregning/avkorting/${args.behandlingId}`, { ...args.avkortingGrunnlag })

export const lagreInntektListe = async (args: {
  behandlingId: string
  inntekter: IAvkortingGrunnlagLagre[]
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(`/beregning/avkorting/${args.behandlingId}/liste`, { inntekter: args.inntekter })

export const hentManglendeInntektsaar = async (behandlingId: string): Promise<ApiResponse<number[]>> =>
  apiClient.get(`/beregning/avkorting/${behandlingId}/manglende-inntektsaar`)
