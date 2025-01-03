import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Underkjent } from './underkjent'
import { Oversikt } from './oversikt'
import { IBeslutning } from '../types'
import { Innvilget } from './innvilget'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Avbrutt } from '~components/behandling/attestering/oppsummering/Avbrutt'

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
    behandlingsInfo.status === IBehandlingStatus.IVERKSATT ||
    beslutning === IBeslutning.godkjenn
  ) {
    return <Innvilget behandlingsInfo={behandlingsInfo} />
  }
  if (behandlingsInfo.status === IBehandlingStatus.RETURNERT || beslutning === IBeslutning.underkjenn) {
    return <Underkjent behandlingsInfo={behandlingsInfo} />
  }
  if (behandlingsInfo.status === IBehandlingStatus.AVBRUTT) {
    return <Avbrutt behandlingsInfo={behandlingsInfo} />
  }
  return <Oversikt behandlingsInfo={behandlingsInfo} behandlendeSaksbehandler={behandlendeSaksbehandler} />
}
