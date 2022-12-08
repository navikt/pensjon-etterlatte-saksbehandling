export interface Beregning {
  beregningId: string
  behandlingId: string
  beregningsperioder: Beregningsperiode[]
  beregnetDato: string
  grunnlagMetadata: GrunnlagMetadata
}

export interface GrunnlagMetadata {
  sakId: string
  versjon: number
}

export interface Beregningsperiode {
  delytelsesId: string
  type: string
  datoFOM: string
  datoTOM: string
  utbetaltBeloep: number
  soeskenFlokk: any[]
  grunnbelopMnd: number
  grunnbelop: number
}
