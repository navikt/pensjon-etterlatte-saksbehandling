import { IPdlPerson, Persongalleri } from '~shared/types/Person'

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
  innsender?: Personopplysning
  soeker?: Personopplysning
  avdoede: Personopplysning[]
  gjenlevende: Personopplysning[]
}

export interface Personopplysning {
  opplysningType: string
  id: string
  kilde: GrunnlagKilde
  opplysning: IPdlPerson
}

export interface GrunnlagKilde {
  type: string
  tidspunkt: string
  detalj?: string
}

export interface PersongalleriSamsvar {
  persongalleri: Persongalleri
  kilde: GrunnlagKilde
  persongalleriPdl: Persongalleri | null
  kildePdl: GrunnlagKilde | null
  problemer: MismatchPersongalleri[]
}

export type MismatchPersongalleri =
  | 'MANGLER_GJENLEVENDE'
  | 'MANGLER_AVDOED'
  | 'MANGLER_SOESKEN'
  | 'EKSTRA_GJENLEVENDE'
  | 'EKSTRA_AVDOED'
  | 'EKSTRA_SOESKEN'
  | 'HAR_PERSONER_UTEN_IDENTER'

export interface RedigertFamilieforhold {
  gjenlevende: Array<{ fnr: string }>
  avdoede: Array<{ fnr: string }>
}
