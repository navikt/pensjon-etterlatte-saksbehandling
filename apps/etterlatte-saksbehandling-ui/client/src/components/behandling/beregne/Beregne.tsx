import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { behandlingSkalSendeBrev, hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '~utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch } from '~store/Store'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useEffect, useState } from 'react'
import { hentBeregning, opprettBeregningForOpphoer } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBehandlingsstatus, oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Button, ErrorMessage, Heading } from '@navikt/ds-react'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { fattVedtak, upsertVedtak } from '~shared/api/behandling'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import styled from 'styled-components'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/sendTilAttesteringModal'
import { Beregningstype } from '~shared/types/Beregning'
import { BarnepensjonSammendrag } from '~components/behandling/beregne/BarnepensjonSammendrag'
import { OmstillingsstoenadSammendrag } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import { Avkorting } from '~components/behandling/avkorting/Avkorting'
import { SakType } from '~shared/types/sak'
import { erOpphoer } from '~shared/types/Revurderingsaarsak'
import Etterbetaling from '~components/behandling/beregningsgrunnlag/Etterbetaling'

export const Beregne = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const beregningFraState = behandling.beregning
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)
  const [vedtak, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [visAttesteringsmodal, setVisAttesteringsmodal] = useState(false)
  const [, opprettForOpphoer] = useApiCall(opprettBeregningForOpphoer)

  useEffect(() => {
    const hentBeregning = async () => {
      if (behandling.behandlingType === IBehandlingsType.REVURDERING) {
        await opprettForOpphoer(behandling.id)
      }
      hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
    }

    if (!beregningFraState) {
      void hentBeregning()
    }
  }, [])

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const behandles = hentBehandlesFraStatus(behandling.status)
  const opphoer = behandling?.revurderingsaarsak && erOpphoer(behandling.revurderingsaarsak)
  const vedtaksresultat =
    behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER ? useVedtaksResultat() : 'opphoer'

  const opprettEllerOppdaterVedtak = () => {
    oppdaterVedtakRequest(behandling.id, () => {
      const nyStatus =
        behandling.sakType === SakType.BARNEPENSJON ? IBehandlingStatus.BEREGNET : IBehandlingStatus.AVKORTET
      dispatch(oppdaterBehandlingsstatus(nyStatus))
      if (behandlingSkalSendeBrev(behandling)) {
        next()
      } else {
        setVisAttesteringsmodal(true)
      }
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading size="large" level="1">
            Beregning og vedtak
          </Heading>
        </HeadingWrapper>
        <InfoWrapper>
          <div className="text">
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </div>
        </InfoWrapper>
        {!beregningFraState && !isFailure(beregning) && <Spinner visible label="Laster" />}
        {isFailure(beregning) && <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>}
        {beregningFraState &&
          !opphoer &&
          {
            [Beregningstype.BP]: <BarnepensjonSammendrag behandling={behandling} beregning={beregningFraState} />,
            [Beregningstype.OMS]: (
              <>
                <OmstillingsstoenadSammendrag beregning={beregningFraState} />
                {!opphoer && <Avkorting behandling={behandling} />}
              </>
            ),
          }[beregningFraState.type]}
        {behandlingSkalSendeBrev(behandling) ? null : (
          <InfoAlert variant="info" inline>
            Det sendes ikke vedtaksbrev for denne behandlingen.
          </InfoAlert>
        )}
        <EtterbetalingWrapper>
          <Etterbetaling
            behandlingId={behandling.id}
            lagraEtterbetaling={behandling.etterbetaling}
            redigerbar={behandles}
            virkningstidspunkt={virkningstidspunkt}
          />
        </EtterbetalingWrapper>
      </ContentHeader>
      {behandles ? (
        <BehandlingHandlingKnapper>
          {isFailure(vedtak) && <ErrorMessage>Vedtaksoppdatering feilet</ErrorMessage>}
          {visAttesteringsmodal ? (
            <SendTilAttesteringModal behandlingId={behandling.id} fattVedtakApi={fattVedtak} />
          ) : (
            <Button loading={isPending(vedtak)} variant="primary" onClick={opprettEllerOppdaterVedtak}>
              {behandlingSkalSendeBrev(behandling) ? 'Gå videre til brev' : 'Fatt vedtak'}
            </Button>
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

const InfoWrapper = styled.div`
  margin-top: 1em;
  max-width: 500px;

  .text {
    margin: 1em 0 5em 0;
  }
`
const EtterbetalingWrapper = styled.div`
  margin-top: 3rem;
  max-width: 500px;

  .text {
    margin: 1em 0 5em 0;
  }
`

const ApiErrorAlert = styled(Alert).attrs({ variant: 'error' })`
  margin-top: 8px;
`

const InfoAlert = styled(Alert).attrs({ variant: 'info' })`
  margin-top: 2rem;
`
