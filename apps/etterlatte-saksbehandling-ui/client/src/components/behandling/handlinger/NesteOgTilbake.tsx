import { Button, HStack, VStack } from '@navikt/ds-react'
import { BehandlingRouteContext } from '../BehandlingRoutes'
import AvbrytBehandling from './AvbrytBehandling'
import { handlinger } from './typer'
import { useContext } from 'react'

export const NesteOgTilbake = () => {
  const { next, back, lastPage, firstPage } = useContext(BehandlingRouteContext)

  return (
    <VStack gap="space-4">
      <HStack gap="space-4" justify="center">
        {!firstPage && (
          <Button variant="secondary" onClick={back}>
            {handlinger.TILBAKE.navn}
          </Button>
        )}
        {!lastPage && (
          <Button variant="primary" onClick={next}>
            {handlinger.NESTE.navn}
          </Button>
        )}
      </HStack>
      <HStack justify="center">
        <AvbrytBehandling />
      </HStack>
    </VStack>
  )
}
