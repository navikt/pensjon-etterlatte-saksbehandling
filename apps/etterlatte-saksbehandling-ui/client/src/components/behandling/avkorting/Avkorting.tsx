import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect, useState } from 'react'
import { IAvkorting } from '~shared/types/IAvkorting'
import { AvkortingInntekt } from '~components/behandling/avkorting/AvkortingInntekt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import { IBehandlingReducer, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { mapApiResult } from '~shared/api/apiUtils'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'

export const Avkorting = ({
  behandling,
  resetBrevutfallvalidering,
}: {
  behandling: IBehandlingReducer
  resetBrevutfallvalidering: () => void
}) => {
  const dispatch = useAppDispatch()
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const [avkorting, setAvkorting] = useState<IAvkorting>()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  useEffect(() => {
    if (!avkorting) {
      hentAvkortingRequest(behandling.id, (res) => {
        const avkortingFinnesOgErUnderBehandling = res && redigerbar
        if (avkortingFinnesOgErUnderBehandling) {
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.AVKORTET))
        }
        setAvkorting(res)
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
            avkorting={avkorting}
            setAvkorting={setAvkorting}
            redigerbar={redigerbar}
          />
        )
      )}
      {avkorting && (
        <YtelseEtterAvkorting
          ytelser={avkorting.avkortetYtelse}
          behandling={behandling}
          tidligereYtelser={avkorting.tidligereAvkortetYtelse}
          setAvkorting={setAvkorting}
        />
      )}
      {avkorting && <Brevutfall behandling={behandling} resetBrevutfallvalidering={resetBrevutfallvalidering} />}
    </AvkortingWrapper>
  )
}

const AvkortingWrapper = styled.div`
  margin: 2em 0 1em 0;
`
