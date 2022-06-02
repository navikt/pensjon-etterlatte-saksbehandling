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
