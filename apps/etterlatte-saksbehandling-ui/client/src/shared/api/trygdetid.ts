import { apiClient, ApiResponse } from '~shared/api/apiClient'

export const hentTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.get<ITrygdetid>(`/trygdetid/${behandlingsId}`)

export const opprettTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${behandlingsId}`, {})

export const lagreTrygdetidgrunnlag = async (args: {
  behandlingsId: string
  nyttTrygdetidgrunnlag: ITrygdetidGrunnlag
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/grunnlag`, { ...args.nyttTrygdetidgrunnlag })

export const lagreOppsummertTrygdetid = async (args: {
  behandlingsId: string
  oppsummertTrygdetid: number
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/oppsummert`, { verdi: args.oppsummertTrygdetid })

export interface ITrygdetid {
  oppsummertTrygdetid: number | null
  grunnlag: ITrygdetidGrunnlag[]
}

export interface ITrygdetidGrunnlag {
  bosted: string
  periodeTil: string | null
  periodeFra: string | null
}
