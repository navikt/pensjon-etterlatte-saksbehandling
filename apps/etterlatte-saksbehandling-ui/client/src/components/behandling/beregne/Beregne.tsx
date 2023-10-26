import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { behandlingSkalSendeBrev, hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '~utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch } from '~store/Store'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import React, { useEffect, useState } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBehandlingsstatus, oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Button, Heading } from '@navikt/ds-react'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import styled from 'styled-components'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/sendTilAttesteringModal'
import { Beregningstype } from '~shared/types/Beregning'
import { BarnepensjonSammendrag } from '~components/behandling/beregne/BarnepensjonSammendrag'
import { OmstillingsstoenadSammendrag } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import { Avkorting } from '~components/behandling/avkorting/Avkorting'
import { SakType } from '~shared/types/sak'
import Etterbetaling from '~components/behandling/beregningsgrunnlag/Etterbetaling'
import { fattVedtak, upsertVedtak } from '~shared/api/vedtaksvurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'

export const Beregne = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const beregningFraState = behandling.beregning
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)
  const [vedtak, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [visAttesteringsmodal, setVisAttesteringsmodal] = useState(false)

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const behandles = hentBehandlesFraStatus(behandling.status)
  const opphoer = behandling.vilkårsprøving?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  const vedtaksresultat =
    behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER ? useVedtaksResultat() : 'opphoer'

  useEffect(() => {
    const hentBeregning = async () => {
      if (!opphoer) {
        hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
      }
    }

    if (!beregningFraState && behandling.vilkårsprøving) {
      void hentBeregning()
    }
  }, [])

  const opprettEllerOppdaterVedtak = () => {
    oppdaterVedtakRequest(behandling.id, () => {
      const nyStatus =
        behandling.sakType === SakType.BARNEPENSJON ? IBehandlingStatus.BEREGNET : IBehandlingStatus.AVKORTET
      dispatch(oppdaterBehandlingsstatus(nyStatus))
      if (behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak)) {
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
        {!beregningFraState && !opphoer && !isFailure(beregning) && <Spinner visible label="Laster" />}
        {isFailure(beregning) && <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>}
        {beregningFraState &&
          !opphoer &&
          {
            [Beregningstype.BP]: <BarnepensjonSammendrag behandling={behandling} beregning={beregningFraState} />,
            [Beregningstype.OMS]: (
              <>
                <OmstillingsstoenadSammendrag beregning={beregningFraState} />
                <Avkorting behandling={behandling} />
              </>
            ),
          }[beregningFraState.type]}
        {behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak) ? null : (
          <InfoAlert variant="info" inline>
            Det sendes ikke vedtaksbrev for denne behandlingen.
          </InfoAlert>
        )}
        {!opphoer && (
          <EtterbetalingWrapper>
            <Etterbetaling
              behandlingId={behandling.id}
              lagraEtterbetaling={behandling.etterbetaling}
              redigerbar={behandles}
              virkningstidspunkt={virkningstidspunkt}
            />
          </EtterbetalingWrapper>
        )}
      </ContentHeader>

      <Border />

      {isFailure(vedtak) && (
        <FlexRow justify="center">
          <ApiErrorAlert>{vedtak.error.detail || 'Vedtaksoppdatering feilet'}</ApiErrorAlert>
        </FlexRow>
      )}

      {behandles ? (
        <BehandlingHandlingKnapper>
          {visAttesteringsmodal ? (
            <SendTilAttesteringModal behandlingId={behandling.id} fattVedtakApi={fattVedtak} />
          ) : (
            <Button loading={isPending(vedtak)} variant="primary" onClick={opprettEllerOppdaterVedtak}>
              {behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak)
                ? 'Gå videre til brev'
                : 'Fatt vedtak'}
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

const InfoAlert = styled(Alert).attrs({ variant: 'info' })`
  margin-top: 2rem;
`
