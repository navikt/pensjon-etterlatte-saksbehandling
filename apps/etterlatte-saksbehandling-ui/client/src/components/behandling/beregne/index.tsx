import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '../../../utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { useAppSelector } from '../../../store/Store'
import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const Beregne = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
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
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </div>
        </InfoWrapper>
        <Sammendrag />
      </ContentHeader>
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" onClick={next}>
            Gå videre til brev
          </Button>
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
