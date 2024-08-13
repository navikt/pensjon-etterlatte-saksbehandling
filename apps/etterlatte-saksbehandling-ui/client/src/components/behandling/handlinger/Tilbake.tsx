import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { handlinger } from './typer'
import { useNavigate } from 'react-router-dom'

export const Tilbake = () => {
  const { back, firstPage } = useBehandlingRoutes()
  const navigate = useNavigate()

  return (
    <Button variant="secondary" onClick={() => (firstPage ? navigate(-1) : back())}>
      {handlinger.TILBAKE.navn}
    </Button>
  )
}
