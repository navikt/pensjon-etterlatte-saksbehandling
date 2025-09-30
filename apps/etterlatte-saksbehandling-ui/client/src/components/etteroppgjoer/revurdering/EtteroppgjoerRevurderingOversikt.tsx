import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import React, { useEffect, useState } from 'react'
import { IDetaljertBehandling, Opprinnelse } from '~shared/types/IDetaljertBehandling'
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

  const etteroppgjoer = useEtteroppgjoer()

  // bruk forbehandlingId fra etteroppgjør hvis tilgjengelig, ellers relatertBehandlingId
  // ny id opprettes når forbehandling kopieres ved endring av inntekt
  const etteroppgjoerForbehandlingId = etteroppgjoer?.behandling?.id ?? behandling.relatertBehandlingId

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
    const ingenFeilISkjemaer =
      (!informasjonFraBrukerSkjemaErrors || isEmpty(informasjonFraBrukerSkjemaErrors)) &&
      (!fastsettFaktiskInntektSkjemaErrors || isEmpty(fastsettFaktiskInntektSkjemaErrors))

    // Hvis revurderingen stammer fra svarfrist utløpt oppgave, som er automatisk opprettet
    if (behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB && !!etteroppgjoer.behandling.harMottattNyInformasjon) {
      if (ingenFeilISkjemaer) {
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
    } else {
      setOversiktValideringFeilmelding('Du må svare på informasjon fra bruker')
    }
  }

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId || etteroppgjoer) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerForbehandlingId, etteroppgjoer])

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

        {behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB ? (
          <>
            <InformasjonFraBruker
              behandling={behandling}
              setInformasjonFraBrukerSkjemaErrors={setInformasjonFraBrukerSkjemaErrors}
            />

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

            {etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA && (
              <>
                <FastsettFaktiskInntekt
                  erRedigerbar={etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA && erRedigerbar}
                  setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
                />
              </>
            )}
          </>
        ) : (
          <FastsettFaktiskInntekt
            erRedigerbar={erRedigerbar}
            setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
          />
        )}

        <TabellForBeregnetEtteroppgjoerResultat />
        <ResultatAvForbehandling />

        <Box maxWidth="42.5rem">
          <VStack gap="8">
            {/* TODO: prøve å se og merge disse 2 sammen */}
            {!!informasjonFraBrukerSkjemaErrors && <SammendragAvSkjemaFeil errors={informasjonFraBrukerSkjemaErrors} />}

            {!!fastsettFaktiskInntektSkjemaErrors && (
              <SammendragAvSkjemaFeil errors={fastsettFaktiskInntektSkjemaErrors} />
            )}

            {!!oversiktValideringFeilmelding && <Alert variant="error">{oversiktValideringFeilmelding}</Alert>}
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
