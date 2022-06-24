import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { format } from 'date-fns'
import { FileIcon } from '../../../shared/icons/fileIcon'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { BeregningModal } from '../handlinger/sendTilAttesteringModal'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { IBehandlingStatus } from '../../../store/reducers/BehandlingReducer'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'

export const Beregne = () => {
  const virkningstidspunkt = useContext(AppContext).state.behandlingReducer.virkningstidspunkt
  const behandlingStatus = useContext(AppContext).state.behandlingReducer.status

  return (
    <Content>
      <ContentHeader>
        <h1>Beregning og vedtak</h1>
        <InfoWrapper>
          <DetailWrapper>
            <TypeStatusWrap type="barn">Barnepensjon</TypeStatusWrap>
            <TypeStatusWrap type="statsborgerskap">Nasjonal sak</TypeStatusWrap>
          </DetailWrapper>

          <div className="text">
            Vilkårsresultat: <strong>Innvilget fra {format(new Date(virkningstidspunkt), 'dd.MM.yyyy')}</strong>
          </div>
        </InfoWrapper>

        <Sammendrag />

        <VedtaksbrevWrapper>
          <FileIcon />
          <span className="text">Vis vedtaksbrev</span>
        </VedtaksbrevWrapper>
      </ContentHeader>
      {behandlingStatus === IBehandlingStatus.UNDER_BEHANDLING ||
      behandlingStatus === IBehandlingStatus.GYLDIG_SOEKNAD ? (
        <BehandlingHandlingKnapper>
          <BeregningModal />
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

export const InfoWrapper = styled.div`
  margin-top 5em;
  max-width: 500px;
  .text {
    margin: 2em 0em 5em 0em;
  }
`
export const DetailWrapper = styled.div`
  display: flex;
  max-width: 400px;
  margin-left: 0;
`
export const VedtaksbrevWrapper = styled.div`
  display: inline-flex;
  margin-top: 8em;
  cursor: pointer;

  .text {
    line-height: 1.5em;
    margin-left: 0.3em;
    color: #0056b4;
    font-size: 18px;
    font-weight: 600;
    text-decoration-line: underline;
  }
`
