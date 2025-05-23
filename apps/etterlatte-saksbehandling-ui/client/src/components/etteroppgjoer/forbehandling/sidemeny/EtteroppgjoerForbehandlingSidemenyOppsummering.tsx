import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { SidebarPanel } from '~shared/components/Sidebar'
import { EtteroppgjoerBehandlingStatus } from '~shared/types/EtteroppgjoerForbehandling'
import { Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import React from 'react'

export const EtteroppgjoerForbehandlingSidemenyOppsummering = () => {
  const etteroppgjoer = useEtteroppgjoer()

  const forbehandlingErFerdigstilt = etteroppgjoer.behandling.status === EtteroppgjoerBehandlingStatus.FERDIGSTILT

  return (
    <SidebarPanel
      $border
      style={
        forbehandlingErFerdigstilt
          ? {
              borderLeft: '5px solid var(--a-green-400)',
            }
          : {}
      }
    >
      <VStack gap="4">
        <VStack gap="2">
          <Heading size="small">Etteroppgjør forbehandling</Heading>
          {forbehandlingErFerdigstilt && (
            <Heading size="xsmall" style={{ color: 'var(--a-green-400)' }}>
              Ferdigstilt
            </Heading>
          )}
        </VStack>
        <HStack gap="4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={etteroppgjoer.behandling.sak.id.toString()} />
        </HStack>
      </VStack>
    </SidebarPanel>
  )
}
