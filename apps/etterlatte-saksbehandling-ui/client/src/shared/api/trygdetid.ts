import { apiClient, ApiResponse } from '~shared/api/apiClient'

export const hentTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.get<ITrygdetid>(`/trygdetid/${behandlingsId}`)

export const opprettTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${behandlingsId}`, {})

export const lagreTrygdetidgrunnlag = async (args: {
  behandlingsId: string
  trygdetidgrunnlag: OppdaterTrygdetidGrunnlag
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingsId}/grunnlag`, { ...args.trygdetidgrunnlag })

export const slettTrygdetidsgrunnlag = async (args: {
  behandlingsId: string
  trygdetidGrunnlagId: string
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.delete<ITrygdetid>(`/trygdetid/${args.behandlingsId}/grunnlag/${args.trygdetidGrunnlagId}`)

export const hentAlleLand = async (): Promise<ApiResponse<ILand[]>> =>
  apiClient.get<ILand[]>('/trygdetid/kodeverk/land')

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
  avdoedFylteSeksten: IOpplysningsgrunnlag
  avdoedFyllerSeksti: IOpplysningsgrunnlag
}

export type IOpplysningsgrunnlag = {
  opplysning: string
  kilde: {
    type: string
    tidspunkt: string
  }
}

export interface IBeregnetTrygdetid {
  total: number
}

export interface ITrygdetidGrunnlag {
  id: string
  type: ITrygdetidGrunnlagType
  bosted: string
  periodeFra: string
  periodeTil: string
  beregnet?: IBeregnetTrygdetidGrunnlag
  kilde: {
    tidspunkt: string
    ident: string
  }
  begrunnelse?: string
  poengInnAar?: boolean
  poengUtAar?: boolean
  prorata?: boolean
}

export interface OppdaterTrygdetidGrunnlag {
  id?: string
  type: ITrygdetidGrunnlagType
  bosted: string
  periodeFra?: string
  periodeTil?: string
  begrunnelse?: string
  poengInnAar?: boolean
  poengUtAar?: boolean
  prorata?: boolean
}

export interface IBeregnetTrygdetidGrunnlag {
  dager: number
  maaneder: number
  aar: number
}

export enum ITrygdetidGrunnlagType {
  FAKTISK = 'FAKTISK',
  FREMTIDIG = 'FREMTIDIG',
}

export interface ILand {
  gyldigFra: string
  gyldigTil: string
  isoLandkode: string
  beskrivelse: {
    term: string
    tekst: string
  }
}
