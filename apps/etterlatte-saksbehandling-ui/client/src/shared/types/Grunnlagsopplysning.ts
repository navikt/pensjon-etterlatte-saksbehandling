export interface Grunnlagsopplysning<T, K> {
  id: string
  kilde: K
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
