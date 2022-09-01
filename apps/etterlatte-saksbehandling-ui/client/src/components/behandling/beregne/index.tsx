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
import { formaterStringDato } from '../../../utils/formattering'
import { useVedtaksResultat, VedtakResultat } from '../useVedtaksResultat'

const tekstForVedtaksresultat = (vedtaksresultat: VedtakResultat, formatertVirkningstidspunkt: string): string => {
  switch (vedtaksresultat) {
    case 'avslag':
      return 'Avslag'
    case 'endring':
      return `Endring fra ${formatertVirkningstidspunkt}`
    case 'innvilget':
      return `Innvilget fra ${formatertVirkningstidspunkt}`
    case 'opphoer':
      return `Opphør fra ${formatertVirkningstidspunkt}`
    case 'uavklart':
      return `Ikke avklart`
  }
}

export const Beregne = () => {
  const behandling = useContext(AppContext).state.behandlingReducer
  const virkningstidspunkt = formaterStringDato(behandling.virkningstidspunkt)
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const vedtaksresultat = useVedtaksResultat()

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
            Vilkårsresultat: <strong>{tekstForVedtaksresultat(vedtaksresultat, virkningstidspunkt)}</strong>
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
