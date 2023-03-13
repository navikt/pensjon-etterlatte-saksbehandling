import { apiClient, ApiResponse } from '~shared/api/apiClient'

export const hentTrygdetid = async (): Promise<ApiResponse<ITrygdetid>> => apiClient.get<ITrygdetid>(`/trygdetid`)
export const lagreTrygdetidgrunnlag = async (
  nyttTrygdetidgrunnlag: ITrygdetidGrunnlag
): Promise<ApiResponse<ITrygdetidGrunnlag>> =>
  apiClient.post<ITrygdetidGrunnlag>(`/trygdetid/grunnlag`, { ...nyttTrygdetidgrunnlag })

export interface ITrygdetid {
  grunnlag: ITrygdetidGrunnlag[]
}

export interface ITrygdetidGrunnlag {
  bosted: string
  periodeTil: string
  periodeFra: string
}
