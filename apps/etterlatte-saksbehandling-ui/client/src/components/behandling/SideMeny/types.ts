import { IBehandlingStatus, IHendelse } from '../../../store/reducers/BehandlingReducer'

export interface IBehandlingInfo {
  type: string
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
