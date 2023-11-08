import { handlinger } from '../handlinger/typer'
import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useVedtaksResultat } from '../useVedtaksResultat'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { upsertVedtak } from '~shared/api/vedtaksvurdering'
import { oppdaterStatus } from '~shared/api/vilkaarsvurdering'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'

export const VilkaarsvurderingKnapper = (props: { behandlingId: string }) => {
  const { next, goto } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const { behandlingId } = props
  const vedtaksresultat = useVedtaksResultat()
  const [vedtakResult, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [oppdaterStatusResult, oppdaterStatusRequest] = useApiCall(oppdaterStatus)

  const oppdaterVedtakAvslag = () => {
    oppdaterStatusRequest(behandlingId, (result) => {
      if (result.statusOppdatert) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      }
      oppdaterVedtakRequest(behandlingId, () => {
        goto('brev')
      })
    })
  }

  const sjekkGyldighetOgOppdaterStatus = () => {
    oppdaterStatusRequest(behandlingId, (result) => {
      if (result.statusOppdatert) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      }
      next()
    })
  }

  return (
    <>
      {isFailure(vedtakResult) && (
        <ApiErrorAlert>Kunne ikke opprette eller oppdatere vedtak, prøv igjen senere</ApiErrorAlert>
      )}
      {isFailure(oppdaterStatusResult) && <ApiErrorAlert>{oppdaterStatusResult.error.detail}</ApiErrorAlert>}
      <BehandlingHandlingKnapper>
        {(() => {
          switch (vedtaksresultat) {
            case 'innvilget':
            case 'endring':
            case 'opphoer':
              return (
                <Button
                  variant="primary"
                  loading={isPending(oppdaterStatusResult)}
                  onClick={sjekkGyldighetOgOppdaterStatus}
                >
                  {handlinger.NESTE.navn}
                </Button>
              )
            case 'avslag':
              return (
                <Button variant="primary" loading={isPending(vedtakResult)} onClick={() => oppdaterVedtakAvslag()}>
                  {handlinger.AVSLAG.navn}
                </Button>
              )
          }
        })()}
      </BehandlingHandlingKnapper>
    </>
  )
}
