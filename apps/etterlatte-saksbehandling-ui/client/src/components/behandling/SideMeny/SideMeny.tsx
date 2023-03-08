import { useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Button } from '@navikt/ds-react'
import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import { IHendelseType } from '~shared/types/IHendelse'
import { IRolle } from '~store/reducers/SaksbehandlerReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Behandlingsoppsummering } from '~components/behandling/attestering/oppsummering/oppsummering'
import { Attestering } from '~components/behandling/attestering/attestering/attestering'
import { IBeslutning } from '~components/behandling/attestering/types'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'

export const SideMeny = () => {
  const saksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const [collapsed, setCollapsed] = useState(false)
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const behandlingsinfo: IBehandlingInfo = {
    type: behandling.behandlingType,
    sakType: behandling.sakType,
    status: behandling.status,
    saksbehandler: behandling.saksbehandlerId,
    attestant: behandling.attestant,
    virkningsdato: behandling.virkningstidspunkt?.dato,
    datoFattet: behandling.datoFattet,
    datoAttestert: behandling.datoAttestert,
    underkjentLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_UNDERKJENT),
    fattetLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_FATTET),
    rolle: saksbehandler.rolle,
  }

  const kanAttestere =
    behandlingsinfo.rolle === IRolle.attestant && behandlingsinfo.status === IBehandlingStatus.FATTET_VEDTAK

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed(!collapsed)}>
          {/* Bruker &laquo; og &raquo; siden ds-icons ikke har dobbel chevron. */}
          {collapsed ? '«' : '»'}
        </Button>
      </SidebarTools>

      <SidebarContent collapsed={collapsed}>
        {behandlingsinfo && (
          <>
            <Behandlingsoppsummering behandlingsInfo={behandlingsinfo} beslutning={beslutning} />
            {kanAttestere && <Attestering setBeslutning={setBeslutning} beslutning={beslutning} />}
          </>
        )}
      </SidebarContent>
    </CollapsibleSidebar>
  )
}
