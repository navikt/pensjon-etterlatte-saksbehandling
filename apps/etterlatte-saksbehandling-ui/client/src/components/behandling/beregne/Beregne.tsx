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
import { Alert, Button, ErrorMessage, Heading } from '@navikt/ds-react'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import styled from 'styled-components'
import { isFailure, isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { upsertVedtak } from '~shared/api/behandling'

export const Beregne = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)
  const [vedtak, oppdaterVedtakRequest] = useApiCall(upsertVedtak)

  useEffect(() => {
    hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
  }, [])

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const vedtaksresultat = useVedtaksResultat()

  const opprettEllerOppdaterVedtak = () => {
    oppdaterVedtakRequest(behandling.id, () => next())
  }

  const soeker = behandling.søker
  const soesken = behandling.familieforhold?.avdoede.opplysning.avdoedesBarn?.filter(
    (barn) => barn.foedselsnummer !== soeker?.foedselsnummer
  )

  return (
    <Content>
      <ContentHeader>
        <Heading size={"large"} level={"1"}>Beregning og vedtak</Heading>
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
          <Sammendrag beregning={behandling.beregning!!} soeker={soeker} soesken={soesken} />
        ) : isFailure(beregning) ? (
          <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>
        ) : (
          <ApiErrorAlert>Noe uventet har skjedd</ApiErrorAlert>
        )}
      </ContentHeader>
      {behandles ? (
        <BehandlingHandlingKnapper>
          {isFailure(vedtak) && <ErrorMessage>Vedtaksoppdatering feilet</ErrorMessage>}
          <Button loading={isPending(vedtak)} variant="primary" size="medium" onClick={opprettEllerOppdaterVedtak}>
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
