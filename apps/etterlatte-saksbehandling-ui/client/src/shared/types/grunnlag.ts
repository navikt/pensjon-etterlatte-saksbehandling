export interface Grunnlagsopplysning<T, K> {
  id: string
  kilde: K
  opplysningsType: string
  opplysning: T
}

export interface PersonMedNavn {
  fnr: string
  fornavn: string
  mellomnavn?: string
  etternavn: string
}
