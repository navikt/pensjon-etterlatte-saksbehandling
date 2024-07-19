import { Kilde } from '~shared/types/kilde'
import { apiClient, ApiResponse } from './apiClient'

export const hentVilkaarsvurdering = async (behandlingId: string): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.get<IVilkaarsvurdering>(`/vilkaarsvurdering/${behandlingId}`)

export const opprettVilkaarsvurdering = async (args: {
  behandlingId: string
  kopierVedRevurdering: boolean
}): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post<IVilkaarsvurdering>(
    `/vilkaarsvurdering/${args.behandlingId}/opprett?kopierVedRevurdering=${args.kopierVedRevurdering}`,
    {}
  )

export const slettVilkaarsvurdering = async (behandlingsId: string): Promise<ApiResponse<void>> =>
  apiClient.delete(`/vilkaarsvurdering/${behandlingsId}`)

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

export const oppdaterTotalVurdering = async (args: {
  behandlingId: string
  resultat: VilkaarsvurderingResultat
  kommentar: string
}): Promise<ApiResponse<IVilkaarsvurdering>> =>
  apiClient.post(`/vilkaarsvurdering/resultat/${args.behandlingId}`, {
    resultat: args.resultat,
    kommentar: args.kommentar,
  })

export const oppdaterStatus = async (behandlingId: string): Promise<ApiResponse<StatusOppdatert>> =>
  apiClient.post(`/vilkaarsvurdering/${behandlingId}/oppdater-status`, {})

export const hentMigrertYrkesskadeFordel = async (
  behandlingId: string
): Promise<ApiResponse<MigrertYrkesskadefordel>> =>
  apiClient.get(`/vilkaarsvurdering/${behandlingId}/migrert-yrkesskadefordel`)

export interface StatusOppdatert {
  statusOppdatert: boolean
}

export interface IVilkaarsvurdering {
  vilkaar: Vilkaar[]
  resultat?: VilkaarsvurderingVurdertResultat
  virkningstidspunkt: string
  isYrkesskade: boolean
  isGrunnlagUtdatert?: boolean | null
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
  opplysningsType: VilkaarsgrunnlagOpplysningstyper
  kilde: Kilde
  opplysning: T
}

export enum VilkaarsgrunnlagOpplysningstyper {
  SOEKER_FOEDSELSDATO = 'SOEKER_FOEDSELSDATO',
  AVDOED_DOEDSDATO = 'AVDOED_DOEDSDATO',
  VIRKNINGSTIDSPUNKT = 'VIRKNINGSTIDSPUNKT',
  SOEKNAD_MOTTATT_DATO = 'SOEKNAD_MOTTATT_DATO',
}

export interface Delvilkaar {
  type: string
  tittel: string
  beskrivelse: string
  spoersmaal?: string
  lovreferanse: Lovreferanse
  resultat?: VurderingsResultat | null
}

export interface VurdertResultat {
  kommentar?: string
  tidspunkt: Date
  saksbehandler: string
}

export interface Lovreferanse {
  paragraf: string
  ledd?: number
  bokstav?: string
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

export interface MigrertYrkesskadefordel {
  migrertYrkesskadefordel: boolean
}

export enum VilkaarType {
  Ingen = 'Ingen',
  BP_FORMAAL_2024 = 'BP_FORMAAL_2024',
  BP_DOEDSFALL_FORELDER_2024 = 'BP_DOEDSFALL_FORELDER_2024',
}
