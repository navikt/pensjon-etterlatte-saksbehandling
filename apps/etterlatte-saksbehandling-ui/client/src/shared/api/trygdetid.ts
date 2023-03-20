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
  oppsummertTrygdetid: IOppsummertTrygdetid
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/oppsummert`, { ...args.oppsummertTrygdetid })

export interface ITrygdetid {
  oppsummertTrygdetid: IOppsummertTrygdetid | null
  grunnlag: ITrygdetidGrunnlag[]
}

export interface IOppsummertTrygdetid {
  nasjonalTrygdetid: number
  fremtidigTrygdetid: number
  totalt: number
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
  NASJONAL_TRYGDETID = 'NASJONAL_TRYGDETID',
  FREMTIDIG_TRYGDETID = 'FREMTIDIG_TRYGDETID',
  UTENLANDS_TRYGDETID = 'UTENLANDS_TRYGDETID',
}
