import { apiClient, ApiResponse } from './apiClient'
import { KildeType } from '~store/reducers/BehandlingReducer'

export const hentVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.get<Vilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}`)

export const vurderVilkaar = async (
  behandlingId: string,
  request: VurderVilkaarRequest
): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/${behandlingId}`, {
    ...request,
    kommentar: request.kommentar,
  })

export const slettVurdering = async (behandlingId: string, type: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/${behandlingId}/${type}`)

export const slettTotalVurdering = async (behandlingId: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/resultat/${behandlingId}`)

export const lagreTotalVurdering = async (
  behandlingId: string,
  resultat: VilkaarsvurderingResultat,
  kommentar: string
): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/resultat/${behandlingId}`, {
    resultat: resultat,
    kommentar: kommentar,
  })

export interface Vilkaarsvurdering {
  vilkaar: Vilkaar[]
  resultat?: VilkaarsvurderingVurdertResultat
  virkningstidspunkt: string
}

export interface Vilkaar {
  hovedvilkaar: Hovedvilkaar
  unntaksvilkaar?: Unntaksvilkaar[]
  vurdering?: VurdertResultat
  grunnlag: Vilkaarsgrunnlag<any>[]
}

export interface Vilkaarsgrunnlag<T> {
  id: string
  opplysningsType: string
  kilde: Kilde
  opplysning: T
}

export type Kilde = KildeSaksbehandler | KildePdl

interface KildeSaksbehandler {
  type: KildeType.saksbehandler
  tidspunkt: string
  ident: string
}

interface KildePdl {
  type: KildeType.pdl
  tidspunktForInnhenting: string
  navn: string
  registersReferanse: string
  opplysningId: string
}

export interface Hovedvilkaar {
  type: string
  tittel: string
  beskrivelse: string
  lovreferanse: Paragraf
  resultat?: VurderingsResultat
}

export interface Unntaksvilkaar {
  type: string
  tittel: string
  beskrivelse?: string
  lovreferanse: Paragraf
  resultat?: VurderingsResultat
}

export interface VurdertResultat {
  kommentar?: string
  tidspunkt: Date
  saksbehandler: string
}

export interface Paragraf {
  paragraf: string
  lenke: string
}

export enum VurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  IKKE_VURDERT = 'IKKE_VURDERT',
}

export enum VilkaarsvurderingResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
}

export interface VurderVilkaarRequest {
  hovedvilkaar: {
    type: string
    resultat: VurderingsResultat
  }
  unntaksvilkaar?: {
    type: string
    resultat: VurderingsResultat
  }
  kommentar?: string
}

export interface VilkaarsvurderingVurdertResultat {
  utfall: VilkaarsvurderingResultat
  kommentar?: string
  tidspunkt: Date
  saksbehandler: string
}
