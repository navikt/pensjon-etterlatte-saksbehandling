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

export interface ForlengetOmsVurdering {
  behandlingId: string
  aktivitet: ForlengetOmsAktivitet
  begrunnelse: string
  utfall: ForlengetOmsUtfall
}

export interface LagretForlengetOmsVurdering {
  vurdering: ForlengetOmsVurdering
  vilkaarsvurdering: IVilkaarsvurdering
  periode: ForlengetOmsInfo | null
}

export const hentForlengetOmsVurdering = (behandlingId: string): Promise<ApiResponse<ForlengetOmsVurdering | null>> =>
  apiClient.get(`/behandling/${behandlingId}/forlenget-oms`)

export const lagreForlengetOmsVurdering = ({
  behandlingId,
  ...request
}: {
  behandlingId: string
  aktivitet: ForlengetOmsAktivitet
  begrunnelse: string
  utfall: ForlengetOmsUtfall
  periode?: ForlengetOmsInfo
}): Promise<ApiResponse<LagretForlengetOmsVurdering>> =>
  apiClient.post(`/behandling/${behandlingId}/forlenget-oms`, request)
