import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IAvkorting, IAvkortingGrunnlagLagre, IAvkortingSkalHaInntektNesteAar } from '~shared/types/IAvkorting'
import { beregningApiUrl } from '~shared/api/beregning'

export const hentAvkorting = async (behandlingId: string): Promise<ApiResponse<IAvkorting>> => {
  return apiClient.get(beregningApiUrl(`/avkorting/${behandlingId}`))
}

export const avkortingSkalHaToInntekter = async (
  behandlingId: string
): Promise<ApiResponse<IAvkortingSkalHaInntektNesteAar>> => {
  return apiClient.get(beregningApiUrl(`/avkorting/${behandlingId}/skalHaInntektNesteAar`))
}

export const lagreAvkortingGrunnlag = async (args: {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlagLagre
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(beregningApiUrl(`/avkorting/${args.behandlingId}`), { ...args.avkortingGrunnlag })

export const lagreInntektListe = async (args: {
  behandlingId: string
  inntekter: IAvkortingGrunnlagLagre[]
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(beregningApiUrl(`/avkorting/${args.behandlingId}/liste`), { inntekter: args.inntekter })

export const hentManglendeInntektsaar = async (behandlingId: string): Promise<ApiResponse<number[]>> =>
  apiClient.get(beregningApiUrl(`/avkorting/${behandlingId}/manglende-inntektsaar`))
