import { useContext, useEffect, useState } from 'react'
import { IBehandlingInfo } from './types'
import { IBehandlingsStatus } from '../behandlings-status'
import { AppContext } from '../../../store/AppContext'
import { BehandlingInfo } from '../attestering'

export const SideMeny = () => {
  const ctx = useContext(AppContext)
  const [behandlingsInfo, setBehandlingsinfo] = useState<IBehandlingInfo>()

  useEffect(() => {
    const behandling = ctx.state.behandlingReducer
    const innlogget = ctx.state.saksbehandlerReducer

    behandling &&
      setBehandlingsinfo({
        type: IBehandlingsStatus.FORSTEGANG,
        status: behandling.status,
        saksbehandler: behandling.saksbehandlerId,
        attestant: behandling.attestant,
        virkningsdato: behandling.virkningstidspunkt,
        datoFattet: behandling.datoFattet,
        datoAttestert: behandling.datoAttestert,
        rolle: innlogget.rolle,
      })
  }, [ctx.state])

  return <div>{behandlingsInfo && <BehandlingInfo behandlingsInfo={behandlingsInfo} />}</div>
}
