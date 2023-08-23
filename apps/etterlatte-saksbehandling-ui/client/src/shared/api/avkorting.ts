import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IAvkorting, IAvkortingGrunnlag } from '~shared/types/IAvkorting'

export const hentAvkorting = async (behandlingId: string): Promise<ApiResponse<IAvkorting>> => {
  return apiClient.get(`/beregning/avkorting/${behandlingId}`)
}

export const lagreAvkortingGrunnlag = async (args: {
  behandlingId: string
  avkortingGrunnlag: IAvkortingGrunnlag
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(`/beregning/avkorting/${args.behandlingId}`, { ...args.avkortingGrunnlag })

export const lagreManuellRestanse = async (args: {
  behandlingId: string
  avkortetYtelseId: string
  nyRestanse: number
}): Promise<ApiResponse<IAvkorting>> =>
  apiClient.post(`/beregning/avkorting/${args.behandlingId}/restanse`, {
    avkortetYtelseId: args.avkortetYtelseId,
    nyRestanse: args.nyRestanse,
  })
