import { IBehandlingsType, IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IHendelse } from '~shared/types/IHendelse'
import { SakType } from '~shared/types/sak'

export interface IBehandlingInfo {
  type: IBehandlingsType
  behandlingId: string
  sakId: number
  sakType: SakType
  status: IBehandlingStatus
  saksbehandler?: string
  attestant?: string
  virkningsdato?: string
  datoFattet?: string
  datoAttestert?: string
  underkjentLogg?: IHendelse[]
  fattetLogg?: IHendelse[]
  attestertLogg?: IHendelse[]
  rolle: string
}
