import { behandlingErRedigerbar } from '../felles/utils'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { BehandlingRouteContext } from '../BehandlingRoutes'
import React, { useContext, useEffect, useState } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Box, Button, HStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingStatus, IBehandlingsType, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import { OmstillingsstoenadSammendrag } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import { Avkorting } from '~components/behandling/avkorting/Avkorting'
import { fattVedtak, upsertVedtak } from '~shared/api/vedtaksvurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { handlinger } from '~components/behandling/handlinger/typer'

import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { SimulerUtbetaling } from '~components/behandling/beregne/SimulerUtbetaling'
import { useBehandling } from '~components/behandling/useBehandling'
import { avkortingSkalHaToInntekter } from '~shared/api/avkorting'
import { ClickEvent } from '~utils/amplitude'
import { Tilbakemelding } from '~shared/tilbakemelding/Tilbakemelding'

export const BeregneOMS = () => {
  const behandling = useBehandling()
  if (behandling == null) {
    return
  }

  const { next } = useContext(BehandlingRouteContext)
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)

  const [, avkortingSkalHaToInntekterRequest] = useApiCall(avkortingSkalHaToInntekter)
  const [skalHaInntektNesteAar, setSkalHaInntektNesteAar] = useState(false)

  const [vedtakStatus, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [visAttesteringsmodal, setVisAttesteringsmodal] = useState(false)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

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

  const erOpphoer = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  useEffect(() => {
    if (!erOpphoer) {
      hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
      avkortingSkalHaToInntekterRequest(behandling.id, (response) =>
        setSkalHaInntektNesteAar(response.skalHaInntektNesteAar)
      )
    }
  }, [])

  const erAvkortet = () =>
    !(
      [
        IBehandlingStatus.OPPRETTET,
        IBehandlingStatus.VILKAARSVURDERT,
        IBehandlingStatus.TRYGDETID_OPPDATERT,
        IBehandlingStatus.BEREGNET,
      ] as IBehandlingStatus[]
    ).includes(behandling.status)

  const opprettEllerOppdaterVedtak = () => {
    const skalSendeBrev = behandling.sendeBrev

    if (behandlingsstatus == IBehandlingStatus.BEREGNET) {
      setManglerAvkorting(true)
      return
    }
    setManglerAvkorting(false)

    if (skalSendeBrev && !brevutfallOgEtterbetaling?.brevutfall) {
      setManglerbrevutfall(true)
      return
    }
    setManglerbrevutfall(false)

    oppdaterVedtakRequest(behandling.id, () => {
      if (skalSendeBrev) {
        next()
      } else {
        setVisAttesteringsmodal(true)
      }
    })
  }

  const erUtlandForstegangsbehandling = () =>
    behandling.utlandstilknytning &&
    behandling.utlandstilknytning.type !== UtlandstilknytningType.NASJONAL &&
    behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING

  return (
    <>
      {erOpphoer ? (
        <Box paddingInline="18" paddingBlock="4">
          <SimulerUtbetaling behandling={behandling} />
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
                <>
                  <OmstillingsstoenadSammendrag beregning={beregning} />
                  <Avkorting
                    behandling={behandling}
                    resetInntektsavkortingValidering={() => setManglerAvkorting(false)}
                    skalHaInntektNesteAar={skalHaInntektNesteAar}
                  />
                </>
                {erAvkortet() && <SimulerUtbetaling behandling={behandling} />}
                {erAvkortet() && (
                  <Brevutfall behandling={behandling} resetBrevutfallvalidering={() => setManglerbrevutfall(false)} />
                )}
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
                {erUtlandForstegangsbehandling() && (
                  <HStack justify="center" paddingBlock="0 8">
                    <Tilbakemelding
                      spoersmaal="Hvordan opplevde du saksbehandlingen av denne utlandssaken i Gjenny?"
                      clickEvent={
                        behandling.erSluttbehandling
                          ? ClickEvent.TILBAKEMELDING_SAKSBEHANDLING_UTLAND_SLUTTBEHANDLING
                          : ClickEvent.TILBAKEMELDING_SAKSBEHANDLING_UTLAND_FOERSTEGANGSBEHANDLING
                      }
                      behandlingId={behandling.id}
                    />
                  </HStack>
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
