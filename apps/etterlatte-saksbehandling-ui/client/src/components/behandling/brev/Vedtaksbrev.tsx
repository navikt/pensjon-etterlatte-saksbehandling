import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Alert, ErrorMessage, Heading } from '@navikt/ds-react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { hentVedtaksbrev, opprettVedtaksbrev } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/soeknadoversikt/Soeknadsdato'
import styled from 'styled-components'
import { SendTilAttesteringModal } from '../handlinger/sendTilAttesteringModal'
import {
  behandlingSkalSendeBrev,
  hentBehandlesFraStatus,
  manueltBrevKanRedigeres,
} from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import MottakerPanel from '~components/behandling/brev/detaljer/MottakerPanel'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import Spinner from '~shared/Spinner'
import { BrevProsessType, IBrev } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { isFailure, isPending, isPendingOrInitial, useApiCall } from '~shared/hooks/useApiCall'

export const Vedtaksbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const { sakId, soeknadMottattDato, status } = props.behandling

  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev | undefined>(undefined)

  const [hentBrevStatus, hentBrev] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettVedtaksbrev)

  useEffect(() => {
    if (!behandlingId || !sakId || !behandlingSkalSendeBrev(props.behandling)) return

    hentBrev(behandlingId, (brev, statusCode) => {
      if (statusCode === 200) {
        setVedtaksbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVedtaksbrev({ sakId, behandlingId }, (nyttBrev) => {
          setVedtaksbrev(nyttBrev)
        })
      }
    })
  }, [behandlingId, sakId])

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
            <Soeknadsdato mottattDato={soeknadMottattDato} />

            <br />
            {(vedtaksbrev?.prosessType === BrevProsessType.MANUELL ||
              vedtaksbrev?.prosessType === BrevProsessType.REDIGERBAR) && (
              <Alert variant="warning">
                {manueltBrevKanRedigeres(status)
                  ? 'Kan ikke generere brev automatisk. Du må selv redigere innholdet.'
                  : 'Dette er et manuelt opprettet brev. Kontroller innholdet nøye før attestering.'}
              </Alert>
            )}
            <br />
            {vedtaksbrev && <MottakerPanel vedtaksbrev={vedtaksbrev} />}
          </ContentHeader>
        </Sidebar>

        {!!vedtaksbrev &&
          (vedtaksbrev?.prosessType === BrevProsessType.AUTOMATISK ? (
            <ForhaandsvisningBrev brev={vedtaksbrev} />
          ) : (
            <RedigerbartBrev brev={vedtaksbrev!!} kanRedigeres={manueltBrevKanRedigeres(status)} />
          ))}

        {isFailure(hentBrevStatus) && <ErrorMessage>Feil ved henting av brev</ErrorMessage>}
        {isFailure(opprettBrevStatus) && <ErrorMessage>Kunne ikke opprette brev</ErrorMessage>}
      </BrevContent>

      <Border />

      <BehandlingHandlingKnapper>
        {hentBehandlesFraStatus(status) && <SendTilAttesteringModal />}
      </BehandlingHandlingKnapper>
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
