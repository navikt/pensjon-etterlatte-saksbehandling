import { Content, ContentHeader } from '~shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '~utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useEffect } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { Sammendrag } from './Sammendrag'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Button } from '@navikt/ds-react'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import styled from 'styled-components'
import { isFailure, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'

export const Beregne = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)

  useEffect(() => {
    hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
  }, [])

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
        {isPendingOrInitial(beregning) ? (
          <Spinner visible label="Laster" />
        ) : isSuccess(beregning) ? (
          <Sammendrag beregning={behandling.beregning!!} soeker={behandling.søker} />
        ) : isFailure(beregning) ? (
          <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>
        ) : (
          <ApiErrorAlert>Noe uventet har skjedd</ApiErrorAlert>
        )}
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

const InfoWrapper = styled.div`
  margin-top: 5em;
  max-width: 500px;
  .text {
    margin: 2em 0 5em 0;
  }
`
const DetailWrapper = styled.div`
  display: flex;
  max-width: 400px;
  margin-left: 0;
`
const ApiErrorAlert = styled(Alert).attrs({ variant: 'error' })`
  margin-top: 8px;
`
