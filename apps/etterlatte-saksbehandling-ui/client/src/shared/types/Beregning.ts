export interface Beregning {
  beregningId: string
  behandlingId: string
  type: Beregningstype
  beregningsperioder: Beregningsperiode[]
  beregnetDato: string
  grunnlagMetadata: GrunnlagMetadata
}

export interface GrunnlagMetadata {
  sakId: string
  versjon: number
}

export enum Beregningstype {
  BP = 'BP',
  OMS = 'OMS',
}

export interface Beregningsperiode {
  delytelsesId: string
  datoFOM: string
  datoTOM: string
  utbetaltBeloep: number
  soeskenFlokk: string[]
  grunnbelopMnd: number
  grunnbelop: number
  trygdetid: number
}

export interface BeregningsGrunnlag<K> {
  behandlingId: string
  kilde: K
  soeskenMedIBeregning: SoeskenMedIBeregning[]
  institusjonsopphold: Institusjonsopphold
}

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

export interface BeregningsGrunnlagData {
  soeskenMedIBeregning: SoeskenMedIBeregning[]
  institusjonsopphold: Institusjonsopphold
}

export interface Institusjonsopphold {
  institusjonsopphold: boolean
}
