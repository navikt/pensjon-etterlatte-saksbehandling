import { Button, Loader } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

const BeregningsgrunnlagOmstillingsstoenad = () => {
  const [beregning, setOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling?.status)

  const oppdaterBeregning = () => {
    setOpprettEllerEndreBeregning(behandling.id, () => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
      next()
    })
  }

  return (
    <>
      {isFailure(beregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}

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
