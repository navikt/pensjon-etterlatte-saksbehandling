import { Button } from '@navikt/ds-react'
import { BehandlingRouteContext } from '../BehandlingRoutes'
import { handlinger } from './typer'
import { useContext } from 'react'

export const Tilbake = () => {
  const { back, firstPage } = useContext(BehandlingRouteContext)

  return (
    <>
      {!firstPage && (
        <Button variant="secondary" onClick={back}>
          {handlinger.TILBAKE.navn}
        </Button>
      )}
    </>
  )
}
