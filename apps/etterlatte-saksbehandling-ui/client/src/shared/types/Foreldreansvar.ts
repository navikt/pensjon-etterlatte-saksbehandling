export interface Foreldreansvar {
  ansvarligeForeldre: ForelderPeriode[]
  foreldre: string[]
}

export interface ForelderPeriode {
  fraDato?: string
  tilDato?: string
  forelder: string
}
