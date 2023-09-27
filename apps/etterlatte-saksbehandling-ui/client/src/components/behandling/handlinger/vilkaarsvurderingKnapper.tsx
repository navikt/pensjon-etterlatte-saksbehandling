import { handlinger } from './typer'
import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useState } from 'react'
import { useVedtaksResultat } from '../useVedtaksResultat'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { upsertVedtak } from '~shared/api/behandling'
import { useParams } from 'react-router-dom'
import { ApiErrorAlert } from '~ErrorBoundary'

export const VilkaarsVurderingKnapper = () => {
  const { next, goto } = useBehandlingRoutes()
  const [ventNavn, setVentNavn] = useState<string>(handlinger.VILKAARSVURDERING.VENT.navn)
  const [vedtak, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const { behandlingId } = useParams()

  const vedtaksresultat = useVedtaksResultat()

  function toggleVentNavn() {
    const navn =
      ventNavn === handlinger.VILKAARSVURDERING.VENT.navn
        ? handlinger.VILKAARSVURDERING.GJENNOPPTA.navn
        : handlinger.VILKAARSVURDERING.VENT.navn
    setVentNavn(navn)
  }

  const oppdaterVedtakAvslag = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    oppdaterVedtakRequest(behandlingId, () => {
      goto('brev')
    })
  }
  return (
    <>
      {(() => {
        switch (vedtaksresultat) {
          case 'avslag':
            return (
              <>
                <Button variant="primary" loading={isPending(vedtak)} onClick={() => oppdaterVedtakAvslag()}>
                  {handlinger.VILKAARSVURDERING.AVSLAG.navn}
                </Button>
                {isFailure(vedtak) && (
                  <ApiErrorAlert>Kunne ikke opprette eller oppdatere vedtak, prøv igjen senere</ApiErrorAlert>
                )}
              </>
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
