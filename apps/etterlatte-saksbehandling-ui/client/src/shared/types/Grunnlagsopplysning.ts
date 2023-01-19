export interface Grunnlagsopplysning<T> {
  id: string
  kilde: string
  opplysningsType: string
  opplysning: T
}

export interface Soeskenjusteringsgrunnlag {
  beregningsgrunnlag: SoeskenMedIBeregning[]
}

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}
