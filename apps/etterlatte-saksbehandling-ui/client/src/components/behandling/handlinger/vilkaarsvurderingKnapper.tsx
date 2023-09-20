import { handlinger } from './typer'
import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useState } from 'react'
import { useVedtaksResultat } from '../useVedtaksResultat'

export const VilkaarsVurderingKnapper = () => {
  const { next, goto } = useBehandlingRoutes()
  const [ventNavn, setVentNavn] = useState<string>(handlinger.VILKAARSVURDERING.VENT.navn)
  const vedtaksresultat = useVedtaksResultat()

  function toggleVentNavn() {
    const navn =
      ventNavn === handlinger.VILKAARSVURDERING.VENT.navn
        ? handlinger.VILKAARSVURDERING.GJENNOPPTA.navn
        : handlinger.VILKAARSVURDERING.VENT.navn
    setVentNavn(navn)
  }

  return (
    <>
      {(() => {
        switch (vedtaksresultat) {
          case 'avslag':
            return (
              //TODO - Avslag
              <Button variant="primary" onClick={() => goto('brev')}>
                {handlinger.VILKAARSVURDERING.AVSLAG.navn}
              </Button>
            )
          case 'uavklart':
            return (
              //TODO - oppdatere sak med Status "satt på vent" og Handlinger "Gjenoppta saken" i oppgavebenken
              <Button variant="primary" onClick={toggleVentNavn}>
                {ventNavn}
              </Button>
            )
          case 'opphoer':
            return (
              <Button variant="primary" onClick={next}>
                {handlinger.VILKAARSVURDERING.OPPHOER.navn}
              </Button>
            )
          case 'innvilget':
          case 'endring':
            return (
              <Button variant="primary" onClick={next}>
                {handlinger.VILKAARSVURDERING.BEREGNE.navn}
              </Button>
            )
        }
      })()}
    </>
  )
}
