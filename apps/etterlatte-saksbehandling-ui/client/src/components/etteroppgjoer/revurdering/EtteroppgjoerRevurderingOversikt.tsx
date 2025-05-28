import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import React, { useEffect } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { EtteroppgjoerRevurderingResultat } from '~components/etteroppgjoer/revurdering/EtteroppgjoerRevurderingResultat'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { InformasjonFraBruker } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBruker'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  // TODO: ny routing fikses i neste PR
  //const { next } = useContext(BehandlingRouteContext)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const etteroppgjoerForbehandlingId = behandling.relatertBehandlingId

  const dispatch = useAppDispatch()

  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerForbehandlingId])

  return mapResult(hentEtteroppgjoerResult, {
    pending: <Spinner label="Henter etteroppgjør..." />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente forbehandling for etteroppgjør: {error.detail}</ApiErrorAlert>,
    success: (etteroppgjoer) => (
      <VStack gap="10" paddingInline="16" paddingBlock="16 4">
        <Heading size="xlarge" level="1">
          Etteroppgjør for {etteroppgjoer.behandling.aar}
        </Heading>
        <BodyShort>
          <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
        </BodyShort>
        <Inntektsopplysninger />

        <InformasjonFraBruker behandling={behandling} />

        {!!etteroppgjoer.behandling.harMottattNyInformasjon ? (
          <FastsettFaktiskInntekt erRedigerbar={erRedigerbar} />
        ) : (
          <FastsettFaktiskInntekt erRedigerbar={false} />
        )}

        <ResultatAvForbehandling />

        <EtteroppgjoerRevurderingResultat />

        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center">
            <VStack gap="4" align="center">
              <div>
                <Button type="button" variant="primary">
                  Neste steg
                </Button>
              </div>
              <AvbrytBehandling />
            </VStack>
          </HStack>
        </Box>
      </VStack>
    ),
  })
}
