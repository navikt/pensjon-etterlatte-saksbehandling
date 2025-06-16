import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import React, { useEffect, useState } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Alert, BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { TabellForBeregnetEtteroppgjoerResultat } from '~components/etteroppgjoer/components/resultatAvForbehandling/TabellForBeregnetEtteroppgjoerResultat'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { behandlingErRedigerbar, erFerdigBehandlet } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { InformasjonFraBruker } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBruker'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'
import { FieldErrors } from 'react-hook-form'
import { FastsettFaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { SammendragAvSkjemaFeil } from '~shared/sammendragAvSkjemaFeil/SammendragAvSkjemaFeil'
import { isEmpty } from 'lodash'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { AvsluttEtteroppgjoerRevurderingModal } from '~components/etteroppgjoer/revurdering/AvsluttEtteroppgjoerRevurderingModal'

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const { next } = useBehandlingRoutes()

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const etteroppgjoerForbehandlingId = behandling.relatertBehandlingId
  const etteroppgjoer = useEtteroppgjoer()

  const dispatch = useAppDispatch()

  const [, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const [informasjonFraBrukerSkjemaErrors, setInformasjonFraBrukerSkjemaErrors] = useState<
    FieldErrors<IInformasjonFraBruker> | undefined
  >()
  const [fastsettFaktiskInntektSkjemaErrors, setFastsettFaktiskInntektSkjemaErrors] = useState<
    FieldErrors<FastsettFaktiskInntektSkjema> | undefined
  >()

  const [oversiktValideringFeilmelding, setOversiktValideringFeilmelding] = useState<string>('')

  const nesteSteg = () => {
    if (
      (!informasjonFraBrukerSkjemaErrors || isEmpty(informasjonFraBrukerSkjemaErrors)) &&
      (!fastsettFaktiskInntektSkjemaErrors || isEmpty(fastsettFaktiskInntektSkjemaErrors))
    ) {
      if (
        etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA &&
        etteroppgjoer.behandling.kopiertFra === undefined
      ) {
        setOversiktValideringFeilmelding('Du må gjøre en endring i fastsatt inntekt')
        return
      }
      setOversiktValideringFeilmelding('')
      next()
    }
  }

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerForbehandlingId])

  return (
    !!etteroppgjoer && (
      <VStack gap="10" paddingInline="16" paddingBlock="16 4">
        <Heading size="xlarge" level="1">
          Etteroppgjør for {etteroppgjoer.behandling.aar}
        </Heading>
        <BodyShort>
          <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
        </BodyShort>
        <Inntektsopplysninger />

        <InformasjonFraBruker
          behandling={behandling}
          setInformasjonFraBrukerSkjemaErrors={setInformasjonFraBrukerSkjemaErrors}
        />

        {etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA && (
          <>
            <FastsettFaktiskInntekt
              erRedigerbar={etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA && erRedigerbar}
              setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
            />
            <TabellForBeregnetEtteroppgjoerResultat />
            <ResultatAvForbehandling />
          </>
        )}

        <Box maxWidth="42.5rem">
          <VStack gap="8">
            {/* TODO: prøve å se og merge disse 2 sammen */}
            {!!informasjonFraBrukerSkjemaErrors && <SammendragAvSkjemaFeil errors={informasjonFraBrukerSkjemaErrors} />}

            {!!fastsettFaktiskInntektSkjemaErrors && (
              <SammendragAvSkjemaFeil errors={fastsettFaktiskInntektSkjemaErrors} />
            )}

            {!!oversiktValideringFeilmelding && <Alert variant="error">{oversiktValideringFeilmelding}</Alert>}

            {etteroppgjoer.behandling.endringErTilUgunstForBruker === JaNei.JA &&
              !erFerdigBehandlet(behandling.status) && (
                <Alert variant="info">
                  <Heading spacing size="small" level="3">
                    Revurderingen skal avsluttes og det skal opprettes en ny forbehandling
                  </Heading>
                  Du har vurdert at endringen kommer til ugunst for bruker. Revurderingen skal derfor avsluttes, og en
                  ny forbehandling for etteroppgjøret skal opprettes.
                </Alert>
              )}
          </VStack>
        </Box>

        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center">
            <VStack gap="4" align="center">
              {etteroppgjoer.behandling.endringErTilUgunstForBruker === JaNei.JA ? (
                <AvsluttEtteroppgjoerRevurderingModal
                  behandling={behandling}
                  beskrivelseAvUgunst={etteroppgjoer.behandling.beskrivelseAvUgunst}
                />
              ) : (
                <div>
                  <Button type="button" onClick={nesteSteg}>
                    Neste side
                  </Button>
                </div>
              )}
              <AvbrytBehandling />
            </VStack>
          </HStack>
        </Box>
      </VStack>
    )
  )
}
