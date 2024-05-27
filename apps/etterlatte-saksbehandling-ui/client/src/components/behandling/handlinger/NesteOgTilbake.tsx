import { Button, HStack, VStack } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import AvbrytBehandling from './AvbrytBehandling'
import { handlinger } from './typer'

export const NesteOgTilbake = () => {
  const { next, back, lastPage, firstPage } = useBehandlingRoutes()

  return (
    <VStack gap="4">
      <HStack gap="4" justify="center">
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
