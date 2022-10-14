import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { BeregningModal } from '../handlinger/sendTilAttesteringModal'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import BrevModal from './brev-modal'
import { hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '../../../utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { IBehandlingsType } from '../../../store/reducers/BehandlingReducer'
import { useAppSelector } from '../../../store/Store'

export const Beregne = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const virkningstidspunkt = formaterStringDato(behandling.virkningstidspunkt)
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const vedtaksresultat = useVedtaksResultat()
  const harVedtaksbrev = behandling.behandlingType !== IBehandlingsType.REVURDERING

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
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </div>
        </InfoWrapper>
        <Sammendrag />
        {harVedtaksbrev ? <BrevModal /> : null}
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
