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
  opplysninger: IGrunnlagOpplysninger
}

export interface IGrunnlagOpplysninger {
  avdoedDoedsdato: IOpplysningsgrunnlag
  avdoedFoedselsdato: IOpplysningsgrunnlag
}

export type IOpplysningsgrunnlag = {
  opplysning: string
  kilde: {
    type: string
    tidspunktForInnhenting: string
  }
}

export interface IBeregnetTrygdetid {
  total: number
}

export interface ITrygdetidGrunnlag {
  id?: string
  type: ITrygdetidGrunnlagType
  bosted: string
  periodeFra?: string
  periodeTil?: string
  beregnet?: IBeregnetTrygdetidGrunnlag
  kilde: string
}

export interface IBeregnetTrygdetidGrunnlag {
  dager: number
  maaneder: number
  aar: number
}

export enum ITrygdetidGrunnlagType {
  NASJONAL = 'NASJONAL',
  FREMTIDIG = 'FREMTIDIG',
}
