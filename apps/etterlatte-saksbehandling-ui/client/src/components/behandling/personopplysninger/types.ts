export interface IOpplysningProps {
  sistEndringIFolkeregister: Date
  bostedsadresse: Array<IBostedsadresse>
  oppholdstillatelse?: Array<IOppholdstillatelse>
  statsborgerskap: Array<IStatsborgerskap>
  sivilstand: Array<ISivilstand>
}

export interface IBostedsadresse {
  adressenavn: string
  postnummer: string
  gyldigFraOgMed: Date
  gyldigTilOgMed?: Date
}

export interface IOppholdstillatelse {
  oppholdFra: Date
  oppholdTil?: Date
  type: OppholdstillatelseType
}

export interface IStatsborgerskap {
  land: string
  gyldigFraOgMed: Date
  gyldigTilOgMed?: Date
}

export interface ISivilstand {
  sivilstandType: SivilstandType
  gyldigFraOgMed: Date
  gyldigTilOgMed?: Date
}

export enum OppholdstillatelseType {
  MIDLERTIDIG = 'Midlertidig',
  OPPLYSNING_MANGLER = 'Opplysning mangler',
  PERMANENT = 'Permanent',
}

export enum SivilstandType {
  UOPPGITT = 'Uoppgitt',
  UGIFT = 'Ugift',
  GIFT = 'Gift',
  ENKE_ELLER_ENKEMANN = 'Enke eller enkemann',
  SKILT = 'Skilt',
  SEPARERT = 'Separert',
  REGISTRERT_PARTNER = 'Registrert partner',
  SEPARERT_PARTNER = 'Separert partner',
  SKILT_PARTNER = 'Skilt partner',
  GJENLEVENDE_PARTNER = 'Gjenlevende partner',
}
