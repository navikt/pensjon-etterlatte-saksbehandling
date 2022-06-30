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
  const visGodkjent = behandlingsInfo.status === IBehandlingStatus.ATTESTERT || beslutning === IBeslutning.godkjenn
  const visUnderkjent = behandlingsInfo.status === IBehandlingStatus.RETURNERT || beslutning === IBeslutning.underkjenn
  const visOversikt = beslutning === undefined && visGodkjent === false && visUnderkjent === false

  return (
    <>
      {visOversikt && <Oversikt behandlingsInfo={behandlingsInfo} />}
      {visGodkjent && <Innvilget behandlingsInfo={behandlingsInfo} />}
      {visUnderkjent && <Underkjent behandlingsInfo={behandlingsInfo} />}
    </>
  )
}
