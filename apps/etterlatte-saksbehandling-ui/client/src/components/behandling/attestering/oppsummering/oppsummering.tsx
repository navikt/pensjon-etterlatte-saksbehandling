import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Underkjent } from './underkjent'
import { Oversikt } from './oversikt'
import { IBeslutning } from '../types'
import { Innvilget } from './innvilget'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'

type Props = {
  beslutning: IBeslutning | undefined
  behandlingsInfo: IBehandlingInfo
}

export const Behandlingsoppsummering = ({ behandlingsInfo, beslutning }: Props) => {
  if (
    behandlingsInfo.status === IBehandlingStatus.ATTESTERT ||
    behandlingsInfo.status === IBehandlingStatus.TIL_SAMORDNING ||
    behandlingsInfo.status === IBehandlingStatus.SAMORDNET ||
    beslutning === IBeslutning.godkjenn
  ) {
    return <Innvilget behandlingsInfo={behandlingsInfo} />
  }
  if (behandlingsInfo.status === IBehandlingStatus.RETURNERT || beslutning === IBeslutning.underkjenn) {
    return <Underkjent behandlingsInfo={behandlingsInfo} />
  }
  return <Oversikt behandlingsInfo={behandlingsInfo} />
}
