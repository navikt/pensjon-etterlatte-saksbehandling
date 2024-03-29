import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Button, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import React from 'react'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { oppdaterStatus } from '~shared/api/trygdetid'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { handlinger } from '~components/behandling/handlinger/typer'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const TrygdetidVisning = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const { next } = useBehandlingRoutes()

  const vedtaksresultat = useVedtaksResultat()

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const [oppdaterStatusResult, oppdaterStatusRequest] = useApiCall(oppdaterStatus)
  const virkningstidspunktEtterNyRegelDato = () => {
    return (
      behandling.virkningstidspunkt == null || new Date(behandling.virkningstidspunkt.dato) >= new Date('2024-01-01')
    )
  }

  const sjekkGyldighetOgOppdaterStatus = () => {
    oppdaterStatusRequest(behandling.id, (result) => {
      if (result.statusOppdatert) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
      }
      next()
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Trygdetid
          </Heading>
          <Vilkaarsresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
        </HeadingWrapper>
      </ContentHeader>

      <Trygdetid
        redigerbar={redigerbar}
        behandling={behandling}
        vedtaksresultat={vedtaksresultat}
        virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato()}
      />

      <Border />
      {isFailureHandler({
        apiResult: oppdaterStatusResult,
        errorMessage: 'Kunne ikke oppdatere trygdetid status',
      })}

      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" loading={isPending(oppdaterStatusResult)} onClick={sjekkGyldighetOgOppdaterStatus}>
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

export default TrygdetidVisning
