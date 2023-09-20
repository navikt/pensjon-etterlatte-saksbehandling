import { apiClient, ApiResponse } from '~shared/api/apiClient'

export const hentTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.get<ITrygdetid>(`/trygdetid/${behandlingsId}`)

export const opprettTrygdetid = async (behandlingsId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${behandlingsId}`, {})

export const lagreYrkesskadeTrygdetidGrunnlag = async (args: {
  behandlingsId: string
}): Promise<ApiResponse<ITrygdetid>> => apiClient.post(`/trygdetid/${args.behandlingsId}/grunnlag/yrkesskade`, {})

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

export interface TrygdetidAvtaleOptions {
  kode: string
  beskrivelse: string
}

export interface TrygdetidAvtaleDato extends TrygdetidAvtaleOptions {
  fraDato: Date
}

export interface TrygdetidAvtale extends TrygdetidAvtaleOptions {
  kode: string
  beskrivelse: string
  fraDato: Date
  datoer: TrygdetidAvtaleDato[]
}

export interface TrygdetidAvtaleKriteria extends TrygdetidAvtaleOptions {
  kode: string
  beskrivelse: string
  fraDato: Date
}

export interface Trygdeavtale {
  id: string
  behandlingId: string
  avtaleKode: string
  avtaleDatoKode?: string
  avtaleKriteriaKode?: string
  kilde: {
    tidspunkt: string
    ident: string
  }
}

export interface TrygdeavtaleRequest {
  id?: string
  avtaleKode: string
  avtaleDatoKode?: string
  avtaleKriteriaKode?: string
}

export const hentAlleTrygdetidAvtaler = async (): Promise<ApiResponse<TrygdetidAvtale[]>> =>
  apiClient.get<TrygdetidAvtale[]>('/trygdetid/avtaler')

export const hentAlleTrygdetidAvtaleKriterier = async (): Promise<ApiResponse<TrygdetidAvtaleKriteria[]>> =>
  apiClient.get<TrygdetidAvtaleKriteria[]>('/trygdetid/avtaler/kriteria')

export const hentTrygdeavtaleForBehandling = async (args: {
  behandlingId: string
}): Promise<ApiResponse<Trygdeavtale>> => apiClient.get<Trygdeavtale>(`/trygdetid/avtaler/${args.behandlingId}`)

export const lagreTrygdeavtaleForBehandling = async (args: {
  behandlingsId: string
  avtaleRequest: TrygdeavtaleRequest
}): Promise<ApiResponse<Trygdeavtale>> =>
  apiClient.post<Trygdeavtale>(`/trygdetid/avtaler/${args.behandlingsId}`, { ...args.avtaleRequest })

export interface ITrygdetid {
  id: string
  behandlingId: string
  beregnetTrygdetid?: IDetaljertBeregnetTrygdetid
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

export interface IDetaljertBeregnetTrygdetid {
  resultat: IDetaljertBeregnetTrygdetidResultat
  tidspunkt: Date
}

export interface IDetaljertBeregnetTrygdetidResultat {
  faktiskTrygdetidNorge: IFaktiskTrygdetid
  faktiskTrygdetidTeoretisk: IFaktiskTrygdetid
  fremtidigTrygdetidNorge: IFremtidigTrygdetid
  fremtidigTrygdetidTeoretisk: IFremtidigTrygdetid
  samletTrygdetidNorge?: number
  samletTrygdetidTeoretisk?: number
  prorataBroek?: IProrataBroek
}

export interface IFaktiskTrygdetid {
  periode?: string
  antallMaaneder?: number
}

export interface IFremtidigTrygdetid {
  periode?: string
  antallMaaneder?: number
  opptjeningstidIMaaneder?: number
  mindreEnnFireFemtedelerAvOpptjeningstiden?: Boolean
  nordiskKonvensjon?: Boolean
}

export interface IProrataBroek {
  teller: number
  nevner: number
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
