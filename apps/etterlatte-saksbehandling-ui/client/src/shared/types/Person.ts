import { Grunnlagsopplysning } from '~shared/types/Grunnlagsopplysning'
import { IAdresse } from '~shared/types/IAdresse'
import { KildePdl } from './kilde'

export interface IPerson {
  type: PersonType
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string
}

export interface IFamilieforhold {
  avdoede: Grunnlagsopplysning<IPdlPerson, KildePdl>
  gjenlevende: Grunnlagsopplysning<IPdlPerson, KildePdl>
}

export interface IFamilieRelasjon {
  ansvarligeForeldre?: string[]
  foreldre?: string[]
  barn?: string[]
}

export interface IPdlPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: Date
  doedsdato: string
  bostedsadresse?: IAdresse[]
  deltBostedsadresse?: IAdresse[]
  kontaktadresse?: IAdresse[]
  oppholdsadresse?: IAdresse[]
  avdoedesBarn?: IPdlPerson[]
  familieRelasjon?: IFamilieRelasjon
  sivilstatus?: Sivilstatus
  // ...
}

enum Sivilstatus {
  UOPPGITT = 'UOPPGITT',
  UGIFT = 'UGIFT',
  GIFT = 'GIFT',
  ENKE_ELLER_ENKEMANN = 'ENKE_ELLER_ENKEMANN',
  SKILT = 'SKILT',
  SEPARERT = 'SEPARERT',
  REGISTRERT_PARTNER = 'REGISTRERT_PARTNER',
  SEPARERT_PARTNER = 'SEPARERT_PARTNER',
  SKILT_PARTNER = 'SKILT_PARTNER',
  GJENLEVENDE_PARTNER = 'GJENLEVENDE_PARTNER',
}

export enum PersonType {
  INNSENDER = 'INNSENDER',
  GJENLEVENDE = ' GJENLEVENDE',
  GJENLEVENDE_FORELDER = 'GJENLEVENDE_FORELDER',
  AVDOED = 'AVDOED',
  SAMBOER = 'SAMBOER',
  VERGE = 'VERGE',
  BARN = 'BARN',
  FORELDER = 'FORELDER',
}

export enum PersonRolle {
  BARN = 'BARN',
  AVDOED = 'AVDOED',
  GJENLEVENDE = 'GJENLEVENDE',
}
