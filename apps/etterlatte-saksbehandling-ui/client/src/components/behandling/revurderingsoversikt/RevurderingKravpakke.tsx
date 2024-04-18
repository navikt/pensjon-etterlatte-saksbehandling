import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { BoddEllerArbeidetUtlandet } from '~components/behandling/soeknadsoversikt/boddEllerArbeidetUtlandet/BoddEllerArbeidetUtlandet'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import React from 'react'
import styled from 'styled-components'

export const RevurderingKravpakke = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  return (
    <MarginTop>
      <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={redigerbar} />
    </MarginTop>
  )
}

const MarginTop = styled.div`
  margin-top: 3em;
`
