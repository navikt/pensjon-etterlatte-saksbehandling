import { Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlag,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import FastTrygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'
import { Trygdetid as BeregnetTrygdetid } from '~components/behandling/trygdetid/Trygdetid'
import Soeskenjustering, { Soeskengrunnlag } from '~components/behandling/beregningsgrunnlag/Soeskenjustering'
import { Institusjonsopphold } from '~shared/types/Beregning'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import { useEffect, useState } from 'react'

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const dispatch = useAppDispatch()
  const [lagreSoeskenMedIBeregningStatus, postSoeskenMedIBeregning] = useApiCall(lagreBeregningsGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [funksjonsbrytere, postHentFunksjonsbrytere] = useApiCall(hentFunksjonsbrytere)
  const [beregnTrygdetid, setBeregnTrygdetid] = useState<boolean>(false)

  const featureToggleName = 'pensjon-etterlatte.bp-bruk-faktisk-trygdetid'

  useEffect(() => {
    postHentFunksjonsbrytere([featureToggleName], (brytere) => {
      const bryter = brytere.find((bryter) => bryter.toggle === featureToggleName)

      if (bryter) {
        setBeregnTrygdetid(bryter.enabled)
      }
    })
  }, [])

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const onSubmit = (soeskengrunnlag: Soeskengrunnlag) => {
    // TODO EY-2170
    const institusjonsopphold = { institusjonsopphold: false } as Institusjonsopphold
    dispatch(resetBeregning())
    const beregningsgrunnlag = {
      soeskenMedIBeregning: mapListeTilDto(soeskengrunnlag),
      institusjonsopphold: institusjonsopphold,
    }

    postSoeskenMedIBeregning(
      {
        behandlingsId: behandling.id,
        grunnlag: beregningsgrunnlag,
      },
      () =>
        postOpprettEllerEndreBeregning(behandling.id, () => {
          dispatch(oppdaterBeregingsGrunnlag(beregningsgrunnlag))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
          next()
        })
    )
  }
  return (
    <>
      {!isPending(funksjonsbrytere) && (beregnTrygdetid ? <BeregnetTrygdetid /> : <FastTrygdetid />)}
      <Soeskenjustering behandling={behandling} onSubmit={onSubmit} />

      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(lagreSoeskenMedIBeregningStatus) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            size="medium"
            form="form"
            loading={isPending(lagreSoeskenMedIBeregningStatus) || isPending(endreBeregning)}
          >
            Beregne og fatte vedtak
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
  )
}

export default BeregningsgrunnlagBarnepensjon
