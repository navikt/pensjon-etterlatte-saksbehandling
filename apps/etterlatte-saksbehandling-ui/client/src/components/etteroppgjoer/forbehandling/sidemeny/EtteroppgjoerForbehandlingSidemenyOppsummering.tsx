import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { SidebarPanel } from '~shared/components/Sidebar'
import { EtteroppgjoerForbehandlingStatus } from '~shared/types/EtteroppgjoerForbehandling'
import { Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import React from 'react'

export const EtteroppgjoerForbehandlingSidemenyOppsummering = () => {
  const etteroppgjoer = useEtteroppgjoer()

  const forbehandlingErFerdigstilt = etteroppgjoer.behandling.status === EtteroppgjoerForbehandlingStatus.FERDIGSTILT
  const forbehandlingErAvbrutt = etteroppgjoer.behandling.status === EtteroppgjoerForbehandlingStatus.AVBRUTT

  return (
    <SidebarPanel $border>
      <VStack gap="4">
        <VStack gap="2">
          <Heading size="small">Etteroppgjør forbehandling</Heading>
          {forbehandlingErFerdigstilt && (
            <Heading size="xsmall" style={{ color: 'var(--a-green-400)' }}>
              Ferdigstilt
            </Heading>
          )}

          {forbehandlingErAvbrutt && (
            <Heading size="xsmall" style={{ color: 'var(--a-red-400)' }}>
              Avbrutt
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
