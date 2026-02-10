import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { genbehandlingTypeTilLesbartNavn } from '~components/person/sakOgBehandling/behandlingsslistemappere'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'
import { SidebarPanel } from '~shared/components/Sidebar'
import { Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'

export const AttestertVisning = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props

  return (
    <SidebarPanel $border style={{ borderLeft: '5px solid #007C2E' }}>
      <VStack gap="space-4">
        <Heading size="small">{genbehandlingTypeTilLesbartNavn(utlandsBehandling.type)}</Heading>

        <HStack gap="space-4" justify="space-between">
          <div>
            <Label size="small">Attestant</Label>
            <Detail>{utlandsBehandling.attestant?.attestant}</Detail>
          </div>
          <div>
            <Label size="small">Saksbehandler</Label>
            <Detail>{utlandsBehandling.behandler?.saksbehandler}</Detail>
          </div>
        </HStack>

        <HStack gap="space-4" justify="space-between">
          <div>
            <Label size="small">Attestert dato</Label>
            <Detail>
              {formaterKanskjeStringDatoMedFallback('Ikke registrert', utlandsBehandling.attestant?.tidspunkt)}
            </Detail>
          </div>
          <div>
            <Label size="small">Behandlet dato</Label>
            <Detail>
              {formaterKanskjeStringDatoMedFallback('Ikke registrert', utlandsBehandling.behandler?.tidspunkt)}
            </Detail>
          </div>
        </HStack>

        <HStack gap="space-4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={utlandsBehandling.sakId.toString()} />
        </HStack>
      </VStack>
    </SidebarPanel>
  )
}
