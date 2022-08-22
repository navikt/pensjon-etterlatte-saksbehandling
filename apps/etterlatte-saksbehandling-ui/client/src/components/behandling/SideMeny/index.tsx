import { useContext, useEffect, useState } from 'react'
import { IBehandlingInfo } from './types'
import { IBehandlingsType } from '../behandlingsType'
import { AppContext } from '../../../store/AppContext'
import { BehandlingInfo } from '../attestering'
import { IHendelseType } from '../../../store/reducers/BehandlingReducer'

export const SideMeny = () => {
  const ctx = useContext(AppContext)
  const [behandlingsInfo, setBehandlingsinfo] = useState<IBehandlingInfo>()

  useEffect(() => {
    const behandling = ctx.state.behandlingReducer
    const innlogget = ctx.state.saksbehandlerReducer
    const underkjentHendelser = behandling.hendelser.filter(
      (hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_UNDERKJENT
    )
    const fattetHendelser = behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_FATTET)

    behandling &&
      setBehandlingsinfo({
        type: IBehandlingsType.FØRSTEGANGSBEHANDLING,
        status: behandling.status,
        saksbehandler: behandling.saksbehandlerId,
        attestant: behandling.attestant,
        virkningsdato: behandling.virkningstidspunkt,
        datoFattet: behandling.datoFattet,
        datoAttestert: behandling.datoAttestert,
        underkjentLogg: underkjentHendelser,
        fattetLogg: fattetHendelser,
        rolle: innlogget.rolle,
      })
  }, [ctx.state])

  return <div>{behandlingsInfo && <BehandlingInfo behandlingsInfo={behandlingsInfo} />}</div>
}
