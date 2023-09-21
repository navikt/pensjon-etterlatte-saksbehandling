import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import AvbrytBehandling from './AvbrytBehandling'
import { handlinger } from './typer'
import { FlexRow } from '~shared/styled'

export const NesteOgTilbake = () => {
  const { next, back, lastPage, firstPage } = useBehandlingRoutes()

  return (
    <div>
      <FlexRow justify="center" $spacing>
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
      </FlexRow>
      <FlexRow justify="center">
        <AvbrytBehandling />
      </FlexRow>
    </div>
  )
}
