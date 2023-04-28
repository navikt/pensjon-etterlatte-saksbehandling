export interface Grunnlagsopplysning<T, K> {
  id: string
  kilde: K
  opplysningsType: string
  opplysning: T
}

export interface PersonMedNavn {
  fnr: string
  fornavn: string
  etternavn: string
}

export interface Soeskenjusteringsgrunnlag {
  beregningsgrunnlag: SoeskenMedIBeregning[]
}

export interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}
