import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Underkjent } from './underkjent'
import { Oversikt } from './oversikt'
import { IBeslutning } from '../types'
import { Innvilget } from './innvilget'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'

type Props = {
  beslutning: IBeslutning | undefined
  behandlingsInfo: IBehandlingInfo
}

export const Behandlingsoppsummering: React.FC<Props> = ({ behandlingsInfo, beslutning }) => {
  if (behandlingsInfo.status === IBehandlingStatus.ATTESTERT || beslutning === IBeslutning.godkjenn) {
    return <Innvilget behandlingsInfo={behandlingsInfo} />
  }
  if (behandlingsInfo.status === IBehandlingStatus.RETURNERT || beslutning === IBeslutning.underkjenn) {
    return <Underkjent behandlingsInfo={behandlingsInfo} />
  }
  return <Oversikt behandlingsInfo={behandlingsInfo} />
}
