import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect, useState } from 'react'
import { IAvkorting } from '~shared/types/IAvkorting'
import { AvkortingInntekt } from '~components/behandling/avkorting/AvkortingInntekt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~shared/error/ApiErrorAlert'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import { IBehandlingReducer, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'

import { mapApiResult } from '~shared/api/apiUtils'

export const Avkorting = (props: { behandling: IBehandlingReducer }) => {
  const behandling = props.behandling
  const dispatch = useAppDispatch()
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const [avkorting, setAvkorting] = useState<IAvkorting>()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const redigerbar = behandlingErRedigerbar(behandling.status) && innloggetSaksbehandler.skriveTilgang

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
            avkortingGrunnlag={avkorting == null ? [] : avkorting.avkortingGrunnlag}
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
    </AvkortingWrapper>
  )
}

const AvkortingWrapper = styled.div`
  margin: 2em 0 1em 0;
`
