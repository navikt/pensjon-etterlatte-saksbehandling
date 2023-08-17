import { Button, Loader } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingReducer, oppdaterBehandlingsstatus, resetBeregning } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { Trygdetid as BeregnetTrygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import React, { useEffect, useState } from 'react'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import YrkesskadeTrygdetidOMS from '~components/behandling/beregningsgrunnlag/YrkesskadeTrygdetidOMS'
import FastTrygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'

const BeregningsgrunnlagOmstillingsstoenad = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const [beregning, setOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const behandles = hentBehandlesFraStatus(behandling.status)
  const beregnTrygdetid = useState<boolean>(false)
  const [yrkesskadeTrygdetid, setYrkesskadeTrygdetid] = useState<boolean>(false)
  const [vilkaarsvurdering, getVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const oppdaterBeregning = () => {
    dispatch(resetBeregning())
    setOpprettEllerEndreBeregning(behandling.id, () => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
      next()
    })
  }

  useEffect(() => {
    getVilkaarsvurdering(behandling.id, (vurdering) => {
      setYrkesskadeTrygdetid(vurdering.isYrkesskade)
    })
  }, [])

  return (
    <>
      {isFailure(beregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isSuccess(vilkaarsvurdering) &&
        (yrkesskadeTrygdetid ? (
          <YrkesskadeTrygdetidOMS />
        ) : beregnTrygdetid ? (
          <BeregnetTrygdetid redigerbar={behandles} utenlandstilsnitt={behandling.utenlandstilsnitt} />
        ) : (
          <FastTrygdetid />
        ))}
      <Border />
      {isFailure(vilkaarsvurdering) && <ApiErrorAlert>Kunne ikke hente vilkårsvurdering</ApiErrorAlert>}
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
