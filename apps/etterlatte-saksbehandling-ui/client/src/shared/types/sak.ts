import { IUtenlandstilknytning } from '~shared/types/IDetaljertBehandling'

export interface ISak {
  id: number
  ident: string
  sakType: SakType
  enhet: string
}

export interface ISakMedUtenlandstilknytning {
  id: number
  ident: string
  sakType: SakType
  enhet: string
  utenlandstilknytning?: IUtenlandstilknytning
}

export enum SakType {
  BARNEPENSJON = 'BARNEPENSJON',
  OMSTILLINGSSTOENAD = 'OMSTILLINGSSTOENAD',
}

export interface FoersteVirk {
  foersteIverksatteVirkISak: string
  sakId: number
}
