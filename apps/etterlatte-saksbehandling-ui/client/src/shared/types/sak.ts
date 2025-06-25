import { IUtlandstilknytning } from '~shared/types/IDetaljertBehandling'

export interface ISak {
  id: number
  ident: string
  sakType: SakType
  enhet: string
  adressebeskyttelse?: AdressebeskyttelseGradering
  erSkjermet?: boolean
}

export interface ISakMedUtlandstilknytning {
  id: number
  ident: string
  sakType: SakType
  enhet: string
  utlandstilknytning?: IUtlandstilknytning
  adressebeskyttelse?: AdressebeskyttelseGradering
  erSkjermet?: boolean
}

export enum SakType {
  BARNEPENSJON = 'BARNEPENSJON',
  OMSTILLINGSSTOENAD = 'OMSTILLINGSSTOENAD',
}

export interface FoersteVirk {
  foersteIverksatteVirkISak: string
  sakId: number
}

export interface ISaksendring {
  id: string
  endringstype: Endringstype
  foer: ISak
  etter: ISak
  ident: string
  identtype: Identtype
  kommentar: string
  tidspunkt: string
}

export enum Identtype {
  SAKSBEHANDLER = 'SAKSBEHANDLER',
  GJENNY = 'GJENNY',
}

export enum Endringstype {
  OPPRETT_SAK = 'OPPRETT_SAK',
  ENDRE_IDENT = 'ENDRE_IDENT',
  ENDRE_ENHET = 'ENDRE_ENHET',
  ENDRE_ADRESSEBESKYTTELSE = 'ENDRE_ADRESSEBESKYTTELSE',
  ENDRE_SKJERMING = 'ENDRE_SKJERMING',
}

export const tekstEndringstype: Record<Endringstype, string> = {
  OPPRETT_SAK: 'Opprettet sak',
  ENDRE_IDENT: 'Nytt identnummer',
  ENDRE_ENHET: 'Bytte av enhet',
  ENDRE_ADRESSEBESKYTTELSE: 'Endret adressebeskyttelse',
  ENDRE_SKJERMING: 'Endret skjerming',
}

export enum AdressebeskyttelseGradering {
  STRENGT_FORTROLIG_UTLAND = 'STRENGT_FORTROLIG_UTLAND',
  STRENGT_FORTROLIG = 'STRENGT_FORTROLIG',
  FORTROLIG = 'FORTROLIG',
  UGRADERT = 'UGRADERT',
}

export const tekstAdressebeskyttelseGradering: Record<AdressebeskyttelseGradering, string> = {
  STRENGT_FORTROLIG_UTLAND: 'Strengt fortrolig (utland)',
  STRENGT_FORTROLIG: 'Strengt fortrolig',
  FORTROLIG: 'Fortrolig',
  UGRADERT: 'Ugradert',
}
