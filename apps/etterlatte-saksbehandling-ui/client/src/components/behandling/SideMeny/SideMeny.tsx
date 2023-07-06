import { useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Button } from '@navikt/ds-react'
import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import { IHendelseType } from '~shared/types/IHendelse'
import { IRolle } from '~store/reducers/SaksbehandlerReducer'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Behandlingsoppsummering } from '~components/behandling/attestering/oppsummering/oppsummering'
import { Attestering } from '~components/behandling/attestering/attestering/attestering'
import { IBeslutning } from '~components/behandling/attestering/types'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'
import { Dokumentoversikt } from '~components/person/dokumentoversikt'
import styled from 'styled-components'
import AnnullerBehandling from '~components/behandling/handlinger/AnnullerBehanding'
import { SakType } from '~shared/types/sak'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'

export const SideMeny = (props: { behandling: IDetaljertBehandling; vedtak: VedtakSammendrag | undefined }) => {
  const { behandling, vedtak } = props

  const saksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [collapsed, setCollapsed] = useState(false)
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const behandlingsinfo: IBehandlingInfo = {
    type: behandling.behandlingType,
    sakId: behandling.sakId,
    sakType: behandling.sakType,
    status: behandling.status,
    saksbehandler: vedtak?.saksbehandlerId,
    attestant: vedtak?.attestant,
    virkningsdato: behandling.virkningstidspunkt?.dato,
    datoFattet: vedtak?.datoFattet,
    datoAttestert: vedtak?.datoAttestert,
    underkjentLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_UNDERKJENT),
    fattetLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_FATTET),
    attestertLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_ATTESTERT),
    rolle: saksbehandler.rolle,
  }

  const kanAttestere =
    behandlingsinfo.rolle === IRolle.attestant && behandlingsinfo.status === IBehandlingStatus.FATTET_VEDTAK

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed(!collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>

      <SidebarContent collapsed={collapsed}>
        {behandlingsinfo && (
          <>
            <Behandlingsoppsummering behandlingsInfo={behandlingsinfo} beslutning={beslutning} />
            {kanAttestere && (
              <Attestering
                setBeslutning={setBeslutning}
                beslutning={beslutning}
                behandling={behandling}
                vedtak={vedtak}
              />
            )}
          </>
        )}

        {behandlingsinfo && behandling.søker?.foedselsnummer && (
          <Dokumentoversikt fnr={behandling.søker.foedselsnummer} liten />
        )}

        {behandling.sakType == SakType.BARNEPENSJON && <AnnullerBehandling />}
      </SidebarContent>
    </CollapsibleSidebar>
  )
}

export const SidebarPanel = styled.div`
  margin: 20px 8px 0 8px;
  padding: 1em;
  border: 1px solid #c7c0c0;
  border-radius: 3px;

  .flex {
    display: flex;
    justify-content: space-between;
  }

  .info {
    margin-top: 1em;
    margin-bottom: 1em;
  }
`
