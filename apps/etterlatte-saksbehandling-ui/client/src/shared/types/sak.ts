import { IUtlandstilknytning } from '~shared/types/IDetaljertBehandling'

export interface ISak {
  id: number
  ident: string
  sakType: SakType
  enhet: string
}

export interface ISakMedUtlandstilknytning {
  id: number
  ident: string
  sakType: SakType
  enhet: string
  utlandstilknytning?: IUtlandstilknytning
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
}

export const tekstEndringstype: Record<Endringstype, string> = {
  OPPRETT_SAK: 'Opprettet sak',
  ENDRE_IDENT: 'Nytt identnummer',
  ENDRE_ENHET: 'Bytte av enhet',
}
