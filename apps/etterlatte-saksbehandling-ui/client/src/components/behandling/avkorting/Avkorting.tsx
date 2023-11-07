import styled from 'styled-components'
import { isErrorWithCode, useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect, useState } from 'react'
import { IAvkorting } from '~shared/types/IAvkorting'
import { AvkortingInntekt } from '~components/behandling/avkorting/AvkortingInntekt'
import { isPending } from '@reduxjs/toolkit'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

export const Avkorting = (props: { behandling: IBehandlingReducer }) => {
  const behandling = props.behandling
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const [avkorting, setAvkorting] = useState<IAvkorting>()

  useEffect(() => {
    if (!avkorting) {
      hentAvkortingRequest(behandling.id, (res) => setAvkorting(res))
    }
  }, [])

  return (
    <AvkortingWrapper>
      {!['initial', 'pending'].includes(avkortingStatus.status) && (
        <AvkortingInntekt
          behandling={behandling}
          avkortingGrunnlag={avkorting == null ? [] : avkorting.avkortingGrunnlag}
          setAvkorting={setAvkorting}
        />
      )}
      {avkorting && (
        <YtelseEtterAvkorting
          ytelser={avkorting.avkortetYtelse}
          behandling={behandling}
          tidligereYtelser={avkorting.tidligereAvkortetYtelse}
          setAvkorting={setAvkorting}
        />
      )}

      {isPending(avkortingStatus) && <Spinner visible={true} label="Henter avkorting" />}
      {isErrorWithCode(avkortingStatus, 404) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
    </AvkortingWrapper>
  )
}

const AvkortingWrapper = styled.div`
  margin: 2em 0 1em 0;
`
