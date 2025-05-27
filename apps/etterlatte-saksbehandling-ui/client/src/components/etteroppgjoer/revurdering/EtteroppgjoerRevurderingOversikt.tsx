import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import React, { useContext, useEffect, useState } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Alert, BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { EtteroppgjoerRevurderingResultat } from '~components/etteroppgjoer/revurdering/EtteroppgjoerRevurderingResultat'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { EndringFraBruker } from '~components/etteroppgjoer/revurdering/endringFraBruker/EndringFraBruker'

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const { next } = useContext(BehandlingRouteContext)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const etteroppgjoerForbehandlingId = behandling.relatertBehandlingId

  const dispatch = useAppDispatch()

  const etteroppgjoer = useEtteroppgjoer()

  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const [fastsettInntektSkjemaErSkittent, setFastsettInntektSkjemaErSkittent] = useState<boolean>(false)
  const [fastsettInntektSkjemaErSkittentFeilmelding, setFastsettInntektSkjemaErSkittentFeilmelding] =
    useState<string>('')

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerForbehandlingId])

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {etteroppgjoer.behandling.aar}
      </Heading>
      <BodyShort>
        <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
      </BodyShort>
      <Inntektsopplysninger />

      <EndringFraBruker behandling={behandling} />

      {!!etteroppgjoer.behandling.harMottattNyInformasjon ? (
        <FastsettFaktiskInntekt
          erRedigerbar={erRedigerbar}
          setFastsettInntektSkjemaErSkittent={setFastsettInntektSkjemaErSkittent}
        />
      ) : (
        <FastsettFaktiskInntekt erRedigerbar={false} />
      )}

      <ResultatAvForbehandling />

      <EtteroppgjoerRevurderingResultat />

      {fastsettInntektSkjemaErSkittentFeilmelding && (
        <HStack width="100%" justify="center">
          <Alert variant="error">{fastsettInntektSkjemaErSkittentFeilmelding}</Alert>
        </HStack>
      )}

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
  )
}
