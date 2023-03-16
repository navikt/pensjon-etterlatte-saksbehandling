import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Button, Heading } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { handlinger } from '~components/behandling/handlinger/typer'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { useAppSelector } from '~store/Store'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { TrygdetidOppsummert } from '~components/behandling/trygdetid/TrygdetidOppsummert'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentTrygdetid, ITrygdetid, opprettTrygdetid } from '~shared/api/trygdetid'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const Trygdetid = () => {
  const { behandlingId } = useParams()

  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling.status)
  const { next } = useBehandlingRoutes()

  const [trygdetidStatus, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [, requestOpprettTrygdetid] = useApiCall(opprettTrygdetid)
  const [trygdetid, setTrygdetid] = useState<ITrygdetid>()

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    fetchTrygdetid(behandlingId, (trygdetid: ITrygdetid) => {
      if (trygdetid == null) {
        requestOpprettTrygdetid(behandlingId, (trygdetid: ITrygdetid) => {
          setTrygdetid(trygdetid)
        })
      } else {
        setTrygdetid(trygdetid)
      }
    })
  }, [])

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Trygdetid
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      {trygdetid && (
        <>
          <TrygdetidGrunnlag trygdetid={trygdetid} setTrygdetid={setTrygdetid} />
          <TrygdetidOppsummert trygdetid={trygdetid} setTrygdetid={setTrygdetid} />
        </>
      )}
      {isPending(trygdetidStatus) && <Spinner visible={true} label={'Henter trygdetid'} />}
      {isFailure(trygdetidStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
      <Border />

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" className="button" onClick={next}>
            {handlinger.START.navn}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
