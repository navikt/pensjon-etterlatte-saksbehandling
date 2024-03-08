import { ISak } from '~shared/types/sak'

export interface TilbakekrevingBehandling {
  id: string
  status: TilbakekrevingStatus
  sak: ISak
  opprettet: string
  tilbakekreving: Tilbakekreving
}

export interface Tilbakekreving {
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
  FLYTTUTLAND = 'FLYTTUTLAND',
  IKKESJEKKYTELSE = 'IKKESJEKKYTELSE',
  OVERSETTMLD = 'OVERSETTMLD',
  SAMLIV = 'SAMLIV',
  UTBFEILMOT = 'UTBFEILMOT',
}

export const teksterTilbakekrevingAarsak: Record<TilbakekrevingAarsak, string> = {
  ANNET: 'Annet',
  ARBHOYINNT: 'Inntekt',
  BEREGNFEIL: 'Beregningsfeil',
  DODSFALL: 'Dødsfall',
  EKTESKAP: 'Eksteskap/Samboer med felles barn',
  FEILREGEL: 'Feil regelbruk',
  FLYTTUTLAND: 'Flyttet utland',
  IKKESJEKKYTELSE: 'Ikke sjekket mot andre ytelse',
  OVERSETTMLD: 'Oversett melding fra bruker',
  SAMLIV: 'Samliv',
  UTBFEILMOT: 'Utbetaling til feil mottaker',
} as const

export enum TilbakekrevingAktsomhet {
  GOD_TRO = 'GOD_TRO',
  SIMPEL_UAKTSOMHET = 'SIMPEL_UAKTSOMHET',
  GROV_UAKTSOMHET = 'GROV_UAKTSOMHET',
}

export const teksterTilbakekrevingAktsomhet: Record<TilbakekrevingAktsomhet, string> = {
  GOD_TRO: 'God tro',
  SIMPEL_UAKTSOMHET: 'Simpel uaktsomhet',
  GROV_UAKTSOMHET: 'Grov uaktsomhet',
}

export enum TilbakekrevingStatus {
  OPPRETTET = 'OPPRETTET',
  UNDER_ARBEID = 'UNDER_ARBEID',
  FATTET_VEDTAK = 'FATTET_VEDTAK',
  ATTESTERT = 'ATTESTERT',
  UNDERKJENT = 'UNDERKJENT',
}
export const teksterTilbakekrevingStatus: Record<TilbakekrevingStatus, string> = {
  OPPRETTET: 'Opprettet',
  UNDER_ARBEID: 'Under arbeid',
  FATTET_VEDTAK: 'Fattet vedtak',
  ATTESTERT: 'Attestert',
  UNDERKJENT: 'Underkjent',
}

export const erUnderBehandling = (status: TilbakekrevingStatus) =>
  status === TilbakekrevingStatus.OPPRETTET ||
  status === TilbakekrevingStatus.UNDER_ARBEID ||
  status === TilbakekrevingStatus.UNDERKJENT

export enum TilbakekrevingSkyld {
  BRUKER = 'BRUKER',
  IKKE_FORDELT = 'IKKE_FORDELT',
  NAV = 'NAV',
  SKYLDDELING = 'SKYLDDELING',
}

export const teksterTilbakekrevingSkyld: Record<TilbakekrevingSkyld, string> = {
  BRUKER: 'Bruker',
  IKKE_FORDELT: 'Ikke fordelt',
  NAV: 'Nav',
  SKYLDDELING: 'Skylddeling',
}

export enum TilbakekrevingResultat {
  DELVIS_TILBAKEKREV = 'DELVIS_TILBAKEKREV',
  FEILREGISTRERT = 'FEILREGISTRERT',
  FORELDET = 'FORELDET',
  FULL_TILBAKEKREV = 'FULL_TILBAKEKREV',
  INGEN_TILBAKEKREV = 'INGEN_TILBAKEKREV',
}

export const teksterTilbakekrevingResultat: Record<TilbakekrevingResultat, string> = {
  INGEN_TILBAKEKREV: 'Full tilbakekreving',
  DELVIS_TILBAKEKREV: 'Delvis tilbakekreving',
  FULL_TILBAKEKREV: 'Ingen tilbakekreving',
  FEILREGISTRERT: 'Feilregistrert',
  FORELDET: 'Foreldet',
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

export const teksterTilbakekrevingHjemmel: Record<TilbakekrevingHjemmel, string> = {
  ULOVFESTET: 'Lovfestet',
  TJUETO_FEMTEN_EN_LEDD_EN: '§ 22-15 1. ledd, 1. punktum',
  TJUETO_FEMTEN_EN_LEDD_TO_FORSETT: '§ 22-15 1. ledd, 2. punktum (forsett)',
  TJUETO_FEMTEN_EN_LEDD_TO_UAKTSOMT: '§ 22-15 1. ledd, 2. punktum (uaktsomt)',
  TJUETO_FEMTEN_FEM: '§ 22-15 5. ledd',
  TJUETO_FEMTEN_SEKS: '§ 22-15 6. ledd',
  TJUETO_SEKSTEN: '§ 22-16',
} as const
