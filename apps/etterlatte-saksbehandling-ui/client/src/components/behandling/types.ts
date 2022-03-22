export interface IPersonFraSak {
  navn: string
  personStatus: PersonStatus
  rolle: RelatertPersonsRolle
  fnr: string
  adresser: IAdresse[]
  alderEtterlatt?: string
  statsborgerskap: string
  datoForDoedsfall: string
  fnrFraSoeknad: string
  adresseFraSoeknad?: string
}

export interface IAdresse {
  adresseLinje1: string | null
  adresseLinje2?: string | null
  adresseLinje3?: string | null
  aktiv: boolean
  coAdresseNavn?: string | null
  gyldigFraOgMed: string
  gyldigTilOgMed?: string
  kilde: string
  land?: string | null
  postnr: string
  poststed?: string | null
  type: string // adresseType
}

export interface IPersonOpplysningFraPdl {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string
  foedselsår: number
  doedsdato: string
  adressebeskyttelse: string
  bostedsadresse: IAdresse[]
  deltBostedsadresse: IAdresse[]
  kontaktadresse: IAdresse[]
  oppholdsadresse: IAdresse[]
  sivilstatus: string
  statsborgerskap: string
  utland: any
  familieRelasjon: IFamilieRelasjon
}

export interface IFamilieRelasjon {
  ansvarligeForeldre: string[]
  foreldre: string[]
  barn: string[]
}

export interface IAvdoedFraSoeknad {
  type: string
  fornavn: string
  etternavn: string
  foedselsnummer: string
  doedsdato: string
  statsborgerskap: string
  utenlandsopphold: any
  doedsaarsakSkyldesYrkesskadeEllerYrkessykdom: string
}
export interface IBarnFraSoeknad {
  type: string
  fornavn: string
  etternavn: string
  foedselsnummer: string
  statsborgerskap: string
  utenlandsadresse: any
  foreldre: IPersonOpplysning[]
  verge: any //D2
  omsorgsperson: string
}

export interface IGjenlevendeFraSoeknad {
  type: PersonStatus
  fornavn: string
  etternavn: string
  foedselsnummer: string
  adresse: string
  statsborgerskap: string
  telefonnummer: string
}

export interface IPersonOpplysning {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  type: PersonStatus
}

export enum RelatertPersonsRolle {
  BARN = 'barn',
  FORELDER = 'forelder',
}

export enum PersonStatus {
  AVDOED = 'Avdød',
  GJENLEVENDE_FORELDER = 'Gjenlevende',
  BARN = 'Etterlatt',
  ETTERLATT = 'Etterlatt',
}
