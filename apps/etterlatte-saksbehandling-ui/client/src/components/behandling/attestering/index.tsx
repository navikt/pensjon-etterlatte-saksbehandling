import { IBeslutning } from './types'
import { useState } from 'react'
import { IRolle } from '../../../store/reducers/SaksbehandlerReducer'
import { IBehandlingStatus } from '../../../store/reducers/BehandlingReducer'
import { Attestering } from './attestering/attestering'
import { IBehandlingInfo } from '../SideMeny/types'
import { Behandlingsoppsummering } from './oppsummering/oppsummering'

export const BehandlingInfo = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  return (
    <>
      <Behandlingsoppsummering behandlingsInfo={behandlingsInfo} beslutning={beslutning} />

      {behandlingsInfo.rolle === IRolle.attestant && behandlingsInfo.status === IBehandlingStatus.FATTET_VEDTAK && (
        <Attestering setBeslutning={setBeslutning} beslutning={beslutning} />
      )}
    </>
  )
}
