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
  annenForelder?: AnnenForelder
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
  | 'ENDRET_SOEKER_FNR'
  | 'MANGLER_GJENLEVENDE'
  | 'MANGLER_AVDOED'
  | 'MANGLER_SOESKEN'
  | 'EKSTRA_GJENLEVENDE'
  | 'EKSTRA_AVDOED'
  | 'EKSTRA_SOESKEN'
  | 'HAR_PERSONER_UTEN_IDENTER'

export interface RedigertFamilieforhold {
  gjenlevende: string[]
  avdoede: string[]
}

export interface AnnenForelder {
  vurdering: AnnenForelderVurdering | null
  begrunnelse?: string | null
  navn?: string | null
  foedselsdato?: Date | null
}

export enum AnnenForelderVurdering {
  KUN_EN_REGISTRERT_JURIDISK_FORELDER = 'KUN_EN_REGISTRERT_JURIDISK_FORELDER',
  FORELDER_UTEN_IDENT_I_PDL = 'FORELDER_UTEN_IDENT_I_PDL',
}

export const teksterAnnenForelderVurdering: Record<AnnenForelderVurdering, string> = {
  KUN_EN_REGISTRERT_JURIDISK_FORELDER: 'Kun én registrert juridisk forelder',
  FORELDER_UTEN_IDENT_I_PDL: 'Forelder uten ident i PDL',
} as const
