import { IBehandlingsType, IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IHendelse } from '~shared/types/IHendelse'
import { ISaksType } from '~components/behandling/fargetags/saksType'

export interface IBehandlingInfo {
  type: IBehandlingsType
  sakType: ISaksType
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
