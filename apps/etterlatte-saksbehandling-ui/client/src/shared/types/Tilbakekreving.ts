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
  aktsomhet: TilbakekrevingVurderingUaktsomhet
  hjemmel: TilbakekrevingHjemmel | null
}

export interface TilbakekrevingVurderingUaktsomhet {
  aktsomhet: TilbakekrevingAktsomhet | null
  reduseringAvKravet?: string
  strafferettsligVurdering?: string
  rentevurdering?: string
}

export interface TilbakekrevingPeriode {
  maaned: Date
  ytelse: TilbakekrevingBeloep
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
  FEILREGEL = 'FEILREGEL',
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
  FATTET_VEDTAK = 'FATTET_VEDTAK',
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

export enum TilbakekrevingHjemmel {
  ULOVFESTET = 'ULOVFESTET',
  TJUETO_FEMTEN_EN_LEDD_EN = 'TJUETO_FEMTEN_EN_LEDD_EN',
  TJUETO_FEMTEN_EN_LEDD_TO_FORSETT = 'TJUETO_FEMTEN_EN_LEDD_TO_FORSETT',
  TJUETO_FEMTEN_EN_LEDD_TO_UAKTSOMT = 'TJUETO_FEMTEN_EN_LEDD_TO_UAKTSOMT',
  TJUETO_FEMTEN_FEM = 'TJUETO_FEMTEN_FEM',
  TJUETO_FEMTEN_SEKS = 'TJUETO_FEMTEN_SEKS',
  TJUETO_SEKSTEN = 'TJUETO_SEKSTEN',
}
