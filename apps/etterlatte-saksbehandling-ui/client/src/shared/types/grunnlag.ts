import { IPdlPerson } from '~shared/types/Person'

export interface Grunnlagsopplysning<T, K> {
  id: string
  kilde: K
  opplysningsType: string //TODO: Typet??
  opplysning: T
}

export interface PersonMedNavn {
  fnr: string
  fornavn: string
  mellomnavn?: string
  etternavn: string
}

export interface Personopplysninger {
  innsender: Personopplysning
  soeker: Personopplysning
  avdoede: Personopplysning[]
  gjenlevende: Personopplysning[]
}

export interface Personopplysning {
  opplysningType: string
  id: string
  kilde: GenerellKilde
  opplysning: IPdlPerson
}

export interface GenerellKilde {
  type: string
  tidspunkt: string
  detalj?: string
}
