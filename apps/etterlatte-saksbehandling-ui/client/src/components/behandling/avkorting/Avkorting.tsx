import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect } from 'react'
import { AvkortingInntekt } from '~components/behandling/avkorting/AvkortingInntekt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import { IBehandlingReducer, oppdaterAvkorting, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { mapApiResult } from '~shared/api/apiUtils'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'

export const Avkorting = ({
  behandling,
  resetBrevutfallvalidering,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  resetBrevutfallvalidering: () => void
  resetInntektsavkortingValidering: () => void
}) => {
  const dispatch = useAppDispatch()
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  useEffect(() => {
    if (!avkorting || avkorting.behandlingId !== behandling.id) {
      hentAvkortingRequest(behandling.id, (res) => {
        const avkortingFinnesOgErUnderBehandling = res && redigerbar
        if (avkortingFinnesOgErUnderBehandling) {
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.AVKORTET))
        }
        dispatch(oppdaterAvkorting(res))
      })
    }
  }, [])

  return (
    <AvkortingWrapper>
      {mapApiResult(
        avkortingStatus,
        <Spinner visible label="Henter avkorting" />,
        () => (
          <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>
        ),
        () => (
          <AvkortingInntekt
            behandling={behandling}
            redigerbar={redigerbar}
            resetInntektsavkortingValidering={resetInntektsavkortingValidering}
          />
        )
      )}
      {avkorting && <YtelseEtterAvkorting />}
      {avkorting && <Brevutfall behandling={behandling} resetBrevutfallvalidering={resetBrevutfallvalidering} />}
    </AvkortingWrapper>
  )
}

const AvkortingWrapper = styled.div`
  margin: 2em 0 1em 0;
`
