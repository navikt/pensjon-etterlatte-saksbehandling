import { useEffect, useState } from 'react'
import { IBehandlingInfo } from './types'
import { BehandlingInfo } from '../attestering'
import { IHendelseType } from '../../../store/reducers/BehandlingReducer'
import { useAppSelector } from '../../../store/Store'

export const SideMeny = () => {
  const saksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const [behandlingsInfo, setBehandlingsinfo] = useState<IBehandlingInfo>()

  useEffect(() => {
    const underkjentHendelser = behandling.hendelser.filter(
      (hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_UNDERKJENT
    )
    const fattetHendelser = behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_FATTET)

    behandling &&
      setBehandlingsinfo({
        type: behandling.behandlingType,
        status: behandling.status,
        saksbehandler: behandling.saksbehandlerId,
        attestant: behandling.attestant,
        virkningsdato: behandling.virkningstidspunkt,
        datoFattet: behandling.datoFattet,
        datoAttestert: behandling.datoAttestert,
        underkjentLogg: underkjentHendelser,
        fattetLogg: fattetHendelser,
        rolle: saksbehandler.rolle,
      })
  }, [saksbehandler, behandling])

  return <div>{behandlingsInfo && <BehandlingInfo behandlingsInfo={behandlingsInfo} />}</div>
}
