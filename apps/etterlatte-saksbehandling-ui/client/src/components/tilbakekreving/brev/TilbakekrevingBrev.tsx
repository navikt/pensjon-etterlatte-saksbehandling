import { Alert, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/sendTilAttesteringModal'
import { TilbakekrevingBehandling, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { fattVedtak, opprettVedtak } from '~shared/api/tilbakekreving'
import React, { useEffect, useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtaksbrev, opprettVedtaksbrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import { useVedtak } from '~components/vedtak/useVedtak'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'

import { isPending, isPendingOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppSelector } from '~store/Store'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'

export function TilbakekrevingBrev({ tilbakekreving }: { tilbakekreving: TilbakekrevingBehandling }) {
  const kanAttesteres = [
    TilbakekrevingStatus.OPPRETTET,
    TilbakekrevingStatus.UNDER_ARBEID,
    TilbakekrevingStatus.UNDERKJENT,
  ].includes(tilbakekreving.status)
  const vedtak = useVedtak()
  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev | undefined>(undefined)
  const [hentBrevStatus, hentBrevRequest] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettVedtaksbrev)
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const redigerbar = enhetErSkrivbar(tilbakekreving.sak.enhet, innloggetSaksbehandler.skriveEnheter)

  const hentBrev = () => {
    hentBrevRequest(tilbakekreving.id, (brev, statusCode) => {
      if (statusCode === 200) {
        setVedtaksbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVedtaksbrev({ behandlingId: tilbakekreving.id, sakId: tilbakekreving.sak.id }, (nyttBrev) => {
          setVedtaksbrev(nyttBrev)
        })
      }
    })
  }

  useEffect(() => {
    if (vedtak) {
      hentBrev()
    } else {
      opprettVedtak(tilbakekreving.id).then(() => hentBrev())
    }
  }, [tilbakekreving, vedtak])

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
      <FlexRow justify="center">
        {kanAttesteres && (
          <SendTilAttesteringModal
            behandlingId={tilbakekreving.id}
            fattVedtakApi={fattVedtak}
            sakId={tilbakekreving.sak.id}
            validerKanSendeTilAttestering={() => true}
          />
        )}
      </FlexRow>
    </Content>
  )
}

const BrevContent = styled.div`
  display: flex;
  height: 75vh;
  max-height: 75vh;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
