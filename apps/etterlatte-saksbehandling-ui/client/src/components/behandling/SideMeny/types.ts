import { IBehandlingStatus } from '../../../store/reducers/BehandlingReducer'

export interface IBehandlingInfo {
  type: string
  status: IBehandlingStatus
  saksbehandler?: string
  attestant?: string
  virkningsdato?: string
  datoFattet?: string
  rolle: string
}
