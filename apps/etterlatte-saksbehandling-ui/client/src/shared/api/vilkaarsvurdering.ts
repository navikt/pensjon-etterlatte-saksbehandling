import { apiClient, ApiResponse } from './apiClient'

export const hentVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<Vilkaarsvurdering>> =>
  apiClient.get<Vilkaarsvurdering>(`/vilkaarsvurdering/${behandlingsId}`)

export interface Vilkaarsvurdering {
  vilkaar: Vilkaar[]
}

export interface Vilkaar {
  type: VilkaarType
  paragraf: string
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
  ALDER_BARN,
  DOEDSFALL_FORELDER,
  AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
  BARNETS_MEDLEMSKAP,
}

export enum VurderingsResultat {
  OPPFYLT,
  IKKE_OPPFYLT,
}
