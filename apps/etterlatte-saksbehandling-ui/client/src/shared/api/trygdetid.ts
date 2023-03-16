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
  nasjonalTrygdetid: number | null
  fremtidigTrygdetid: number | null
  totalt: number | null
}

export interface ITrygdetidGrunnlag {
  type: ITrygdetidType
  bosted: string
  periodeTil: string | null
  periodeFra: string | null
}

export enum ITrygdetidType {
  NASJONAL_TRYGDETID = 'NASJONAL_TRYGDETID',
  FREMTIDIG_TRYGDETID = 'FREMTIDIG_TRYGDETID',
  UTENLANDS_TRYGDETID = 'UTENLANDS_TRYGDETID',
}
