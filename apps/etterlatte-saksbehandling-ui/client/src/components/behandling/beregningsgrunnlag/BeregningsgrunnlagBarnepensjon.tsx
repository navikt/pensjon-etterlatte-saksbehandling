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
import FastTrygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'
import { Trygdetid as BeregnetTrygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import React, { useEffect, useState } from 'react'
import Institusjonsopphold from '~components/behandling/beregningsgrunnlag/Institusjonsopphold'
import Soeskenjustering, {
  Soeskengrunnlag,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import Spinner from '~shared/Spinner'
import { IPdlPerson } from '~shared/types/Person'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import YrkesskadeTrygdetid from './YrkesskadeTrygdetid'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const behandles = hentBehandlesFraStatus(behandling.status)
  const dispatch = useAppDispatch()
  const [lagreBeregningsgrunnlag, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlag)
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [funksjonsbrytere, postHentFunksjonsbrytere] = useApiCall(hentFunksjonsbrytere)
  const [beregnTrygdetid, setBeregnTrygdetid] = useState<boolean>(false)
  const [vilkaarsvurdering, getVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [yrkesskadeTrygdetid, setYrkesskadeTrygdetid] = useState<boolean>(false)
  const [visInstitusjonsopphold, setVisInstitusjonsopphold] = useState<boolean>(false)
  const [soeskenGrunnlagsData, setSoeskenGrunnlagsData] = useState<Soeskengrunnlag | undefined>(undefined)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] = useState<
    InstitusjonsoppholdGrunnlagData | undefined
  >(undefined)

  const [manglerSoeskenJustering, setSoeskenJusteringMangler] = useState<boolean>(false)
  const featureToggleNameTrygdetid = 'pensjon-etterlatte.bp-bruk-faktisk-trygdetid'
  const featureToggleNameInstitusjonsopphold = 'pensjon-etterlatte.bp-bruk-institusjonsopphold'

  useEffect(() => {
    postHentFunksjonsbrytere([featureToggleNameTrygdetid, featureToggleNameInstitusjonsopphold], (brytere) => {
      const trygdetidBryter = brytere.find((bryter) => bryter.toggle === featureToggleNameTrygdetid)

      if (trygdetidBryter) {
        setBeregnTrygdetid(trygdetidBryter.enabled)

        getVilkaarsvurdering(behandling.id, (vurdering) => {
          setYrkesskadeTrygdetid(vurdering.isYrkesskade)
        })
      }
      const institusjonsoppholdBryter = brytere.find((bryter) => bryter.toggle === featureToggleNameInstitusjonsopphold)
      if (institusjonsoppholdBryter) {
        setVisInstitusjonsopphold(institusjonsoppholdBryter.enabled)
      }
    })

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
      {isSuccess(funksjonsbrytere) &&
        isSuccess(vilkaarsvurdering) &&
        (yrkesskadeTrygdetid ? (
          <YrkesskadeTrygdetid />
        ) : beregnTrygdetid ? (
          <BeregnetTrygdetid redigerbar={behandles} utenlandstilsnitt={behandling.utenlandstilsnitt} />
        ) : (
          <FastTrygdetid />
        ))}
      <>
        {isSuccess(beregningsgrunnlag) && behandling.familieforhold && (
          <Soeskenjustering
            behandling={behandling}
            onSubmit={(soeskenGrunnlag) => setSoeskenGrunnlagsData(soeskenGrunnlag)}
            setSoeskenJusteringManglerIkke={() => setSoeskenJusteringMangler(false)}
          />
        )}
        {isSuccess(funksjonsbrytere) && visInstitusjonsopphold && isSuccess(beregningsgrunnlag) && (
          <Institusjonsopphold
            behandling={behandling}
            onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}
          />
        )}
        <Spinner visible={isPending(beregningsgrunnlag)} label={'Henter beregningsgrunnlag'} />
        {isFailure(beregningsgrunnlag) && <ApiErrorAlert>Beregningsgrunnlag kan ikke hentes</ApiErrorAlert>}
      </>
      {manglerSoeskenJustering && <ApiErrorAlert>Søskenjustering er ikke fylt ut </ApiErrorAlert>}
      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(lagreBeregningsgrunnlag) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}
      {isFailure(vilkaarsvurdering) && <ApiErrorAlert>Kunne ikke hente vilkaarsvurdering</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            size="medium"
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
