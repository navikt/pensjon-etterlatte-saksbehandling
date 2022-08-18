import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { BeregningModal } from '../handlinger/sendTilAttesteringModal'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import BrevModal from './brev-modal'
import { hentBehandlesFraStatus } from '../felles/utils'
import { formatterStringDato } from '../../../utils/formattering'

export const Beregne = () => {
  const behandling = useContext(AppContext).state.behandlingReducer
  const virkningstidspunkt = formatterStringDato(behandling.virkningstidspunkt)
  const behandles = hentBehandlesFraStatus(behandling?.status)

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
            Vilkårsresultat: <strong>Innvilget fra {virkningstidspunkt}</strong>
          </div>
        </InfoWrapper>
        <Sammendrag />
        <BrevModal />
      </ContentHeader>
      {behandles ? (
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
  margin-top: 5em;
  max-width: 500px;
  .text {
    margin: 2em 0 5em 0;
  }
`
export const DetailWrapper = styled.div`
  display: flex;
  max-width: 400px;
  margin-left: 0;
`
