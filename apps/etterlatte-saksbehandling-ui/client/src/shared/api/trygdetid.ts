import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { JaNei } from '~shared/types/ISvar'

export const hentTrygdetid = async (behandlingId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.get<ITrygdetid>(`/trygdetid/${behandlingId}`)

export const opprettTrygdetid = async (behandlingId: string): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${behandlingId}`, {})

export const overstyrTrygdetid = async (overstyring: ITrygdetidOverstyring): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${overstyring.behandlingId}/overstyr`, { ...overstyring })

export const lagreYrkesskadeTrygdetidGrunnlag = async (args: {
  behandlingId: string
}): Promise<ApiResponse<ITrygdetid>> => apiClient.post(`/trygdetid/${args.behandlingId}/grunnlag/yrkesskade`, {})

export const lagreTrygdetidgrunnlag = async (args: {
  behandlingId: string
  trygdetidgrunnlag: OppdaterTrygdetidGrunnlag
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingId}/grunnlag`, { ...args.trygdetidgrunnlag })

export const slettTrygdetidsgrunnlag = async (args: {
  behandlingId: string
  trygdetidGrunnlagId: string
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.delete<ITrygdetid>(`/trygdetid/${args.behandlingId}/grunnlag/${args.trygdetidGrunnlagId}`)

export const hentAlleLand = async (): Promise<ApiResponse<ILand[]>> =>
  apiClient.get<ILand[]>('/trygdetid/kodeverk/land')

export const oppdaterStatus = async (behandlingId: string): Promise<ApiResponse<StatusOppdatert>> =>
  apiClient.post(`/trygdetid/${behandlingId}/oppdater-status`, {})

export interface StatusOppdatert {
  statusOppdatert: boolean
}

export const sorterLand = (landListe: ILand[]): ILand[] => {
  landListe.sort((a: ILand, b: ILand) => {
    if (a.beskrivelse.tekst > b.beskrivelse.tekst) {
      return 1
    }
    return -1
  })
  return landListe
}

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
  personKrets?: JaNei
  arbInntekt1G?: JaNei
  arbInntekt1GKommentar?: string
  beregArt50?: JaNei
  beregArt50Kommentar?: string
  nordiskTrygdeAvtale?: JaNei
  nordiskTrygdeAvtaleKommentar?: string
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
  arbInntekt1G?: JaNei
  arbInntekt1GKommentar?: string
  beregArt50?: JaNei
  beregArt50Kommentar?: string
  nordiskTrygdeAvtale?: JaNei
  nordiskTrygdeAvtaleKommentar?: string
}

export const hentAlleTrygdetidAvtaler = async (): Promise<ApiResponse<TrygdetidAvtale[]>> =>
  apiClient.get<TrygdetidAvtale[]>('/trygdetid/avtaler')

export const hentAlleTrygdetidAvtaleKriterier = async (): Promise<ApiResponse<TrygdetidAvtaleKriteria[]>> =>
  apiClient.get<TrygdetidAvtaleKriteria[]>('/trygdetid/avtaler/kriteria')

export const hentTrygdeavtaleForBehandling = async (args: {
  behandlingId: string
}): Promise<ApiResponse<Trygdeavtale>> => apiClient.get<Trygdeavtale>(`/trygdetid/avtaler/${args.behandlingId}`)

export const lagreTrygdeavtaleForBehandling = async (args: {
  behandlingId: string
  avtaleRequest: TrygdeavtaleRequest
}): Promise<ApiResponse<Trygdeavtale>> =>
  apiClient.post<Trygdeavtale>(`/trygdetid/avtaler/${args.behandlingId}`, { ...args.avtaleRequest })

export const opprettTrygdetidOverstyrtMigrering = async (args: { behandlingId: string }): Promise<ApiResponse<void>> =>
  apiClient.post(`/trygdetid/${args.behandlingId}/migrering/manuell/opprett`, {})

export const oppdaterTrygdetidOverstyrtMigrering = async (args: {
  behandlingId: string
  anvendtTrygdetid: number
  prorataBroek?: IProrataBroek
}): Promise<ApiResponse<ITrygdetid>> =>
  apiClient.post(`/trygdetid/${args.behandlingId}/migrering/manuell/lagre`, {
    samletTrygdetidNorge: args.prorataBroek ? undefined : args.anvendtTrygdetid,
    samletTrygdetidTeoretisk: args.prorataBroek ? args.anvendtTrygdetid : undefined,
    prorataBroek: args.prorataBroek,
    overstyrt: true,
  })

export interface ITrygdetid {
  id: string
  behandlingId: string
  beregnetTrygdetid?: IDetaljertBeregnetTrygdetid
  trygdetidGrunnlag: ITrygdetidGrunnlag[]
  opplysninger: IGrunnlagOpplysninger
  overstyrtNorskPoengaar: number | undefined
  opplysningerDifferanse: IOpplysningerDifferanse | undefined
}

export interface ITrygdetidOverstyring {
  id: string
  behandlingId: string
  overstyrtNorskPoengaar: number | undefined
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
  overstyrt: boolean
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

export interface IOpplysningerDifferanse {
  differanse: Boolean
  oppdaterteGrunnlagsopplysninger: IGrunnlagOpplysninger
}
