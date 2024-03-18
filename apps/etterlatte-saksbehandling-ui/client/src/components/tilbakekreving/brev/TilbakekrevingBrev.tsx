import { Alert, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import { erUnderBehandling, TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { fattVedtak, opprettVedtak } from '~shared/api/tilbakekreving'
import React, { useEffect, useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtaksbrev, opprettVedtaksbrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'

import { isPending, isPendingOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { VedtakSammendrag } from '~components/vedtak/typer'

export function TilbakekrevingBrev({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const kanAttesteres = erUnderBehandling(behandling.status)
  const [, hentVedtak] = useApiCall(hentVedtakSammendrag)
  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev | undefined>(undefined)
  const [hentBrevStatus, hentBrevRequest] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettVedtaksbrev)

  const hentBrev = () => {
    hentBrevRequest(behandling.id, (brev, statusCode) => {
      if (statusCode === 200) {
        setVedtaksbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVedtaksbrev({ behandlingId: behandling.id, sakId: behandling.sak.id }, (nyttBrev) => {
          setVedtaksbrev(nyttBrev)
        })
      }
    })
  }

  useEffect(() => {
    hentVedtak(behandling.id, (vedtak: VedtakSammendrag | null) => {
      if (vedtak) {
        hentBrev()
      } else {
        opprettVedtak(behandling.id).then(() => hentBrev())
      }
    })
  }, [behandling])

  if (isPendingOrInitial(hentBrevStatus)) {
    return <Spinner visible label="Henter brev ..." />
  } else if (isPending(opprettBrevStatus)) {
    return <Spinner visible label="Ingen brev funnet. Oppretter brev ..." />
  }

  return (
    <Content>
      <BrevContent>
        <Sidebar>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing size="large" level="1">
                Vedtaksbrev
              </Heading>
            </HeadingWrapper>

            <br />
            <Alert variant="warning">
              Dette er et manuelt opprettet brev. Kontroller innholdet nøye før attestering.
            </Alert>
            <br />
            {vedtaksbrev && <BrevMottaker brev={vedtaksbrev} kanRedigeres={redigerbar} />}
          </ContentHeader>
        </Sidebar>

        {vedtaksbrev && <RedigerbartBrev brev={vedtaksbrev} kanRedigeres={redigerbar} />}
        {isFailureHandler({
          apiResult: hentBrevStatus,
          errorMessage: 'Feil ved henting av brev',
        })}
        {isFailureHandler({
          apiResult: opprettBrevStatus,
          errorMessage: 'Kunne ikke opprette brev',
        })}
      </BrevContent>
      <Border />
      <FlexRow justify="center">
        {kanAttesteres && (
          <SendTilAttesteringModal
            behandlingId={behandling.id}
            fattVedtakApi={fattVedtak}
            sakId={behandling.sak.id}
            validerKanSendeTilAttestering={() => true}
          />
        )}
      </FlexRow>
    </Content>
  )
}

const BrevContent = styled.div`
  display: flex;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
