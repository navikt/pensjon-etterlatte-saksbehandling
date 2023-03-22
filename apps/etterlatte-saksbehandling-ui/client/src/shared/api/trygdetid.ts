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
  beregnetTrygdetid: IBeregnetTrygdetid
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/beregnet`, { ...args.beregnetTrygdetid })

export interface ITrygdetid {
  beregnetTrygdetid: IBeregnetTrygdetid | null
  trygdetidGrunnlag: ITrygdetidGrunnlag[]
}

export interface IBeregnetTrygdetid {
  nasjonal: number
  fremtidig: number
  total: number
}

export interface ITrygdetidGrunnlag {
  id: string | null
  type: ITrygdetidType
  bosted: string
  periodeFra: string | null
  periodeTil: string | null
  kilde: string
}

export enum ITrygdetidType {
  NASJONAL = 'NASJONAL',
  FREMTIDIG = 'FREMTIDIG',
  UTLAND = 'UTLAND',
}
