/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { useAppDispatch, useAppSelector } from '~store/Store'
import React, { useContext, useEffect } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BodyShort, Box, Button, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { OmstillingsstoenadSammendrag } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import { ApiErrorAlert } from '~ErrorBoundary'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { SimulerUtbetaling } from '~components/behandling/beregne/SimulerUtbetaling'
import { useBehandling } from '~components/behandling/useBehandling'
import { Avkorting } from '~components/behandling/avkorting/Avkorting'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { upsertVedtak } from '~shared/api/vedtaksvurdering'
import { handlinger } from '~components/behandling/handlinger/typer'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'

export const BeregneEtteroppgjoerOMS = () => {
  const behandling = useBehandling()
  if (behandling == null) {
    return
  }
  const { next } = useContext(BehandlingRouteContext)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const behandlingsstatus = useAppSelector((state) => state.behandlingReducer.behandling?.status)

  const dispatch = useAppDispatch()
  const [beregningResult, hentBeregningRequest] = useApiCall(hentBeregning)
  const [vedtakResult, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const etteroppgjoerForbehandling = useEtteroppgjoerForbehandling()

  useEffect(() => {
    hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
  }, [])

  const opprettEllerOppdaterVedtak = () => {
    if (behandlingsstatus == IBehandlingStatus.BEREGNET) {
      return
    }

    oppdaterVedtakRequest(behandling.id, () => {
      next()
    })
  }

  const erAvkortet = () =>
    !(
      [
        IBehandlingStatus.OPPRETTET,
        IBehandlingStatus.VILKAARSVURDERT,
        IBehandlingStatus.TRYGDETID_OPPDATERT,
        IBehandlingStatus.BEREGNET,
      ] as IBehandlingStatus[]
    ).includes(behandling.status)

  const etteroppgjoerResultat = etteroppgjoerForbehandling?.beregnetEtteroppgjoerResultat?.resultatType
  const erEtteroppgjoerMedIngenEndring = [
    EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING,
    EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING,
  ].includes(etteroppgjoerResultat!!)

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-4 space-12">
        {erEtteroppgjoerMedIngenEndring ? (
          <>
            <BodyShort>
              Resultatet av etteroppgjøret er ingen endring, dermed vil det ikke fattes et endringsvedtak i eksisterende
              ytelse. Ytelsen vil fortsette som siste iverksatte vedtak i saken.
            </BodyShort>
          </>
        ) : (
          mapResult(beregningResult, {
            pending: <Spinner label="Henter beregning" />,
            error: (error) => <ApiErrorAlert>Kunne ikke hente beregning. {error.detail}</ApiErrorAlert>,
            success: (beregning) => (
              <>
                <VStack gap="space-8">
                  <OmstillingsstoenadSammendrag beregning={beregning} />
                  {/* TODO må vurdere om denne komponenten skal brukes eller om man skal klare å dra enkelt-deler ut */}
                  <Avkorting />
                </VStack>
                {erAvkortet() && <SimulerUtbetaling behandling={behandling} />}
              </>
            ),
          })
        )}
      </Box>

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button loading={isPending(vedtakResult)} variant="primary" onClick={opprettEllerOppdaterVedtak}>
              {handlinger.NESTE.navn}
            </Button>
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}
