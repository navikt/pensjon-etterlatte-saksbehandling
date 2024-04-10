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
