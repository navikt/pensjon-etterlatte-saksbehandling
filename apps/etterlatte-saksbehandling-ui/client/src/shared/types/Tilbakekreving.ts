import { ISak } from '~shared/types/sak'

export interface Tilbakekreving {
  id: string
  status: TilbakekrevingStatus
  sak: ISak
  opprettet: string
  vurdering: TilbakekrevingVurdering
  perioder: TilbakekrevingPeriode[]
}

export interface TilbakekrevingVurdering {
  beskrivelse: string | null
  konklusjon: string | null
  aarsak: TilbakekrevingAarsak | null
  aktsomhet: TilbakekrevingAktsomhet | null
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

export enum TilbakekrevingAarsak {
  ANNET = 'ANNET',
  ARBHOYINNT = 'ARBHOYINNT',
  BEREGNFEIL = 'BEREGNFEIL',
  DODSFALL = 'DODSFALL',
  EKTESKAP = 'EKTESKAP',
  FEILREGEL = ' FEILREGEL',
  FEILUFOREG = 'FEILUFOREG',
  FLYTTUTLAND = 'FLYTTUTLAND',
  IKKESJEKKYTELSE = 'IKKESJEKKYTELSE',
  OVERSETTMLD = 'OVERSETTMLD',
  SAMLIV = 'SAMLIV',
  UTBFEILMOT = 'UTBFEILMOT',
}

export enum TilbakekrevingAktsomhet {
  GOD_TRO = 'GOD_TRO',
  SIMPEL_UAKTSOMHET = 'SIMPEL_UAKTSOMHET',
  GROV_UAKTSOMHET = 'GROV_UAKTSOMHET',
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
