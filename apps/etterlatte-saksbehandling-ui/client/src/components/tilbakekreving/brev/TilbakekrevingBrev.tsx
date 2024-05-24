import { Alert, Box, Heading } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import { erUnderBehandling, TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { fattVedtak } from '~shared/api/tilbakekreving'
import React, { useEffect, useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtaksbrev, opprettVedtaksbrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'

import { isFailure, isPending } from '~shared/api/apiUtils'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { ApiErrorAlert } from '~ErrorBoundary'

export function TilbakekrevingBrev({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const kanAttesteres = erUnderBehandling(behandling.status)
  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev | undefined>(undefined)
  const [hentBrevStatus, hentBrevRequest] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettVedtaksbrev)

  useEffect(() => {
    hentBrevRequest(behandling.id, (brev, statusCode) => {
      if (statusCode === 200) {
        setVedtaksbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVedtaksbrev({ behandlingId: behandling.id, sakId: behandling.sak.id }, (nyttBrev) => {
          setVedtaksbrev(nyttBrev)
        })
      }
    })
  }, [behandling])

  if (isPending(hentBrevStatus)) {
    return <Spinner visible label="Henter brev ..." />
  }
  if (isPending(opprettBrevStatus)) {
    return <Spinner visible label="Ingen brev funnet. Oppretter brev ..." />
  }

  if (isFailure(hentBrevStatus)) {
    return <ApiErrorAlert>{hentBrevStatus.error.detail}</ApiErrorAlert>
  }
  if (isFailure(opprettBrevStatus)) {
    return <ApiErrorAlert>{opprettBrevStatus.error.detail}</ApiErrorAlert>
  }

  return (
    <>
      <BrevContent>
        <Sidebar>
          <Box paddingInline="16" paddingBlock="4">
            <HeadingWrapper>
              <Heading spacing size="large" level="1">
                Vedtaksbrev
              </Heading>
            </HeadingWrapper>

            <br />
            <Alert variant="warning">
              Dette er et manuelt opprettet brev. Kontroller innholdet nøye før attestering.
            </Alert>
            {vedtaksbrev && (
              <>
                <br />
                <BrevSpraak brev={vedtaksbrev} kanRedigeres={redigerbar} />
                <br />
                <BrevMottaker brev={vedtaksbrev} kanRedigeres={redigerbar} />
              </>
            )}
          </Box>
        </Sidebar>

        {vedtaksbrev && <RedigerbartBrev brev={vedtaksbrev} kanRedigeres={redigerbar} />}
      </BrevContent>
      <Border />
      <FlexRow justify="center">
        {kanAttesteres && (
          <SendTilAttesteringModal
            behandlingId={behandling.id}
            fattVedtakApi={fattVedtak}
            validerKanSendeTilAttestering={() => true}
          />
        )}
      </FlexRow>
    </>
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
