import { Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { hentBeregningsGrunnlag, opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlag,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import React, { useEffect, useState } from 'react'
import InstitusjonsoppholdBP from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdBP'
import Soeskenjustering, {
  Soeskengrunnlag,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import Spinner from '~shared/Spinner'
import { IPdlPerson } from '~shared/types/Person'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

const featureToggleNameInstitusjonsopphold = 'pensjon-etterlatte.bp-bruk-institusjonsopphold' as const

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const behandles = hentBehandlesFraStatus(behandling.status)
  const dispatch = useAppDispatch()
  const [lagreBeregningsgrunnlag, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlag)
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const visInstitusjonsopphold = useFeatureEnabledMedDefault(featureToggleNameInstitusjonsopphold, false)
  const [soeskenGrunnlagsData, setSoeskenGrunnlagsData] = useState<Soeskengrunnlag | null>(null)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] =
    useState<InstitusjonsoppholdGrunnlagData | null>(null)

  const [manglerSoeskenJustering, setSoeskenJusteringMangler] = useState<boolean>(false)

  useEffect(() => {
    fetchBeregningsgrunnlag(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlag({ ...result, institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag })
        )
      }
    })
  }, [])

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }
  const soesken: IPdlPerson[] =
    behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
      (barn) => barn.foedselsnummer !== behandling.søker?.foedselsnummer
    ) ?? []
  const harSoesken = soesken.length > 0

  const onSubmit = () => {
    if (harSoesken && !(soeskenGrunnlagsData || behandling.beregningsGrunnlag?.soeskenMedIBeregning)) {
      setSoeskenJusteringMangler(true)
    }
    if (behandling.beregningsGrunnlag?.soeskenMedIBeregning || soeskenGrunnlagsData || !harSoesken) {
      dispatch(resetBeregning())
      const beregningsgrunnlag = {
        soeskenMedIBeregning: soeskenGrunnlagsData
          ? mapListeTilDto(soeskenGrunnlagsData)
          : behandling.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
        institusjonsopphold: institusjonsoppholdsGrunnlagData
          ? mapListeTilDto(institusjonsoppholdsGrunnlagData)
          : behandling.beregningsGrunnlag?.institusjonsopphold ?? [],
      }

      postBeregningsgrunnlag(
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
  }

  return (
    <>
      <>
        {isSuccess(beregningsgrunnlag) && behandling.familieforhold && (
          <Soeskenjustering
            behandling={behandling}
            onSubmit={(soeskenGrunnlag) => setSoeskenGrunnlagsData(soeskenGrunnlag)}
            setSoeskenJusteringManglerIkke={() => setSoeskenJusteringMangler(false)}
          />
        )}
        {visInstitusjonsopphold && isSuccess(beregningsgrunnlag) && (
          <InstitusjonsoppholdBP
            behandling={behandling}
            onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}
          />
        )}
        <Spinner visible={isPending(beregningsgrunnlag)} label="Henter beregningsgrunnlag" />
        {isFailure(beregningsgrunnlag) && <ApiErrorAlert>Beregningsgrunnlag kan ikke hentes</ApiErrorAlert>}
      </>
      {manglerSoeskenJustering && <ApiErrorAlert>Søskenjustering er ikke fylt ut </ApiErrorAlert>}
      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(lagreBeregningsgrunnlag) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      <Border />

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            onClick={onSubmit}
            loading={isPending(lagreBeregningsgrunnlag) || isPending(endreBeregning)}
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
