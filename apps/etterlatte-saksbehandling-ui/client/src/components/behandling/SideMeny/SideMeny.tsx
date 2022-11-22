import { useEffect, useState } from 'react'
import { IBehandlingInfo } from './types'
import { BehandlingInfo } from '../attestering/Attestering'
import { IHendelseType } from '~store/reducers/BehandlingReducer'
import { useAppSelector } from '~store/Store'
import { Button } from '@navikt/ds-react'
import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'

export const SideMeny = () => {
  const saksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const [behandlingsInfo, setBehandlingsinfo] = useState<IBehandlingInfo>()
  const [collapsed, setCollapsed] = useState(false)

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
        virkningsdato: behandling.virkningstidspunkt?.dato,
        datoFattet: behandling.datoFattet,
        datoAttestert: behandling.datoAttestert,
        underkjentLogg: underkjentHendelser,
        fattetLogg: fattetHendelser,
        rolle: saksbehandler.rolle,
      })
  }, [saksbehandler, behandling])

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed(!collapsed)}>
          {/* Bruker &laquo; og &raquo; siden ds-icons ikke har dobbel chevron. */}
          {collapsed ? '«' : '»'}
        </Button>
      </SidebarTools>

      <SidebarContent collapsed={collapsed}>
        {behandlingsInfo && <BehandlingInfo behandlingsInfo={behandlingsInfo} />}
      </SidebarContent>
    </CollapsibleSidebar>
  )
}
