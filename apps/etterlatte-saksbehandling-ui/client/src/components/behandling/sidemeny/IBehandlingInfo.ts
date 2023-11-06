import { IBehandlingStatus, IBehandlingsType, UtenlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { IHendelse } from '~shared/types/IHendelse'
import { SakType } from '~shared/types/sak'

export interface IBehandlingInfo {
  type: IBehandlingsType
  behandlingId: string
  sakId: number
  sakType: SakType
  status: IBehandlingStatus
  saksbehandler?: string
  virkningsdato?: string
  datoFattet?: string
  datoAttestert?: string
  nasjonalEllerUtland: UtenlandstilknytningType
  underkjentLogg?: IHendelse[]
  fattetLogg?: IHendelse[]
  attestertLogg?: IHendelse[]
}

export enum BehandlingFane {
  DOKUMENTER = 'DOKUMENTER',
  SJEKKLISTE = 'SJEKKLISTE',
}
