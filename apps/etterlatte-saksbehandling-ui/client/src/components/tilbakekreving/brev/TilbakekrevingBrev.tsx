import { Alert, Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import { erUnderBehandling, TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { fattVedtak } from '~shared/api/tilbakekreving'
import React, { useEffect, useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevTilBehandling, opprettBrevTilBehandling } from '~shared/api/brev'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'

import { isFailure, isPending } from '~shared/api/apiUtils'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { ApiErrorAlert } from '~ErrorBoundary'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'

export function TilbakekrevingBrev({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const kanAttesteres = erUnderBehandling(behandling.status)
  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev | undefined>(undefined)
  const [hentBrevStatus, hentBrevRequest] = useApiCall(hentBrevTilBehandling)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettBrevTilBehandling)
  const [tilbakestilt, setTilbakestilt] = useState(false)

  useEffect(() => {
    hentBrevRequest(behandling.id, (brev, statusCode) => {
      if (statusCode === 200) {
        setVedtaksbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVedtaksbrev(
          {
            behandlingId: behandling.id,
            sakId: behandling.sak.id,
          },
          (nyttBrev) => {
            setVedtaksbrev(nyttBrev)
          }
        )
      }
    })
  }, [behandling, tilbakestilt])

  if (isPending(hentBrevStatus)) {
    return <Spinner label="Henter brev ..." />
  }
  if (isPending(opprettBrevStatus)) {
    return <Spinner label="Ingen brev funnet. Oppretter brev ..." />
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
          <Box paddingInline="space-16" paddingBlock="space-16 space-4">
            <VStack gap="space-4">
              <Heading size="large" level="1">
                Vedtaksbrev
              </Heading>

              <Alert variant="warning">
                Dette er et manuelt opprettet brev. Kontroller innholdet nøye før attestering.
              </Alert>

              {vedtaksbrev && (
                <>
                  <BrevSpraak brev={vedtaksbrev} kanRedigeres={redigerbar} />

                  <BrevMottakerWrapper brev={vedtaksbrev} kanRedigeres={redigerbar} />
                </>
              )}
            </VStack>
          </Box>
        </Sidebar>

        {vedtaksbrev && (
          <RedigerbartBrev
            brev={vedtaksbrev}
            kanRedigeres={redigerbar}
            tilbakestillingsaction={() => setTilbakestilt(true)}
          />
        )}
      </BrevContent>
      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="neutral-subtle">
        <HStack justify="center">
          {kanAttesteres && (
            <SendTilAttesteringModal
              behandlingId={behandling.id}
              fattVedtakApi={fattVedtak}
              validerKanSendeTilAttestering={() => true}
            />
          )}
        </HStack>
      </Box>
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
