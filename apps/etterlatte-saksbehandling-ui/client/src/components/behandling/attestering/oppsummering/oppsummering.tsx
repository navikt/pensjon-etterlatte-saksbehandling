import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Underkjent } from './underkjent'
import { Oversikt } from './oversikt'
import { IBeslutning } from '../types'
import { Innvilget } from './innvilget'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'

type Props = {
  beslutning: IBeslutning | undefined
  behandlingsInfo: IBehandlingInfo
  behandlendeSaksbehandler?: string
}

export const Behandlingsoppsummering = ({ behandlingsInfo, beslutning, behandlendeSaksbehandler }: Props) => {
  if (
    behandlingsInfo.status === IBehandlingStatus.ATTESTERT ||
    behandlingsInfo.status === IBehandlingStatus.TIL_SAMORDNING ||
    behandlingsInfo.status === IBehandlingStatus.SAMORDNET ||
    behandlingsInfo.status === IBehandlingStatus.AVSLAG ||
    beslutning === IBeslutning.godkjenn
  ) {
    return <Innvilget behandlingsInfo={behandlingsInfo} />
  }
  if (behandlingsInfo.status === IBehandlingStatus.RETURNERT || beslutning === IBeslutning.underkjenn) {
    return <Underkjent behandlingsInfo={behandlingsInfo} />
  }
  return <Oversikt behandlingsInfo={behandlingsInfo} behandlendeSaksbehandler={behandlendeSaksbehandler} />
}
