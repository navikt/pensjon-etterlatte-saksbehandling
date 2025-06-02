import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import React, { useEffect, useState } from 'react'
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
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'
import { FieldErrors } from 'react-hook-form'
import { FastsettFaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { SammendragAvSkjemaFeil } from '~shared/sammendragAvSkjemaFeil/SammendragAvSkjemaFeil'

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

  function nesteSteg() {
    // if (!!feilmeldingEtteroppgjoer) {
    //   validerSkjema()
    //   if (feilmeldingEtteroppgjoer !== EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST) {
    //     document.getElementById(INFORMASJON_FRA_BRUKER_ID)?.scrollIntoView({ block: 'center', behavior: 'smooth' })
    //   }
    //   setVisFeilmelding(true)
    //   return
    // }
    next()
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
            <ResultatAvForbehandling />
            <EtteroppgjoerRevurderingResultat />
          </>
        )}

        {!!informasjonFraBrukerSkjemaErrors && (
          <Box maxWidth="42.5rem">
            <SammendragAvSkjemaFeil errors={informasjonFraBrukerSkjemaErrors} />
          </Box>
        )}

        {!!fastsettFaktiskInntektSkjemaErrors && (
          <Box maxWidth="42.5rem">
            <SammendragAvSkjemaFeil errors={fastsettFaktiskInntektSkjemaErrors} />
          </Box>
        )}

        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center">
            <VStack gap="4" align="center">
              <div>
                <Button type="button" onClick={nesteSteg} variant="primary">
                  Neste side
                </Button>
              </div>
              <AvbrytBehandling />
            </VStack>
          </HStack>
        </Box>
      </VStack>
    )
  )
}
