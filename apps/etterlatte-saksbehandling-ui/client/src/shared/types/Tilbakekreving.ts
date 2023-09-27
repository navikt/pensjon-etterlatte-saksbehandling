import { ISak } from '~shared/types/sak'

export interface Tilbakekreving {
  id: string
  status: TilbakekrevingStatus
  sak: ISak
  opprettet: string
  perioder: TilbakekrevingPeriode[]
}

export interface TilbakekrevingPeriode {
  maaned: Date
  ytelsebeloeper: TilbakekrevingBeloep
}

export interface TilbakekrevingBeloep {
  bruttoUtbetaling: number
  nyBruttoUtbetaling: number
  skatteprosent: number
  beregnetFeilutbetaling: number | null
  bruttoTilbakekreving: number | null
  nettoTilbakekreving: number | null
  skatt: number | null
  skyld: TilbakekrevingSkyld | null
  resultat: TilbakekrevingResultat | null
  tilbakekrevingsprosent: number | null
  rentetillegg: number | null
}

export enum TilbakekrevingStatus {
  OPPRETTET = 'OPPRETTET',
  UNDER_ARBEID = 'UNDER_ARBEID',
}
export enum TilbakekrevingSkyld {
  BRUKER = 'BRUKER',
  IKKE_FORDELT = 'IKKE_FORDELT',
  NAV = 'NAV',
  SKYLDDELING = 'SKYLDDELING',
}
export enum TilbakekrevingResultat {
  DELVIS_TILBAKEKREV = 'DELVIS_TILBAKEKREV',
  FEILREGISTRERT = 'FEILREGISTRERT',
  FORELDET = 'FORELDET',
  FULL_TILBAKEKREV = 'FULL_TILBAKEKREV',
  INGEN_TILBAKEKREV = 'INGEN_TILBAKEKREV',
}
