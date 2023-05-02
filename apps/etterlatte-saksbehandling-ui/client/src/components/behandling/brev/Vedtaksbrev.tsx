import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { ErrorMessage, Heading } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { opprettEllerOppdaterBrevForVedtak } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/soeknadoversikt/Soeknadsdato'
import styled from 'styled-components'
import { SendTilAttesteringModal } from '../handlinger/sendTilAttesteringModal'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { IBehandlingsType, IDetaljertBehandling, IProsesstype } from '~shared/types/IDetaljertBehandling'
import MottakerPanel from '~components/behandling/brev/detaljer/MottakerPanel'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import Spinner from '~shared/Spinner'

export const Vedtaksbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const { sak, soeknadMottattDato, status } = props.behandling

  const [vedtaksbrev, setVedtaksbrev] = useState<any>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    const fetchVedtaksbrev = async () => {
      const skalIkkeHenteVedtaksbrev =
        props.behandling.behandlingType === IBehandlingsType.REVURDERING &&
        props.behandling.prosesstype === IProsesstype.AUTOMATISK
      if (skalIkkeHenteVedtaksbrev) {
        return
      }

      const brevResponse = await opprettEllerOppdaterBrevForVedtak(sak, behandlingId!!)
      if (brevResponse.status === 'ok') {
        setVedtaksbrev(brevResponse.data)
      } else {
        setError(brevResponse.error)
      }
      setLoading(false)
    }
    fetchVedtaksbrev()
  }, [behandlingId])

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

            {vedtaksbrev && <MottakerPanel vedtaksbrev={vedtaksbrev} />}
          </ContentHeader>
        </Sidebar>

        {loading ? (
          <SpinnerContainer>
            <Spinner visible={true} label={'Henter brev ...'} />
          </SpinnerContainer>
        ) : error ? (
          <ErrorMessage>{error}</ErrorMessage>
        ) : (
          <ForhaandsvisningBrev brev={vedtaksbrev} />
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
