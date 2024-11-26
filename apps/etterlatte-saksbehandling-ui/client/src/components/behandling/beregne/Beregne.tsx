import { behandlingErRedigerbar } from '../felles/utils'
import { formaterDato } from '~utils/formatering/dato'
import { useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { BehandlingRouteContext } from '../BehandlingRoutes'
import React, { useContext, useEffect, useState } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBehandlingsstatus, oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Box, Button, Heading, HStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingStatus, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import { Beregningstype } from '~shared/types/Beregning'
import { BarnepensjonSammendrag } from '~components/behandling/beregne/BarnepensjonSammendrag'
import { OmstillingsstoenadSammendrag } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import { Avkorting } from '~components/behandling/avkorting/Avkorting'
import { SakType } from '~shared/types/sak'
import { fattVedtak, upsertVedtak } from '~shared/api/vedtaksvurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { handlinger } from '~components/behandling/handlinger/typer'
import { Vedtaksresultat } from '~components/behandling/felles/Vedtaksresultat'

import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { SimulerUtbetaling } from '~components/behandling/beregne/SimulerUtbetaling'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

export const Beregne = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useContext(BehandlingRouteContext)
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)
  const [vedtakStatus, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [visAttesteringsmodal, setVisAttesteringsmodal] = useState(false)
  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterDato(behandling.virkningstidspunkt.dato)
    : undefined
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const vedtaksresultat = useVedtaksResultat()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const brevutfallOgEtterbetaling = useAppSelector(
    (state) => state.behandlingReducer.behandling?.brevutfallOgEtterbetaling
  )

  const behandlingsstatus = useAppSelector((state) => state.behandlingReducer.behandling?.status)

  const [manglerBrevutfall, setManglerbrevutfall] = useState(false)
  const [manglerAvkorting, setManglerAvkorting] = useState(false)

  const skalHaInntektNesteAar = useFeaturetoggle(FeatureToggle.validere_aarsintnekt_neste_aar)

  const erOpphoer = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  useEffect(() => {
    if (!erOpphoer) {
      hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
    }
  }, [])

  const opprettEllerOppdaterVedtak = () => {
    const erBarnepensjon = behandling.sakType === SakType.BARNEPENSJON
    const skalSendeBrev = behandling.sendeBrev

    if (!erBarnepensjon && behandlingsstatus == IBehandlingStatus.BEREGNET) {
      setManglerAvkorting(true)
      return
    } else {
      setManglerAvkorting(false)
    }

    if (skalSendeBrev && !brevutfallOgEtterbetaling?.brevutfall) {
      setManglerbrevutfall(true)
      return
    }
    setManglerbrevutfall(false)

    if (behandling.kilde === Vedtaksloesning.GJENOPPRETTA) {
      oppdaterStatus(erBarnepensjon, skalSendeBrev)
    } else {
      oppdaterVedtakRequest(behandling.id, () => {
        oppdaterStatus(erBarnepensjon, skalSendeBrev)
      })
    }
  }

  const oppdaterStatus = (erBarnepensjon: boolean, skalSendeBrev: boolean) => {
    const nyStatus = erBarnepensjon ? IBehandlingStatus.BEREGNET : IBehandlingStatus.AVKORTET
    dispatch(oppdaterBehandlingsstatus(nyStatus))
    if (skalSendeBrev) {
      next()
    } else {
      setVisAttesteringsmodal(true)
    }
  }

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Beregning og vedtak
        </Heading>
        <Vedtaksresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
      </Box>
      {erOpphoer ? (
        <Box paddingInline="18" paddingBlock="4">
          <Brevutfall behandling={behandling} resetBrevutfallvalidering={() => setManglerbrevutfall(false)} />
        </Box>
      ) : (
        <>
          {mapApiResult(
            beregning,
            <Spinner label="Henter beregning" />,
            () => (
              <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>
            ),
            (beregning) => (
              <Box paddingInline="18" paddingBlock="4">
                {(() => {
                  switch (beregning.type) {
                    case Beregningstype.BP:
                      return (
                        <>
                          <BarnepensjonSammendrag beregning={beregning} />
                          <SimulerUtbetaling behandling={behandling} />
                          <Brevutfall
                            behandling={behandling}
                            resetBrevutfallvalidering={() => setManglerbrevutfall(false)}
                          />
                        </>
                      )
                    case Beregningstype.OMS:
                      return (
                        <>
                          <OmstillingsstoenadSammendrag beregning={beregning} />
                          <Avkorting
                            behandling={behandling}
                            resetBrevutfallvalidering={() => setManglerbrevutfall(false)}
                            resetInntektsavkortingValidering={() => setManglerAvkorting(false)}
                          />
                        </>
                      )
                  }
                })()}
                {manglerBrevutfall && (
                  <Alert style={{ maxWidth: '16em' }} variant="error">
                    Du må fylle ut utfall i brev
                  </Alert>
                )}
                {manglerAvkorting && (
                  <Alert style={{ maxWidth: '16em' }} variant="error">
                    {skalHaInntektNesteAar
                      ? 'Du må legge til inntektsavkorting for inneværende og neste år, også når etterlatte ikke har inntekt. Legg da inn 0 i inntektsfeltene.'
                      : 'Du må legge til inntektsavkorting, også når etterlatte ikke har inntekt. Legg da inn 0 i inntektsfeltene.'}
                  </Alert>
                )}
              </Box>
            )
          )}
        </>
      )}

      {isFailureHandler({
        apiResult: vedtakStatus,
        errorMessage: 'Vedtaksoppdatering feilet',
        wrapperComponent: { component: HStack, props: { justify: 'center' } },
      })}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            {visAttesteringsmodal ? (
              <SendTilAttesteringModal
                behandlingId={behandling.id}
                fattVedtakApi={fattVedtak}
                validerKanSendeTilAttestering={() => true}
              />
            ) : (
              <Button loading={isPending(vedtakStatus)} variant="primary" onClick={opprettEllerOppdaterVedtak}>
                {behandling.sendeBrev ? handlinger.NESTE.navn : handlinger.FATT_VEDTAK.navn}
              </Button>
            )}
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}
