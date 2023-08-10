import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Alert, ErrorMessage, Heading } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
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
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'

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

  const erReadOnly = () => {
    if (vedtaksbrev.prosessType === BrevProsessType.MANUELL) {
      return false
    }
    if (!props.behandling.revurderingsaarsak) {
      return true
    }
    const aarsakerMedManueltBrev = [
      Revurderingsaarsak.OMGJOERING_AV_FARSKAP,
      Revurderingsaarsak.ADOPSJON,
      Revurderingsaarsak.FENGSELSOPPHOLD,
      Revurderingsaarsak.YRKESSKADE,
    ]
    return !aarsakerMedManueltBrev.includes(props.behandling.revurderingsaarsak)
  }

  return (
    <Content>
      <BrevContent>
        <Sidebar>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing size={'large'} level={'1'}>
                Vedtaksbrev
              </Heading>
            </HeadingWrapper>
            <Soeknadsdato mottattDato={soeknadMottattDato} />

            <br />
            {vedtaksbrev?.prosessType === BrevProsessType.MANUELL && (
              <Alert variant={'warning'}>
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
            <Spinner visible={true} label={'Henter brev ...'} />
          </SpinnerContainer>
        ) : error ? (
          <ErrorMessage>{error}</ErrorMessage>
        ) : erReadOnly() ? (
          <ForhaandsvisningBrev brev={vedtaksbrev} />
        ) : (
          <RedigerbartBrev brev={vedtaksbrev} kanRedigeres={manueltBrevKanRedigeres(status)} />
        )}
      </BrevContent>

      <BrevContentFooter>
        <BehandlingHandlingKnapper>
          {hentBehandlesFraStatus(status) && <SendTilAttesteringModal />}
        </BehandlingHandlingKnapper>
      </BrevContentFooter>
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

const BrevContentFooter = styled.div`
  border-top: 1px solid #c6c2bf;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
