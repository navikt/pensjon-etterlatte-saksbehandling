import { Button, Loader } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterBehandlingsstatus, resetBeregning } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { Border } from '~components/behandling/soeknadsoversikt/styled'

const BeregningsgrunnlagOmstillingsstoenad = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const [beregning, setOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const behandles = hentBehandlesFraStatus(behandling.status)

  const oppdaterBeregning = () => {
    dispatch(resetBeregning())
    setOpprettEllerEndreBeregning(behandling.id, () => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
      next()
    })
  }

  return (
    <>
      {isFailure(beregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}

      <Trygdetid redigerbar={behandles} />
      <Border />

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" onClick={oppdaterBeregning}>
            Beregne og fatte vedtak {isPending(beregning) && <Loader />}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
  )
}

export default BeregningsgrunnlagOmstillingsstoenad
