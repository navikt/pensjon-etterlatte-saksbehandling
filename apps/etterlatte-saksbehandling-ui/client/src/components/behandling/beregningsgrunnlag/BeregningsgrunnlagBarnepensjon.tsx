import { Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { behandlingErRedigerbar } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { hentBeregningsGrunnlag, lagreBeregningsGrunnlag, opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlag,
  oppdaterBeregning,
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
import { hentLevendeSoeskenFraAvdoedeForSoeker } from '~shared/types/Person'
import {
  Beregning,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  InstitusjonsoppholdGrunnlagData,
} from '~shared/types/Beregning'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import BeregningsgrunnlagMetode from './BeregningsgrunnlagMetode'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysningerAvdoede } from '~components/person/usePersonopplysninger'

const featureToggleNameInstitusjonsopphold = 'pensjon-etterlatte.bp-bruk-institusjonsopphold' as const
const featureToggleNameBrukFaktiskTrygdetid = 'pensjon-etterlatte.bp-bruk-faktisk-trygdetid' as const

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const avdoede = usePersonopplysningerAvdoede()
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const dispatch = useAppDispatch()
  const [lagreBeregningsgrunnlag, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlag)
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const visInstitusjonsopphold = useFeatureEnabledMedDefault(featureToggleNameInstitusjonsopphold, false)
  const visBeregningsmetode = useFeatureEnabledMedDefault(featureToggleNameBrukFaktiskTrygdetid, false)
  const [soeskenGrunnlagsData, setSoeskenGrunnlagsData] = useState<Soeskengrunnlag | null>(null)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] =
    useState<InstitusjonsoppholdGrunnlagData | null>(null)
  const [beregningsMetodeBeregningsgrunnlag, setBeregningsMetodeBeregningsgrunnlag] =
    useState<BeregningsMetodeBeregningsgrunnlag | null>(null)

  const [manglerSoeskenJustering, setSoeskenJusteringMangler] = useState<boolean>(false)

  useEffect(() => {
    fetchBeregningsgrunnlag(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlag({ ...result, institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag })
        )
        setBeregningsMetodeBeregningsgrunnlag(result.beregningsMetode)
      }
    })
  }, [])

  if (behandling.kommerBarnetTilgode == null || avdoede == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const soesken =
    (avdoede && hentLevendeSoeskenFraAvdoedeForSoeker(avdoede, behandling.søker?.foedselsnummer as string)) ?? []
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
        beregningsMetode: beregningsMetodeBeregningsgrunnlag
          ? beregningsMetodeBeregningsgrunnlag
          : behandling.beregningsGrunnlag?.beregningsMetode ?? {
              beregningsMetode: BeregningsMetode.NASJONAL,
            },
      }

      postBeregningsgrunnlag(
        {
          behandlingId: behandling.id,
          grunnlag: beregningsgrunnlag,
        },
        () =>
          postOpprettEllerEndreBeregning(behandling.id, (beregning: Beregning) => {
            dispatch(oppdaterBeregingsGrunnlag(beregningsgrunnlag))
            dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
            dispatch(oppdaterBeregning(beregning))
            next()
          })
      )
    }
  }

  return (
    <>
      <>
        {visBeregningsmetode && isSuccess(beregningsgrunnlag) && (
          <BeregningsgrunnlagMetode
            redigerbar={redigerbar}
            grunnlag={beregningsMetodeBeregningsgrunnlag}
            onUpdate={(grunnlag) => {
              setBeregningsMetodeBeregningsgrunnlag({ ...grunnlag })
            }}
          />
        )}
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

      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            onClick={onSubmit}
            loading={isPending(lagreBeregningsgrunnlag) || isPending(endreBeregning)}
          >
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
  )
}

export default BeregningsgrunnlagBarnepensjon
