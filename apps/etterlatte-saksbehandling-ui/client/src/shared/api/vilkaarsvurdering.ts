import { apiClient, ApiResponse } from './apiClient'

export const hentVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.get<Vilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}`)

export const vurderVilkaar = async (
  behandlingId: string,
  request: VurderVilkaarRequest
): Promise<ApiResponse<Vilkaarsvurdering>> => apiClient.post(`/vilkaarsvurdering/${behandlingId}/vurder`, { request })

export interface Vilkaarsvurdering {
  vilkaar: Vilkaar[]
}

export interface Vilkaar {
  type: VilkaarType
  paragraf: string
  paragrafTittel: string
  paragrafLenke: string
  lovtekst: string
  vurdering?: VurdertResultat
}

export interface VurdertResultat {
  resultat: VurderingsResultat
  kommentar?: string
  tidspunkt: Date
  saksbehandler: string
}

export enum VilkaarType {
  FORMAAL,
  FORUTGAAENDE_MEDLEMSKAP,
  ALDER_BARN,
  FORTSATT_MEDLEMSKAP,
  BARNETS_MEDLEMSKAP,
}

export enum VurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
}

export interface VurderVilkaarRequest {
  type: VilkaarType
  resultat: VurderingsResultat
  kommentar?: string
}
