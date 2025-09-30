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
import { EtteroppgjoerOversiktSkjemaer } from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'
import { FormProvider, useForm } from 'react-hook-form'
import { isEmpty } from 'lodash'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { AvsluttEtteroppgjoerRevurderingModal } from '~components/etteroppgjoer/revurdering/AvsluttEtteroppgjoerRevurderingModal'

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const etteroppgjoer = useEtteroppgjoer()
  // const { next } = useBehandlingRoutes()

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  // bruk forbehandlingId fra etteroppgjør hvis tilgjengelig, ellers relatertBehandlingId
  // ny id opprettes når forbehandling kopieres ved endring av inntekt
  const etteroppgjoerForbehandlingId = etteroppgjoer?.behandling?.id ?? behandling.relatertBehandlingId

  const dispatch = useAppDispatch()
  const [, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const methods = useForm<EtteroppgjoerOversiktSkjemaer>({
    shouldUnregister: true,
  })

  const [oversiktValideringFeilmelding, setOversiktValideringFeilmelding] = useState<string>('')

  const nesteSteg = () => {
    setOversiktValideringFeilmelding('')

    methods.trigger()

    const ingenFeil = isEmpty(methods.formState.errors.informasjonFraBruker)

    console.log(ingenFeil)

    // // Hvis revurderingen stammer fra svarfrist utløpt oppgave, som er automatisk opprettet
    // if (behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB && !!etteroppgjoer.behandling.harMottattNyInformasjon) {
    //   if (ingenFeilISkjemaer) {
    //     if (
    //       etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA &&
    //       etteroppgjoer.behandling.kopiertFra === undefined
    //     ) {
    //       setOversiktValideringFeilmelding('Du må gjøre en endring i fastsatt inntekt')
    //       return
    //     }
    //     setOversiktValideringFeilmelding('')
    //     next()
    //   }
    // } else {
    //   setOversiktValideringFeilmelding('Du må svare på informasjon fra bruker')
    // }
  }

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId || etteroppgjoer) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
      methods.setValue('informasjonFraBruker', {
        harMottattNyInformasjon: etteroppgjoer?.behandling.harMottattNyInformasjon,
        endringErTilUgunstForBruker: etteroppgjoer?.behandling.endringErTilUgunstForBruker,
        beskrivelseAvUgunst: etteroppgjoer?.behandling.beskrivelseAvUgunst,
      })
      methods.setValue(
        'faktiskInntekt',
        etteroppgjoer.faktiskInntekt
          ? {
              loennsinntekt: new Intl.NumberFormat('nb').format(etteroppgjoer.faktiskInntekt.loennsinntekt),
              afp: new Intl.NumberFormat('nb').format(etteroppgjoer.faktiskInntekt.afp),
              naeringsinntekt: new Intl.NumberFormat('nb').format(etteroppgjoer.faktiskInntekt.naeringsinntekt),
              utlandsinntekt: new Intl.NumberFormat('nb').format(etteroppgjoer.faktiskInntekt.utlandsinntekt),
              spesifikasjon: etteroppgjoer.faktiskInntekt.spesifikasjon,
            }
          : {
              loennsinntekt: '0',
              afp: '0',
              naeringsinntekt: '0',
              utlandsinntekt: '0',
              spesifikasjon: '',
            }
      )
    })
  }, [etteroppgjoerForbehandlingId, etteroppgjoer])

  return (
    !!etteroppgjoer && (
      <FormProvider {...methods}>
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
              <InformasjonFraBruker behandling={behandling} />

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
                  />
                </>
              )}
            </>
          ) : (
            <FastsettFaktiskInntekt erRedigerbar={erRedigerbar} />
          )}

          <TabellForBeregnetEtteroppgjoerResultat />
          <ResultatAvForbehandling />

          <Box maxWidth="42.5rem">
            <VStack gap="8">
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
      </FormProvider>
    )
  )
}
