import { IOpplysningProps, OppholdstillatelseType, SivilstandType } from './types'

export const mockdata: IOpplysningProps = {
  sistEndringIFolkeregister: new Date(2015, 1, 6),
  bostedsadresse: [
    {
      adressenavn: 'Osloveien 12',
      postnummer: '0125 Oslo',
      gyldigFraOgMed: new Date(2015, 1, 6),
    },
    {
      adressenavn: 'Adresse-mock 2',
      postnummer: '0000 Oslo',
      gyldigFraOgMed: new Date(2010, 1, 6),
      gyldigTilOgMed: new Date(2015, 1, 5),
    },
  ],
  oppholdstillatelse: [
    {
      oppholdFra: new Date(2015, 1, 6),
      type: OppholdstillatelseType.PERMANENT,
    },
  ],
  statsborgerskap: [
    {
      land: 'Norsk',
      gyldigFraOgMed: new Date(1974, 3, 12),
    },
    {
      land: 'Sverige',
      gyldigFraOgMed: new Date(2020, 3, 12),
    },
  ],
  sivilstand: [
    {
      sivilstandType: SivilstandType.UGIFT,
      gyldigFraOgMed: new Date(2015, 5, 15),
    },
    {
      sivilstandType: SivilstandType.GIFT,
      gyldigFraOgMed: new Date(1995, 5, 15),
      gyldigTilOgMed: new Date(2015, 5, 15),
    },
  ],
}
