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
  statsborgerskap: string
  datoForDoedsfall?: string
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

export interface IPersonOpplysning {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  type: PersonStatus
}

export interface IDodsfall {
  doedsdato: string
  foedselsnummer: string
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
