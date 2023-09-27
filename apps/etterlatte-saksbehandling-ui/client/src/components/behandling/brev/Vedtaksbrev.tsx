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
import { BrevProsessType } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'

export const Vedtaksbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const { sakId, soeknadMottattDato, status } = props.behandling

  const [vedtaksbrev, setVedtaksbrev] = useState<any>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (!behandlingId || !sakId) return

    const fetchVedtaksbrev = async () => {
      if (!behandlingSkalSendeBrev(props.behandling)) {
        return
      }

      const brevResponse = await hentVedtaksbrev(behandlingId!!)
      if (brevResponse.status === 'ok' && brevResponse.statusCode === 200) {
        setVedtaksbrev(brevResponse.data)
      } else if (brevResponse.statusCode === 204) {
        const brevOpprettetResponse = await opprettVedtaksbrev(sakId, behandlingId!!)

        if (brevOpprettetResponse.status === 'ok' && brevOpprettetResponse.statusCode === 201) {
          setVedtaksbrev(brevOpprettetResponse.data)
        } else {
          setError('Oppretting av vedtaksbrev feilet...')
        }
      } else {
        setError('Feil ved henting av brev...')
      }
      setLoading(false)
    }
    fetchVedtaksbrev()
  }, [behandlingId, sakId])

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

        {loading ? (
          <SpinnerContainer>
            <Spinner visible={true} label="Henter brev ..." />
          </SpinnerContainer>
        ) : error ? (
          <ErrorMessage>{error}</ErrorMessage>
        ) : vedtaksbrev.prosessType === BrevProsessType.AUTOMATISK ? (
          <ForhaandsvisningBrev brev={vedtaksbrev} />
        ) : (
          <RedigerbartBrev brev={vedtaksbrev} kanRedigeres={manueltBrevKanRedigeres(status)} />
        )}
      </BrevContent>

      <Border />

      <BehandlingHandlingKnapper>
        {hentBehandlesFraStatus(status) && <SendTilAttesteringModal />}
      </BehandlingHandlingKnapper>
    </Content>
  )
}

const SpinnerContainer = styled.div`
  height: 100%;
  width: 100%;
  text-align: center;
`

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
