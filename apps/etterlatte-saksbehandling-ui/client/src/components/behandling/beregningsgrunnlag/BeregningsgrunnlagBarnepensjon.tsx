import { Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreSoeskenMedIBeregning } from '~shared/api/beregning'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlag,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import Trygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'
import Soeskenjustering, { Soeskengrunnlag } from '~components/behandling/beregningsgrunnlag/Soeskenjustering'

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const soeskenjustering = behandling.beregningsGrunnlag?.soeskenMedIBeregning
  const dispatch = useAppDispatch()
  const [lagreSoeskenMedIBeregningStatus, postSoeskenMedIBeregning] = useApiCall(lagreSoeskenMedIBeregning)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)

  const soeskenjusteringErDefinertIRedux = soeskenjustering !== undefined

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const onSubmit = (soeskengrunnlag: Soeskengrunnlag) => {
    dispatch(resetBeregning())
    postSoeskenMedIBeregning({ behandlingsId: behandling.id, soeskenMedIBeregning: soeskengrunnlag }, () =>
      postOpprettEllerEndreBeregning(behandling.id, () => {
        dispatch(
          oppdaterBeregingsGrunnlag({
            soeskenMedIBeregning: soeskengrunnlag,
          })
        )
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
        next()
      })
    )
  }
  return (
    <>
      <Trygdetid />
      <Soeskenjustering behandling={behandling} onSubmit={onSubmit} />

      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(lagreSoeskenMedIBeregningStatus) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          {soeskenjusteringErDefinertIRedux && (
            <Button
              variant="primary"
              size="medium"
              form="form"
              loading={isPending(lagreSoeskenMedIBeregningStatus) || isPending(endreBeregning)}
            >
              Beregne og fatte vedtak
            </Button>
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
  )
}

export default BeregningsgrunnlagBarnepensjon
