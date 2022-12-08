import { Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { Kilde } from '~shared/types/kilde'
import { apiClient, ApiResponse } from './apiClient'

export const hentVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.get<IVilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}`)

export const vurderVilkaar = async (
  behandlingId: string,
  request: VurderVilkaarRequest
): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/${behandlingId}`, {
    ...request,
    kommentar: request.kommentar,
  })

export const slettVurdering = async (behandlingId: string, type: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/${behandlingId}/${type}`)

export const slettTotalVurdering = async (behandlingId: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/resultat/${behandlingId}`)

export const lagreTotalVurdering = async (
  behandlingId: string,
  resultat: VilkaarsvurderingResultat,
  kommentar: string
): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/resultat/${behandlingId}`, {
    resultat: resultat,
    kommentar: kommentar,
  })

export interface IVilkaarsvurdering {
  vilkaar: Vilkaar[]
  resultat?: VilkaarsvurderingVurdertResultat
  virkningstidspunkt: Virkningstidspunkt
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
