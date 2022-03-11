export interface IStatsborgerskap {
  land: string
  gyldigFraOgMed: Date
  gyldigTilOgMed?: Date
}

export interface IPersonFraSak {
  navn: string
  personStatus: PersonStatus
  rolle: RelatertPersonsRolle
  fnr: string
  adresser: IAdresse[]
  alderEtterlatt?: string
  statsborgerskap?: string
  datoForDoedsfall?: Date
  fnrFraSoeknad: string
  adresseFraSoeknad?: string
  adresseFraPdl: string
}

export interface IAdresse {
  adresseLinje1: string
  adresseLinje2?: string
  adresseLinje3?: string
  aktiv: boolean
  coAdresseNavn?: string
  gyldigFraOgMed: Date
  gyldigTilOgMed?: Date
  kilde: string
  land?: string
  postnr: string
  poststed?: string
  type: string // adresseType
}

export interface IPersonOpplysning {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  adresse: string
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
