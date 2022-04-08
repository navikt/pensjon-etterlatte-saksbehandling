import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { handlinger } from './typer'

export const Start = ({ soeknadGyldigFremsatt }: { soeknadGyldigFremsatt: boolean }) => {
  const { next } = useBehandlingRoutes()

  return (
    <Button variant="primary" size="medium" className="button" onClick={next} disabled={!soeknadGyldigFremsatt}>
      {handlinger.START.navn}
    </Button>
  )
}
