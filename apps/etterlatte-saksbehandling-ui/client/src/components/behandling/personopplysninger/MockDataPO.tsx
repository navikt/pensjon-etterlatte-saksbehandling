import { IPersonFraSak, RelatertPersonsRolle, PersonStatus } from './types'

export const mockDataSoesken: IPersonFraSak[] = [
  {
    navn: 'Snorkefrøken',
    personStatus: PersonStatus.ETTERLATT,
    alderEtterlatt: '15',
    rolle: RelatertPersonsRolle.BARN,
    fnr: '13243546765',
    adressenavn: 'Fyrstikkalléen 1',
    statsborgerskap: {
      land: 'Norsk',
      gyldigFraOgMed: new Date(2005, 3, 12),
    },
  },
  {
    navn: 'Mummitrollet',
    personStatus: PersonStatus.ETTERLATT,
    alderEtterlatt: '15',
    rolle: RelatertPersonsRolle.BARN,
    fnr: '13243546765',
    adressenavn: 'Fyrstikkalléen 1',
    statsborgerskap: {
      land: 'Norsk',
      gyldigFraOgMed: new Date(2005, 3, 12),
    },
  },
]

export const mockDataPerson1: IPersonFraSak = {
  navn: 'Lille My',
  personStatus: PersonStatus.ETTERLATT,
  alderEtterlatt: '15',
  rolle: RelatertPersonsRolle.BARN,
  fnr: '13243546765',
  adressenavn: 'Fyrstikkalléen 1',
  statsborgerskap: {
    land: 'Norsk',
    gyldigFraOgMed: new Date(2005, 3, 12),
  },
}

export const mockDataPerson2: IPersonFraSak = {
  personStatus: PersonStatus.LEVENDE,
  rolle: RelatertPersonsRolle.FAR,
  fnr: '98765432123',
  adressenavn: 'Fyrstikkalléen 1',
  statsborgerskap: {
    land: 'Norsk',
    gyldigFraOgMed: new Date(2005, 3, 12),
  },
}

export const mockDataPerson3: IPersonFraSak = {
  personStatus: PersonStatus.DØD,
  datoForDoedsfall: new Date(2021, 5, 12),
  rolle: RelatertPersonsRolle.MOR,
  fnr: '12345678912',
  adressenavn: 'Fyrstikkalléen 1',
  alderEtterlatt: '15',
  statsborgerskap: {
    land: 'Norsk',
    gyldigFraOgMed: new Date(2005, 3, 12),
  },
}

export const mockDataPersoner: { person: IPersonFraSak; foreldre: IPersonFraSak[] } = {
  person: mockDataPerson1,
  foreldre: [mockDataPerson2, mockDataPerson3]}
