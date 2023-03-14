import { Kilde } from '~shared/types/kilde'
import { apiClient, ApiResponse } from './apiClient'

export const hentVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.get<IVilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}`)

export const opprettVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post<IVilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}/opprett`, {})

export const vurderVilkaar = async (args: {
  behandlingId: string
  request: VurderVilkaarRequest
}): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/${args.behandlingId}`, { ...args.request })

export const slettVurdering = async (args: {
  behandlingId: string
  type: string
}): Promise<ApiResponse<IVilkaarsvurdering>> => apiClient.delete(`/vilkaarsvurdering/${args.behandlingId}/${args.type}`)

export const slettTotalVurdering = async (behandlingId: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/resultat/${behandlingId}`)

export const lagreTotalVurdering = async (args: {
  behandlingId: string
  resultat: VilkaarsvurderingResultat
  kommentar: string
}): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/resultat/${args.behandlingId}`, {
    resultat: args.resultat,
    kommentar: args.kommentar,
  })

export interface IVilkaarsvurdering {
  vilkaar: Vilkaar[]
  resultat?: VilkaarsvurderingVurdertResultat
  virkningstidspunkt: string
}

export interface Vilkaar {
  id: string
  hovedvilkaar: Delvilkaar
  unntaksvilkaar: Delvilkaar[]
  vurdering?: VurdertResultat | null
  grunnlag: Vilkaarsgrunnlag<any>[]
}

export interface Vilkaarsgrunnlag<T> {
  id: string
  opplysningsType: string
  kilde: Kilde
  opplysning: T
}

export interface Delvilkaar {
  type: string
  tittel: string
  beskrivelse: string
  spoersmaal?: string
  lovreferanse: Paragraf
  resultat?: VurderingsResultat | null
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
  vilkaarId: string
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
