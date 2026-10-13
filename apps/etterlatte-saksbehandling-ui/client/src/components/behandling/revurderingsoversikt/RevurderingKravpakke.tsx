import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useBehandlingRedigerbarhet } from '~components/behandling/useBehandlingRedigerbarhet'
import { BoddEllerArbeidetUtlandet } from '~components/behandling/soeknadsoversikt/boddEllerArbeidetUtlandet/BoddEllerArbeidetUtlandet'
import React from 'react'
import styled from 'styled-components'

export const RevurderingKravpakke = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props

  const { redigerbar } = useBehandlingRedigerbarhet(behandling)
  return (
    <MarginTop>
      <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={redigerbar} />
    </MarginTop>
  )
}

const MarginTop = styled.div`
  margin-top: 3em;
`
