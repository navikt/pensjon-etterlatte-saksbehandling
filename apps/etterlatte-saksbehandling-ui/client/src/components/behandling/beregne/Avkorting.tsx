import styled from 'styled-components'
import { isFailure, useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkorting } from '~shared/api/avkorting'
import React, { useEffect, useState } from 'react'
import { IAvkorting } from '~shared/types/IAvkorting'
import { AvkortingInntekt } from '~components/behandling/beregne/AvkortingInntekt'
import { isPending } from '@reduxjs/toolkit'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useParams } from 'react-router-dom'
import { YtelseEtterAvkorting } from '~components/behandling/beregne/YtelseEtterAvkorting'

export const Avkorting = () => {
  const { behandlingId } = useParams()
  const [avkortingStatus, hentAvkortingRequest] = useApiCall(hentAvkorting)
  const [avkorting, setAvkorting] = useState<IAvkorting>()

  useEffect(() => {
    if (!avkorting) {
      if (!behandlingId) throw new Error('Mangler behandlingsid')
      hentAvkortingRequest(behandlingId, (res) => setAvkorting(res))
    }
  }, [])

  return (
    <AvkortingWrapper>
      {!['initial', 'pending'].includes(avkortingStatus.status) && (
        <AvkortingInntekt avkortingGrunnlag={avkorting?.avkortingGrunnlag} setAvkorting={setAvkorting} />
      )}

      <YtelseEtterAvkorting ytelser={avkorting?.avkortetYtelse} />

      {isPending(avkortingStatus) && <Spinner visible={true} label={'Henter avkorting'} />}
      {isFailure(avkortingStatus) && avkortingStatus.error.statusCode !== 404 && (
        <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>
      )}
    </AvkortingWrapper>
  )
}

const AvkortingWrapper = styled.div`
  margin: 2em 0 1em 0;
`
