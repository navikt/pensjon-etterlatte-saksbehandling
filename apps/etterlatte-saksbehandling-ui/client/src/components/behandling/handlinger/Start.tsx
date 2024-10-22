import { Button } from '@navikt/ds-react'
import { BehandlingRouteContext } from '../BehandlingRoutes'
import { handlinger } from './typer'
import { useContext } from 'react'

export const Start = ({ disabled }: { disabled?: boolean }) => {
  const { next } = useContext(BehandlingRouteContext)

  return (
    <Button variant="primary" onClick={next} disabled={disabled}>
      {handlinger.NESTE.navn}
    </Button>
  )
}
