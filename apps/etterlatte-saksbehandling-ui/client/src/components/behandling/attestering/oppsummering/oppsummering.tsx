import { IBehandlingInfo } from '../../SideMeny/types'
import { IBehandlingStatus } from '../../../../store/reducers/BehandlingReducer'
import { Underkjent } from './underkjent'
import { Oversikt } from './oversikt'
import { IBeslutning } from '../types'
import { Innvilget } from './innvilget'

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
