import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { handlinger } from './typer'

export const Start = ({ disabled }: { disabled?: boolean }) => {
  const { next } = useBehandlingRoutes()

  return (
    <Button variant="primary" onClick={next} disabled={disabled}>
      {handlinger.START.navn}
    </Button>
  )
}
