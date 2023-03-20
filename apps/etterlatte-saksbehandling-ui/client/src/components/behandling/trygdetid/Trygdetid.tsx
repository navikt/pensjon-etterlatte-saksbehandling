import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Button, Heading } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { handlinger } from '~components/behandling/handlinger/typer'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentTrygdetid, ITrygdetid, opprettTrygdetid } from '~shared/api/trygdetid'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TrygdetidBeregnet } from '~components/behandling/trygdetid/TrygdetidBeregnet'
import { Content, ContentHeader } from '~shared/styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const Trygdetid = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const { behandlingId } = useParams()

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
          <ContentHeader>
            <Heading spacing size="medium" level="3">
              Grunnlag trygdetid
            </Heading>
          </ContentHeader>
          <TrygdetidGrunnlag trygdetid={trygdetid} setTrygdetid={setTrygdetid} />
          <TrygdetidGrunnlag trygdetid={trygdetid} setTrygdetid={setTrygdetid} erFremtidigTrygdetid={true} />
          <TrygdetidBeregnet trygdetid={trygdetid} setTrygdetid={setTrygdetid} />
        </>
      )}
      {isPending(trygdetidStatus) && <Spinner visible={true} label={'Henter trygdetid'} />}
      {isFailure(trygdetidStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
      <Border />

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" className="button" onClick={next}>
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
