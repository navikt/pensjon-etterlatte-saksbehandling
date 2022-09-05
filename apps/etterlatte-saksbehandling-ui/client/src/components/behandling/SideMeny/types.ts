import { IBehandlingStatus, IBehandlingsType, IHendelse } from '../../../store/reducers/BehandlingReducer'

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
