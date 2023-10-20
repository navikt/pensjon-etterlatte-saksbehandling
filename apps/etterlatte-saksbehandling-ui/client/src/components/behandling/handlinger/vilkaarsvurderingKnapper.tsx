import { handlinger } from './typer'
import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useState } from 'react'
import { useVedtaksResultat } from '../useVedtaksResultat'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { fattVedtak, upsertVedtak } from '~shared/api/vedtaksvurdering'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/sendTilAttesteringModal'

export const VilkaarsVurderingKnapper = (props: { behandlingId: string; skalSendeBrev: boolean }) => {
  const { next, goto } = useBehandlingRoutes()
  const { behandlingId, skalSendeBrev } = props
  const [ventNavn, setVentNavn] = useState<string>(handlinger.VILKAARSVURDERING.VENT.navn)
  const [vedtak, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const vedtaksresultat = useVedtaksResultat()
  const [visAttesteringsmodal, setVisAttesteringsmodal] = useState(false)

  function toggleVentNavn() {
    const navn =
      ventNavn === handlinger.VILKAARSVURDERING.VENT.navn
        ? handlinger.VILKAARSVURDERING.GJENNOPPTA.navn
        : handlinger.VILKAARSVURDERING.VENT.navn
    setVentNavn(navn)
  }

  const oppdaterVedtak = () => {
    oppdaterVedtakRequest(behandlingId, () => {
      if (skalSendeBrev) {
        goto('brev')
      } else {
        setVisAttesteringsmodal(true)
      }
    })
  }

  return (
    <>
      {(() => {
        switch (vedtaksresultat) {
          case 'avslag':
            return (
              <>
                <Button variant="primary" loading={isPending(vedtak)} onClick={() => oppdaterVedtak()}>
                  {handlinger.VILKAARSVURDERING.AVSLAG.navn}
                </Button>
                {isFailure(vedtak) && (
                  <ApiErrorAlert>Kunne ikke opprette eller oppdatere vedtak, prøv igjen senere</ApiErrorAlert>
                )}
              </>
            )
          case 'opphoer':
            return (
              <>
                {visAttesteringsmodal ? (
                  <SendTilAttesteringModal behandlingId={behandlingId} fattVedtakApi={fattVedtak} />
                ) : (
                  <Button variant="primary" loading={isPending(vedtak)} onClick={() => oppdaterVedtak()}>
                    {skalSendeBrev
                      ? handlinger.VILKAARSVURDERING.OPPHOER.navn
                      : handlinger.VILKAARSVURDERING.OPPHOER_FATT_VEDTAK.navn}
                  </Button>
                )}
                {isFailure(vedtak) && (
                  <ApiErrorAlert>Kunne ikke opprette eller oppdatere vedtak, prøv igjen senere</ApiErrorAlert>
                )}
              </>
            )
          case 'innvilget':
          case 'endring':
            return (
              <Button variant="primary" onClick={next}>
                {handlinger.VILKAARSVURDERING.BEREGNE.navn}
              </Button>
            )
          case 'uavklart':
            return (
              //TODO - oppdatere sak med Status "satt på vent" og Handlinger "Gjenoppta saken" i oppgavebenken
              <Button variant="primary" onClick={toggleVentNavn}>
                {ventNavn}
              </Button>
            )
        }
      })()}
    </>
  )
}
