import { apiClient, ApiResponse } from './apiClient'

export const hentVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.get<Vilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}`)

export const vurderVilkaar = async (
  behandlingId: string,
  request: VurderVilkaarRequest
): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/${behandlingId}`, {
    type: request.type,
    resultat: request.resultat,
    kommentar: request.kommentar,
  })

export const slettVurdering = async (behandlingId: string, type: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/${behandlingId}/${type}`, true)

export const slettTotalVurdering = async (behandlingId: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.delete(`/vilkaarsvurdering/resultat/${behandlingId}`)

export const setTotalVurdering = async (
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
}

export interface Vilkaar {
  type: string
  paragraf: Paragraf
  vurdering?: VurdertResultat
}

export interface VurdertResultat {
  resultat: VurderingsResultat
  kommentar?: string
  tidspunkt: Date
  saksbehandler: string
}

export interface Paragraf {
  paragraf: string
  tittel: string
  lenke: string
  lovtekst: string
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
  type: string
  resultat: VurderingsResultat
  kommentar?: string
}

export interface VilkaarsvurderingVurdertResultat {
  utfall: VilkaarsvurderingResultat
  kommentar?: string
  tidspunkt: Date
  saksbehandler: string
}
