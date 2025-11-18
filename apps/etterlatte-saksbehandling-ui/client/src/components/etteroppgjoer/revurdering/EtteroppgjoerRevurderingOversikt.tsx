import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import React, { useState } from 'react'
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
import Spinner from '~shared/Spinner'
import {
  OpphoerSkyldesDoedsfall,
  OpphoerSkyldesDoedsfallSkjema,
} from '~components/etteroppgjoer/components/opphoerSkyldesDoedsfall/OpphoerSkyldesDoedsfall'

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const { next } = useBehandlingRoutes()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const etteroppgjoer = useEtteroppgjoer()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [opphoerSkyldesDoedsfallSkjemaErrors, setOpphoerSkyldesDoedsfallSkjemaErrors] = useState<
    FieldErrors<OpphoerSkyldesDoedsfallSkjema> | undefined
  >()
  const [informasjonFraBrukerSkjemaErrors, setInformasjonFraBrukerSkjemaErrors] = useState<
    FieldErrors<IInformasjonFraBruker> | undefined
  >()
  const [fastsettFaktiskInntektSkjemaErrors, setFastsettFaktiskInntektSkjemaErrors] = useState<
    FieldErrors<FastsettFaktiskInntektSkjema> | undefined
  >()
  const [valideringFeilmelding, setValideringFeilmelding] = useState<string>('')

  if (!etteroppgjoer) {
    return <Spinner label="Laster etteroppgjør" />
  }

  const harIngenSkjemaErrors =
    (!opphoerSkyldesDoedsfallSkjemaErrors || isEmpty(opphoerSkyldesDoedsfallSkjemaErrors)) &&
    (!informasjonFraBrukerSkjemaErrors || isEmpty(informasjonFraBrukerSkjemaErrors)) &&
    (!fastsettFaktiskInntektSkjemaErrors || isEmpty(fastsettFaktiskInntektSkjemaErrors))

  const revurderingStammerFraSvarfristUtloept =
    behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB && !etteroppgjoer.behandling.harMottattNyInformasjon

  const erForbehandling = etteroppgjoer.behandling.kopiertFra === undefined

  const manglerFastsattInntektPaaForbehandling =
    etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA && erForbehandling

  const erAutomatiskBehandlingMedNyInformasjon =
    behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB &&
    etteroppgjoer.behandling.harMottattNyInformasjon === JaNei.JA &&
    etteroppgjoer.behandling.endringErTilUgunstForBruker !== JaNei.JA

  const kanRedigereFaktiskInntekt =
    erRedigerbar && (behandling.opprinnelse !== Opprinnelse.AUTOMATISK_JOBB || erAutomatiskBehandlingMedNyInformasjon)

  const navigerTilNesteSteg = () => {
    if (harIngenSkjemaErrors) {
      if (!!etteroppgjoer.behandling.harVedtakAvTypeOpphoer && !etteroppgjoer.behandling.opphoerSkyldesDoedsfall) {
        setValideringFeilmelding('Du må ta stilling til om opphør skyldes dødsfall')
        return
      } else if (revurderingStammerFraSvarfristUtloept) {
        setValideringFeilmelding('Du må ta stilling til informasjon fra bruker')
        return
      } else if (manglerFastsattInntektPaaForbehandling) {
        setValideringFeilmelding('Du må gjøre en endring i fastsatt inntekt')
        return
      }
      setValideringFeilmelding('')
      next()
    }
  }

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {etteroppgjoer.behandling.aar}
      </Heading>
      <BodyShort>
        <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
      </BodyShort>
      <Inntektsopplysninger />

      {!!etteroppgjoer.behandling.harVedtakAvTypeOpphoer && (
        <OpphoerSkyldesDoedsfall
          erRedigerbar={erRedigerbar}
          setOpphoerSkyldesDoedsfallSkjemaErrors={setOpphoerSkyldesDoedsfallSkjemaErrors}
        />
      )}

      {behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB && (
        <>
          <InformasjonFraBruker
            behandling={behandling}
            setInformasjonFraBrukerSkjemaErrors={setInformasjonFraBrukerSkjemaErrors}
            setValideringFeilmedling={setValideringFeilmelding}
          />

          {etteroppgjoer.behandling.endringErTilUgunstForBruker === JaNei.JA &&
            !erFerdigBehandlet(behandling.status) && (
              <Box maxWidth="42.5rem">
                <Alert variant="info">
                  <Heading spacing size="small" level="3">
                    Revurderingen skal avsluttes og det skal opprettes en ny forbehandling
                  </Heading>
                  Du har vurdert at endringen kommer til ugunst for bruker. Revurderingen skal derfor avsluttes, og en
                  ny forbehandling for etteroppgjøret skal opprettes.
                </Alert>
              </Box>
            )}
        </>
      )}

      <FastsettFaktiskInntekt
        erRedigerbar={kanRedigereFaktiskInntekt}
        setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
      />

      {etteroppgjoer.behandling.endringErTilUgunstForBruker !== JaNei.JA && (
        <>
          <TabellForBeregnetEtteroppgjoerResultat />
          <ResultatAvForbehandling />
        </>
      )}

      <Box maxWidth="42.5rem">
        <VStack gap="8">
          {/* TODO: prøve å se og merge disse 3 sammen */}
          {!!opphoerSkyldesDoedsfallSkjemaErrors && (
            <SammendragAvSkjemaFeil errors={opphoerSkyldesDoedsfallSkjemaErrors} />
          )}

          {!!informasjonFraBrukerSkjemaErrors && <SammendragAvSkjemaFeil errors={informasjonFraBrukerSkjemaErrors} />}

          {!!fastsettFaktiskInntektSkjemaErrors && (
            <SammendragAvSkjemaFeil errors={fastsettFaktiskInntektSkjemaErrors} />
          )}

          {!!valideringFeilmelding && <Alert variant="error">{valideringFeilmelding}</Alert>}
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
                <Button type="button" onClick={navigerTilNesteSteg}>
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
}
