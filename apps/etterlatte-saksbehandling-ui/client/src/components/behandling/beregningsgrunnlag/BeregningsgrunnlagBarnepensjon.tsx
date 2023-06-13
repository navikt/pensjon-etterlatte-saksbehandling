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
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import { useEffect, useState } from 'react'
import { InstitusjonsoppholdGrunnlag } from '~shared/types/Beregning'
import Institusjonsopphold from '~components/behandling/beregningsgrunnlag/Institusjonsopphold'
import Soeskenjustering, {
  Soeskengrunnlag,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const behandles = hentBehandlesFraStatus(behandling.status)
  const dispatch = useAppDispatch()
  const [lagreSoeskenMedIBeregningStatus, postSoeskenMedIBeregning] = useApiCall(lagreBeregningsGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [funksjonsbrytere, postHentFunksjonsbrytere] = useApiCall(hentFunksjonsbrytere)
  const [beregnTrygdetid, setBeregnTrygdetid] = useState<boolean>(false)
  const [visInstitusjonsopphold, setVisInstitusjonsopphold] = useState<boolean>(false)
  const [soeskenGrunnlagsData, setSoeskenGrunnlagsData] = useState<Soeskengrunnlag | undefined>(undefined)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] = useState<
    InstitusjonsoppholdGrunnlag | undefined
  >(undefined)

  const [manglerSoeskenJustering, setSoeskenJusteringMangler] = useState<boolean>(false)
  const featureToggleNameTrygdetid = 'pensjon-etterlatte.bp-bruk-faktisk-trygdetid'
  const featureToggleNameInstitusjonsopphold = 'pensjon-etterlatte.bp-bruk-institusjonsopphold'

  useEffect(() => {
    postHentFunksjonsbrytere([featureToggleNameTrygdetid, featureToggleNameInstitusjonsopphold], (brytere) => {
      const trygdetidBryter = brytere.find((bryter) => bryter.toggle === featureToggleNameTrygdetid)

      if (trygdetidBryter) {
        setBeregnTrygdetid(trygdetidBryter.enabled)
      }
      const institusjonsoppholdBryter = brytere.find((bryter) => bryter.toggle === featureToggleNameInstitusjonsopphold)
      if (institusjonsoppholdBryter) {
        setVisInstitusjonsopphold(institusjonsoppholdBryter.enabled)
      }
    })
  }, [])

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const onSubmit = () => {
    if (soeskenGrunnlagsData) {
      setSoeskenJusteringMangler(false)
      dispatch(resetBeregning())
      const beregningsgrunnlag = {
        soeskenMedIBeregning: mapListeTilDto(soeskenGrunnlagsData),
        institusjonsopphold: institusjonsoppholdsGrunnlagData,
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
    } else {
      setSoeskenJusteringMangler(true)
    }
  }
  return (
    <>
      {!isPending(funksjonsbrytere) &&
        (beregnTrygdetid ? <BeregnetTrygdetid redigerbar={behandles} /> : <FastTrygdetid />)}
      <Soeskenjustering
        behandling={behandling}
        onSubmit={(soeskenGrunnlag) => setSoeskenGrunnlagsData(soeskenGrunnlag)}
      />
      {!isPending(funksjonsbrytere) && !isFailure(funksjonsbrytere) && visInstitusjonsopphold && (
        <Institusjonsopphold
          behandling={behandling}
          onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}
        />
      )}
      {manglerSoeskenJustering && <ApiErrorAlert>Søskenjustering er ikke fylt ut </ApiErrorAlert>}
      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(lagreSoeskenMedIBeregningStatus) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            size="medium"
            onClick={onSubmit}
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
