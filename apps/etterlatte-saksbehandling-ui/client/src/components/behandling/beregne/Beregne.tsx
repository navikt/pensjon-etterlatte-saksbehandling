import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { behandlingSkalSendeBrev, hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '~utils/formattering'
import { useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch } from '~store/Store'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import React, { useEffect, useState } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBehandlingsstatus, oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Button, Heading } from '@navikt/ds-react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
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
import { handlinger } from '~components/behandling/handlinger/typer'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'

export const Beregne = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
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
    if (!opphoer) {
      hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
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
          <Heading spacing size="large" level="1">
            Beregning og vedtak
          </Heading>
          <Vilkaarsresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
        </HeadingWrapper>
      </ContentHeader>

      {isPending(beregning) && <Spinner visible label="Henter beregning" />}
      {isFailure(beregning) && <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>}
      {isSuccess(beregning) && (
        <BeregningWrapper>
          {!opphoer &&
            (() => {
              switch (beregning.data.type) {
                case Beregningstype.BP:
                  return <BarnepensjonSammendrag behandling={behandling} beregning={beregning.data} />
                case Beregningstype.OMS:
                  return (
                    <>
                      <OmstillingsstoenadSammendrag beregning={beregning.data} />
                      <Avkorting behandling={behandling} />
                    </>
                  )
              }
            })()}
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
        </BeregningWrapper>
      )}

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
                ? handlinger.NESTE.navn
                : handlinger.FATT_VEDTAK.navn}
            </Button>
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

const EtterbetalingWrapper = styled.div`
  margin-top: 3rem;
  margin-bottom: 3rem;
  max-width: 500px;

  .text {
    margin: 1em 0 5em 0;
  }
`

const InfoAlert = styled(Alert).attrs({ variant: 'info' })`
  margin-top: 2rem;
`

const BeregningWrapper = styled.div`
  padding: 0 4em;
`
