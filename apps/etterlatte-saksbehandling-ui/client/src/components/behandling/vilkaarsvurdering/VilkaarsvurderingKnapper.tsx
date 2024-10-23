import { handlinger } from '../handlinger/typer'
import { Button } from '@navikt/ds-react'
import { behandlingHarVarselbrev, BehandlingRouteContext } from '../BehandlingRoutes'
import { useVedtaksResultat } from '../useVedtaksResultat'
import { useApiCall } from '~shared/hooks/useApiCall'
import { upsertVedtak } from '~shared/api/vedtaksvurdering'
import { oppdaterStatus } from '~shared/api/vilkaarsvurdering'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useBehandling } from '~components/behandling/useBehandling'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useContext } from 'react'

export const VilkaarsvurderingKnapper = (props: { behandlingId: string }) => {
  const { next, goto } = useContext(BehandlingRouteContext)
  const dispatch = useAppDispatch()
  const { behandlingId } = props
  const vedtaksresultat = useVedtaksResultat()
  const behandling = useBehandling()
  const personopplysninger = usePersonopplysninger()
  const [vedtakResult, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [oppdaterStatusResult, oppdaterStatusRequest] = useApiCall(oppdaterStatus)

  const boddEllerArbeidetUtlandet = behandling?.boddEllerArbeidetUtlandet?.boddEllerArbeidetUtlandet ?? false
  const ukjentAvdoed = personopplysninger?.avdoede.length === 0
  const skalBrukeTrygdetid = boddEllerArbeidetUtlandet && !ukjentAvdoed

  const oppdaterVedtakAvslag = () => {
    oppdaterStatusRequest(behandlingId, (result) => {
      if (result.statusOppdatert) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      }
      oppdaterVedtakRequest(behandlingId, () => {
        if (skalBrukeTrygdetid) {
          goto('trygdetid')
        } else {
          if (behandlingHarVarselbrev(behandling)) {
            next()
          } else {
            goto('brev')
          }
        }
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

  const genererNesteKnapp = () => {
    switch (vedtaksresultat) {
      case 'innvilget':
      case 'endring':
      case 'opphoer':
        return (
          <Button variant="primary" loading={isPending(oppdaterStatusResult)} onClick={sjekkGyldighetOgOppdaterStatus}>
            {handlinger.NESTE.navn}
          </Button>
        )
      case 'avslag':
        return (
          <Button variant="primary" loading={isPending(vedtakResult)} onClick={() => oppdaterVedtakAvslag()}>
            {skalBrukeTrygdetid
              ? handlinger.AVSLAG_UTLAND.navn
              : behandlingHarVarselbrev(behandling)
                ? handlinger.NESTE.navn
                : handlinger.AVSLAG.navn}
          </Button>
        )
    }
  }

  return (
    <>
      {isFailureHandler({
        apiResult: vedtakResult,
        errorMessage: 'Kunne ikke opprette eller oppdatere vedtak, prøv igjen senere',
      })}
      {isFailureHandler({
        apiResult: oppdaterStatusResult,
        errorMessage: 'Kunne ikke oppdatere status',
      })}
      <BehandlingHandlingKnapper>{genererNesteKnapp()}</BehandlingHandlingKnapper>
    </>
  )
}
