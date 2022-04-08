import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { FileIcon } from '../../../shared/icons/fileIcon'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { BeregningModal } from '../handlinger/sendTilAttesteringModal'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'

export const Beregne = () => {
  const { virkningstidspunkt } = usePersonInfoFromBehandling()
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
            Vilkårsresultat:
            <strong>Innvilget fra {virkningstidspunkt}</strong>
          </div>
        </InfoWrapper>

        <Sammendrag />

        <VedtaksbrevWrapper>
          <FileIcon />
          <span className="text">Vis vedtaksbrev</span>
        </VedtaksbrevWrapper>
      </ContentHeader>
      <BehandlingHandlingKnapper>
        <BeregningModal />
      </BehandlingHandlingKnapper>
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
