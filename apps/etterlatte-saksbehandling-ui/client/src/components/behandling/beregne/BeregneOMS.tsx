/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

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
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
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

export const BeregneOMS = () => {
  const behandling = useBehandling()
  if (behandling == null) {
    return
  }

  const { next } = useContext(BehandlingRouteContext)
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)

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

  const [manglerBrevutfall, setManglerbrevutfall] = useState(false)

  const erOpphoer = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT

  useEffect(() => {
    if (!erOpphoer) {
      hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
    }
  }, [])

  const erAvkortet = !(
    [
      IBehandlingStatus.OPPRETTET,
      IBehandlingStatus.VILKAARSVURDERT,
      IBehandlingStatus.TRYGDETID_OPPDATERT,
      IBehandlingStatus.BEREGNET,
    ] as IBehandlingStatus[]
  ).includes(behandling.status)

  const opprettEllerOppdaterVedtak = () => {
    const skalSendeBrev = behandling.sendeBrev
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

  return (
    <>
      {erOpphoer ? (
        <Box paddingInline="space-16" paddingBlock="space-4">
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
              <Box paddingInline="space-16" paddingBlock="space-4">
                <>
                  <OmstillingsstoenadSammendrag beregning={beregning} />
                  <Avkorting />
                </>
                {erAvkortet && <SimulerUtbetaling behandling={behandling} />}
                {erAvkortet && (
                  <Brevutfall behandling={behandling} resetBrevutfallvalidering={() => setManglerbrevutfall(false)} />
                )}
                {manglerBrevutfall && (
                  <Alert style={{ maxWidth: '16em' }} variant="error">
                    Du må fylle ut utfall i brev
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

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
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
