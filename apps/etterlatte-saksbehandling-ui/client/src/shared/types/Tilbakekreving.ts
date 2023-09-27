import { ISak } from '~shared/types/sak'

export interface Tilbakekreving {
  id: string
  status: TilbakekrevingStatus
  sak: ISak
  opprettet: string
  utbetalinger: Utbetalinger[]
}

export interface Utbetalinger {
  maaned: Date
  type: string
  bruttoUtbetaling: number
  nyBruttoUtbetaling: number
  skatteprosent: number
  skatt: number
  beregnetFeilutbetaling: number | null
  bruttoTilbakekreving: number | null
  nettoTilbakekreving: number | null
  skyld: string | null
  resultat: string | null
  tilbakekrevingsprosent: number | null
  rentetillegg: number | null
}

export enum TilbakekrevingStatus {
  OPPRETTET = 'OPPRETTET',
}
