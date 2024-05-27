import AvbrytBehandling from './AvbrytBehandling'
import { Tilbake } from './Tilbake'
import { ReactNode } from 'react'
import { HStack, VStack } from '@navikt/ds-react'

export const BehandlingHandlingKnapper = ({ children }: { children: ReactNode }) => {
  return (
    <VStack gap="4">
      <HStack gap="4" justify="center">
        <Tilbake />
        {children}
      </HStack>
      <HStack justify="center">
        <AvbrytBehandling />
      </HStack>
    </VStack>
  )
}
