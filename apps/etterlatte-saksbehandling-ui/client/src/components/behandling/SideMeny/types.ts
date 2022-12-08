import { IBehandlingsType, IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IHendelse } from '~shared/types/IHendelse'

export interface IBehandlingInfo {
  type: IBehandlingsType
  status: IBehandlingStatus
  saksbehandler?: string
  attestant?: string
  virkningsdato?: string
  datoFattet?: string
  datoAttestert?: string
  underkjentLogg?: IHendelse[]
  fattetLogg?: IHendelse[]
  rolle: string
}
