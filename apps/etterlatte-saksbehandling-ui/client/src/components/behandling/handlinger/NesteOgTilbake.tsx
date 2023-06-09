import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import AvbrytBehandling from './AvbrytBehandling'
import { KnapperWrapper } from './BehandlingHandlingKnapper'
import { handlinger } from './typer'

export const NesteOgTilbake = () => {
  const { next, back, lastPage, firstPage } = useBehandlingRoutes()

  return (
    <KnapperWrapper>
      <div>
        {!firstPage && (
          <Button variant="secondary" size="medium" className="button" onClick={back}>
            {handlinger.TILBAKE.navn}
          </Button>
        )}
        {!lastPage && (
          <Button variant="primary" size="medium" className="button" onClick={next}>
            {handlinger.NESTE.navn}
          </Button>
        )}
      </div>
      <AvbrytBehandling />
    </KnapperWrapper>
  )
}
