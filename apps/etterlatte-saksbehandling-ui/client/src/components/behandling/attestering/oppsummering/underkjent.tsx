import { formaterBehandlingstype } from '~utils/formatering/formatering'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import React from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { SidebarPanel } from '~shared/components/Sidebar'

export const Underkjent = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const { ident: innloggetId } = useInnloggetSaksbehandler()
  const oppgave = useSelectorOppgaveUnderBehandling()

  const underkjentSiste = behandlingsInfo.underkjentLogg?.slice(-1)[0]
  const fattetSiste = behandlingsInfo.fattetLogg?.slice(-1)[0]

  const erReturnert = behandlingsInfo.status === IBehandlingStatus.RETURNERT
  const saksbehandler = fattetSiste?.ident
  const attestant = erReturnert ? underkjentSiste?.ident : innloggetId

  return (
    <SidebarPanel $border style={{ borderLeft: '5px solid #881d0c' }}>
      <VStack gap="space-4">
        <div>
          <Heading size="small">{formaterBehandlingstype(behandlingsInfo.type)}</Heading>
          <Heading size="xsmall">Underkjent</Heading>

          {underkjentSiste && <Detail>{formaterDatoMedKlokkeslett(underkjentSiste.opprettet)}</Detail>}
        </div>

        <HStack gap="space-4" justify="space-between">
          <div>
            <Label size="small">Attestant</Label>
            <Detail>{attestant}</Detail>
          </div>
          <div>
            <Label size="small">Saksbehandler</Label>
            <Detail>{saksbehandler}</Detail>
          </div>
        </HStack>

        {erReturnert && underkjentSiste && (
          <div>
            <Label size="small">Årsak til retur</Label>
            <Detail>{underkjentSiste.valgtBegrunnelse}</Detail>
            <Detail>{underkjentSiste.kommentar}</Detail>
          </div>
        )}

        <HStack gap="space-4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
        </HStack>

        <SettPaaVent oppgave={oppgave} />
      </VStack>
    </SidebarPanel>
  )
}
