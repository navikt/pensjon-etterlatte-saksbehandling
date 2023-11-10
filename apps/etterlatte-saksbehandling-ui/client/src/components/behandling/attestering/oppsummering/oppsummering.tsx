import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Underkjent } from './underkjent'
import { Oversikt } from './oversikt'
import { IBeslutning } from '../types'
import { Innvilget } from './innvilget'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import styled from 'styled-components'

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
    return (
      <Innvilget behandlingsInfo={behandlingsInfo}>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </Innvilget>
    )
  }
  if (behandlingsInfo.status === IBehandlingStatus.RETURNERT || beslutning === IBeslutning.underkjenn) {
    return (
      <Underkjent behandlingsInfo={behandlingsInfo}>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </Underkjent>
    )
  }
  return (
    <Oversikt behandlingsInfo={behandlingsInfo}>
      <SakFlexbox>
        <Info>Sakid: </Info>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </SakFlexbox>
    </Oversikt>
  )
}

const SakFlexbox = styled.div`
  display: flex;
  flex-direction: row;
  margin-top: 1em;
`
const Info = styled.div`
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
`
