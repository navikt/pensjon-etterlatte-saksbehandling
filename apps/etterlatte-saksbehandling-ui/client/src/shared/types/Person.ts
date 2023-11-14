import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { IAdresse } from '~shared/types/IAdresse'
import { KildePdl } from './kilde'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

export interface IFamilieforhold {
  avdoede: Grunnlagsopplysning<IPdlPerson, KildePdl>
  gjenlevende: Grunnlagsopplysning<IPdlPerson, KildePdl>
}

export const hentLevendeSoeskenFraAvdoedeForSoeker = (
  avdoede: Grunnlagsopplysning<IPdlPerson, KildePdl>,
  soekerFnr: string
) => {
  const soeskenliste = (avdoede?.opplysning.avdoedesBarn ?? []).filter((person) => person.foedselsnummer !== soekerFnr)
  return soeskenliste.filter((soesken) => soesken.doedsdato === null)
}

export interface IFamilieRelasjon {
  ansvarligeForeldre?: string[]
  foreldre?: string[]
  barn?: string[]
}

export interface IPdlPerson {
  fornavn: string
  mellomnavn?: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: Date
  doedsdato: string | undefined
  bostedsadresse?: IAdresse[]
  deltBostedsadresse?: IAdresse[]
  kontaktadresse?: IAdresse[]
  oppholdsadresse?: IAdresse[]
  avdoedesBarn?: IPdlPerson[]
  familieRelasjon?: IFamilieRelasjon
  sivilstatus?: Sivilstatus
  sivilstand?: Sivilstand[]
  vergemaalEllerFremtidsfullmakt?: VergemaalEllerFremtidsfullmakt[]
}

export interface Sivilstand {
  sivilstatus: Sivilstatus
  relatertVedSiviltilstand?: string
  gyldigFraOgMed?: Date
  bekreftetDato?: Date
  kilde: string
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

export function formaterNavn(person: IPdlPerson): string {
  return [person.fornavn, person.mellomnavn, person.etternavn].join(' ')
}

export interface Persongalleri {
  soeker?: string
  innsender?: string
  soesken?: string[]
  avdoed?: string[]
  gjenlevende?: string[]
}
