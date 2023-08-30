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
import { Trygdetid as BeregnetTrygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import React, { useEffect, useState } from 'react'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import YrkesskadeTrygdetidOMS from '~components/behandling/beregningsgrunnlag/YrkesskadeTrygdetidOMS'
import FastTrygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'
import InstitusjonsoppholdOMS from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdBP'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'

const BeregningsgrunnlagOmstillingsstoenad = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const behandles = hentBehandlesFraStatus(behandling.status)
  const beregnTrygdetid = useState<boolean>(false)
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlagOMS)
  const [yrkesskadeTrygdetid, setYrkesskadeTrygdetid] = useState<boolean>(false)
  const [vilkaarsvurdering, getVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [visInstitusjonsopphold, setVisInstitusjonsopphold] = useState<boolean>(false)
  const [lagreBeregningsgrunnlagOMS, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlagOMS)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] = useState<
    InstitusjonsoppholdGrunnlagData | undefined
  >(undefined)

  useEffect(() => {
    getVilkaarsvurdering(behandling.id, (vurdering) => {
      setYrkesskadeTrygdetid(vurdering.isYrkesskade)
    })
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
    setVisInstitusjonsopphold(true)
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
      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isSuccess(vilkaarsvurdering) &&
        (yrkesskadeTrygdetid ? (
          <YrkesskadeTrygdetidOMS />
        ) : beregnTrygdetid ? (
          <BeregnetTrygdetid redigerbar={behandles} utenlandstilsnitt={behandling.utenlandstilsnitt} />
        ) : (
          <FastTrygdetid />
        ))}
      <Border />
      {visInstitusjonsopphold && isSuccess(beregningsgrunnlag) && (
        <InstitusjonsoppholdOMS
          behandling={behandling}
          onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}
        />
      )}
      {isFailure(vilkaarsvurdering) && <ApiErrorAlert>Kunne ikke hente vilkårsvurdering</ApiErrorAlert>}
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            size="medium"
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
