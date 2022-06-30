import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { BeregningModal } from '../handlinger/sendTilAttesteringModal'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { IBehandlingStatus } from '../../../store/reducers/BehandlingReducer'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import BrevModal from "./brev-modal";

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

        <BrevModal />
      </ContentHeader>
      {behandlingStatus === IBehandlingStatus.UNDER_BEHANDLING ||
      behandlingStatus === IBehandlingStatus.GYLDIG_SOEKNAD || IBehandlingStatus.RETURNERT ? (
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
