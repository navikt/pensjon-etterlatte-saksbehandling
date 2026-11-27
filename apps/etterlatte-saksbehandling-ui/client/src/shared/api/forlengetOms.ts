import { apiClient, ApiResponse } from './apiClient'
import { IVilkaarsvurdering } from './vilkaarsvurdering'
import { ForlengetOmsInfo } from '~shared/types/RevurderingInfo'
import { JaNei } from '~shared/types/ISvar'

export type Tiltaksomfang = 'HELTID' | 'DELTID'
export type ForlengetOmsUtfall = 'INNVILGELSE' | 'AVSLAG'

export type ForlengetOmsAktivitet =
  | { type: 'UTDANNING'; skole: string; studium: string; studererMinst50Prosent: JaNei }
  | { type: 'ARBEIDSRETTET_TILTAK'; tiltak: string; omfang: Tiltaksomfang; aktivitetsplan: JaNei }
  | { type: 'NEI' }

export interface AktivitetOgUtfall {
  behandlingId: string
  aktivitet: ForlengetOmsAktivitet
  begrunnelse: string
  utfall: ForlengetOmsUtfall
}

export interface LagretForlengetOmsVurdering {
  vurdering: AktivitetOgUtfall
  vilkaarsvurdering: IVilkaarsvurdering
  periode: ForlengetOmsInfo | null
}

export interface LagreForlengetOmsVurdering {
  aktivitet: ForlengetOmsAktivitet
  begrunnelse: string
  utfall: ForlengetOmsUtfall
  periode?: ForlengetOmsInfo
}

export const hentAktivitetOgUtfall = (behandlingId: string): Promise<ApiResponse<AktivitetOgUtfall | null>> =>
  apiClient.get(`/behandling/${behandlingId}/forlenget-oms`)

export const lagreForlengetOmsVurdering = ({
  behandlingId,
  ...request
}: LagreForlengetOmsVurdering & { behandlingId: string }): Promise<ApiResponse<LagretForlengetOmsVurdering>> =>
  apiClient.post(`/behandling/${behandlingId}/forlenget-oms`, request)
