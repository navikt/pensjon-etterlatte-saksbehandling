import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { Link } from 'react-router-dom'
import { EtteroppjoerForbehandlingSteg } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { TabellForBeregnetEtteroppgjoerResultat } from '~components/etteroppgjoer/components/resultatAvForbehandling/TabellForBeregnetEtteroppgjoerResultat'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { EtteroppgjoerResultatType, kanRedigereEtteroppgjoerBehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { SammendragAvSkjemaFeil } from '~shared/sammendragAvSkjemaFeil/SammendragAvSkjemaFeil'
import React, { useState } from 'react'
import { FieldErrors } from 'react-hook-form'
import { FastsettFaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FerdigstillEtteroppgjoerUtenBrev } from '~components/etteroppgjoer/components/FerdigstillEtteroppgjoerUtenBrev'
import {
  OpphoerSkyldesDoedsfall,
  OpphoerSkyldesDoedsfallSkjema,
} from '~components/etteroppgjoer/components/opphoerSkyldesDoedsfall/OpphoerSkyldesDoedsfall'

export const EtteroppgjoerForbehandlingOversikt = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const { beregnetEtteroppgjoerResultat, behandling } = useEtteroppgjoer()

  const erRedigerbar =
    kanRedigereEtteroppgjoerBehandling(behandling.status) &&
    enhetErSkrivbar(behandling.sak.enhet, innloggetSaksbehandler.skriveEnheter)

  const [fastsettFaktiskInntektSkjemaErrors, setFastsettFaktiskInntektSkjemaErrors] = useState<
    FieldErrors<FastsettFaktiskInntektSkjema> | undefined
  >()
  const [opphoerSkyldesDoedsfallSkjemaErrors, setOpphoerSkyldesDoedsfallSkjemaErrors] = useState<
    FieldErrors<OpphoerSkyldesDoedsfallSkjema> | undefined
  >()

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {behandling.aar}
      </Heading>
      <BodyShort>
        <b>Skatteoppgjør mottatt:</b> {formaterDato(behandling.opprettet)}
      </BodyShort>
      <Inntektsopplysninger />

      {!!behandling.harVedtakAvTypeOpphoer && (
        <OpphoerSkyldesDoedsfall
          erRedigerbar={erRedigerbar}
          setOpphoerSkyldesDoedsfallSkjemaErrors={setOpphoerSkyldesDoedsfallSkjemaErrors}
        />
      )}

      <FastsettFaktiskInntekt
        erRedigerbar={erRedigerbar}
        setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
      />

      {!!beregnetEtteroppgjoerResultat && (
        <VStack gap="4">
          <TabellForBeregnetEtteroppgjoerResultat />
          <ResultatAvForbehandling />
        </VStack>
      )}

      {!!opphoerSkyldesDoedsfallSkjemaErrors && (
        <Box maxWidth="42.5rem">
          <SammendragAvSkjemaFeil errors={opphoerSkyldesDoedsfallSkjemaErrors} />
        </Box>
      )}

      {!!fastsettFaktiskInntektSkjemaErrors && (
        <Box maxWidth="42.5rem">
          <SammendragAvSkjemaFeil errors={fastsettFaktiskInntektSkjemaErrors} />
        </Box>
      )}

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
        <HStack width="100%" justify="center">
          {beregnetEtteroppgjoerResultat?.resultatType === EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING ? (
            <FerdigstillEtteroppgjoerUtenBrev />
          ) : (
            <div>
              <Button as={Link} to={`/etteroppgjoer/${behandling.id}/${EtteroppjoerForbehandlingSteg.BREV}`}>
                Gå til brev
              </Button>
            </div>
          )}
        </HStack>
      </Box>
    </VStack>
  )
}
