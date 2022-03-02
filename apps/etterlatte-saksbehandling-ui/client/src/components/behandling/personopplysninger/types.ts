export interface IStatsborgerskap {
  land: string
  gyldigFraOgMed: Date
  gyldigTilOgMed?: Date
}

//Data som må hentes utenom saken fra register
export interface IPersonFraRegister {
  navn?: string
  personStatus: PersonStatus // for barn og foreldre
  rolle: RelatertPersonsRolle // for barn og foreldre
  datoForDoedsfall?: Date
  fnr: string // for barn og foreldre
  adressenavn: string // for barn og foreldre
}

export interface IPersonFraSak {
  navn?: string
  personStatus: PersonStatus
  rolle: RelatertPersonsRolle
  fnr: string
  adressenavn: string
  alderEtterlatt?: string
  statsborgerskap?: string
  datoForDoedsfall?: Date
}

export enum RelatertPersonsRolle {
  MOR = 'mor',
  FAR = 'far',
  MEDMOR = 'medmor',
  BARN = 'barn',
}

export enum PersonStatus {
  DØD = 'Avdød',
  LEVENDE = 'Gjenlevende',
  ETTERLATT = 'Etterlatt',
}
