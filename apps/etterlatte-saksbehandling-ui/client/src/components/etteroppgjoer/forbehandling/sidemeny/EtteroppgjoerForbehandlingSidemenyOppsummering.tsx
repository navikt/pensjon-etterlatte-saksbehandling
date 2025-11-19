import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { SidebarPanel } from '~shared/components/Sidebar'
import { EtteroppgjoerForbehandlingStatus } from '~shared/types/EtteroppgjoerForbehandling'
import { Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import React from 'react'

export const EtteroppgjoerForbehandlingSidemenyOppsummering = () => {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  const forbehandlingErFerdigstilt = forbehandling.status === EtteroppgjoerForbehandlingStatus.FERDIGSTILT
  const forbehandlingErAvbrutt = forbehandling.status === EtteroppgjoerForbehandlingStatus.AVBRUTT

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
          <KopierbarVerdi value={forbehandling.sak.id.toString()} />
        </HStack>
      </VStack>
    </SidebarPanel>
  )
}
