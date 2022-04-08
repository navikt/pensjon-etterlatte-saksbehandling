import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { handlinger } from './typer'

export const Tilbake = () => {
  const { back, firstPage } = useBehandlingRoutes()
 
  return (
    <>
      {!firstPage && (
        <Button variant="secondary" size="medium" className="button" onClick={back}>
          {handlinger.TILBAKE.navn}
        </Button>
      )}
    </>
  )
}

