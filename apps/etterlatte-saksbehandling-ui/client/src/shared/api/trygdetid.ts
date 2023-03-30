import { apiClient, ApiResponse } from '~shared/api/apiClient'

export const hentTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.get<ITrygdetid>(`/trygdetid/${behandlingsId}`)

export const opprettTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${behandlingsId}`, {})

export const lagreTrygdetidgrunnlag = async (args: {
  behandlingsId: string
  trygdetidgrunnlag: ITrygdetidGrunnlag
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/grunnlag`, { ...args.trygdetidgrunnlag })

export const lagreOppsummertTrygdetid = async (args: {
  behandlingsId: string
  beregnetTrygdetid: IBeregnetTrygdetid
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/beregnet`, { ...args.beregnetTrygdetid })

export interface ITrygdetid {
  id: string
  behandlingId: string
  beregnetTrygdetid?: IBeregnetTrygdetid
  trygdetidGrunnlag: ITrygdetidGrunnlag[]
}

export interface IBeregnetTrygdetid {
  nasjonal: number
  fremtidig: number
  total: number
}

export interface ITrygdetidGrunnlag {
  id?: string
  type: ITrygdetidGrunnlagType
  bosted: string
  periodeFra?: string
  periodeTil?: string
  trygdetid?: number
  kilde: string
}

export enum ITrygdetidGrunnlagType {
  NASJONAL = 'NASJONAL',
  FREMTIDIG = 'FREMTIDIG',
}
