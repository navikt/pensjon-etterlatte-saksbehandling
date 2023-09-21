import { Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import {
  hentBeregningsGrunnlagOMS,
  lagreBeregningsGrunnlagOMS,
  opprettEllerEndreBeregning,
} from '~shared/api/beregning'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlagOMS,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import React, { useEffect, useState } from 'react'
import InstitusjonsoppholdOMS from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdOMS'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import Spinner from '~shared/Spinner'

const BeregningsgrunnlagOmstillingsstoenad = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const behandles = hentBehandlesFraStatus(behandling.status)
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlagOMS)
  const [lagreBeregningsgrunnlagOMS, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlagOMS)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] =
    useState<InstitusjonsoppholdGrunnlagData | null>(null)

  useEffect(() => {
    fetchBeregningsgrunnlag(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlagOMS({
            ...result,
            institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag,
          })
        )
      }
    })
  }, [])

  const onSubmit = () => {
    dispatch(resetBeregning())
    const beregningsgrunnlagOMS = {
      institusjonsopphold: institusjonsoppholdsGrunnlagData
        ? mapListeTilDto(institusjonsoppholdsGrunnlagData)
        : behandling.beregningsGrunnlag?.institusjonsopphold ?? [],
    }

    postBeregningsgrunnlag(
      {
        behandlingsId: behandling.id,
        grunnlag: beregningsgrunnlagOMS,
      },
      () =>
        postOpprettEllerEndreBeregning(behandling.id, () => {
          dispatch(oppdaterBeregingsGrunnlagOMS(beregningsgrunnlagOMS))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
          next()
        })
    )
  }

  return (
    <>
      <>
        {isSuccess(beregningsgrunnlag) && (
          <InstitusjonsoppholdOMS
            behandling={behandling}
            onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}
          />
        )}
        <Spinner visible={isPending(beregningsgrunnlag)} label="Henter beregningsgrunnlag" />
        {isFailure(beregningsgrunnlag) && <ApiErrorAlert>Beregningsgrunnlag kan ikke hentes</ApiErrorAlert>}
      </>
      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(lagreBeregningsgrunnlagOMS) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      <Border />

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            onClick={onSubmit}
            loading={isPending(lagreBeregningsgrunnlagOMS) || isPending(endreBeregning)}
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

export default BeregningsgrunnlagOmstillingsstoenad
