import { Button, HStack, VStack } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import AvbrytBehandling from './AvbrytBehandling'
import { handlinger } from './typer'
import { useNavigate } from 'react-router-dom'

export const NesteOgTilbake = () => {
  const { next, back, lastPage, firstPage } = useBehandlingRoutes()
  const navigate = useNavigate()

  return (
    <VStack gap="4">
      <HStack gap="4" justify="center">
        <Button variant="secondary" onClick={() => (firstPage ? navigate(-1) : back())}>
          {handlinger.TILBAKE.navn}
        </Button>
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
