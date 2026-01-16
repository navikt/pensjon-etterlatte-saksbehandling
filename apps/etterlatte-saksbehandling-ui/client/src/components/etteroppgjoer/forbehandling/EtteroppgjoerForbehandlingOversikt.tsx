import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { Alert, BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { Link } from 'react-router-dom'
import { EtteroppjoerForbehandlingSteg } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { TabellForBeregnetEtteroppgjoerResultat } from '~components/etteroppgjoer/components/resultatAvForbehandling/TabellForBeregnetEtteroppgjoerResultat'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import {
  EtteroppgjoerResultatType,
  kanRedigereEtteroppgjoerForbehandling,
} from '~shared/types/EtteroppgjoerForbehandling'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { SammendragAvSkjemaFeil } from '~shared/sammendragAvSkjemaFeil/SammendragAvSkjemaFeil'
import React, { useState } from 'react'
import { FieldErrors } from 'react-hook-form'
import { FastsettFaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FerdigstillEtteroppgjoerForbehandlingUtenBrev } from '~components/etteroppgjoer/components/FerdigstillEtteroppgjoerForbehandlingUtenBrev'
import {
  OpphoerSkyldesDoedsfall,
  OpphoerSkyldesDoedsfallSkjema,
} from '~components/etteroppgjoer/components/opphoerSkyldesDoedsfall/OpphoerSkyldesDoedsfall'
import { JaNei } from '~shared/types/ISvar'

export const EtteroppgjoerForbehandlingOversikt = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const { beregnetEtteroppgjoerResultat, forbehandling } = useEtteroppgjoerForbehandling()

  const erRedigerbar =
    kanRedigereEtteroppgjoerForbehandling(forbehandling.status) &&
    enhetErSkrivbar(forbehandling.sak.enhet, innloggetSaksbehandler.skriveEnheter)

  const [opphoerSkyldesDoedsfallSkjemaErrors, setOpphoerSkyldesDoedsfallSkjemaErrors] = useState<
    FieldErrors<OpphoerSkyldesDoedsfallSkjema> | undefined
  >()
  const [fastsettFaktiskInntektSkjemaErrors, setFastsettFaktiskInntektSkjemaErrors] = useState<
    FieldErrors<FastsettFaktiskInntektSkjema> | undefined
  >()

  const doedsfallIEtteroppgjoersaaret = forbehandling.opphoerSkyldesDoedsfallIEtteroppgjoersaar === JaNei.JA

  const ferdigstillUtenBrev =
    beregnetEtteroppgjoerResultat?.resultatType === EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING ||
    forbehandling.opphoerSkyldesDoedsfall === JaNei.JA

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {forbehandling.aar}
      </Heading>
      <BodyShort>
        <b>Skatteoppgjør mottatt:</b> {formaterDato(forbehandling.opprettet)}
      </BodyShort>
      <Inntektsopplysninger />

      {!!forbehandling.harVedtakAvTypeOpphoer && (
        <OpphoerSkyldesDoedsfall
          erRedigerbar={erRedigerbar}
          setOpphoerSkyldesDoedsfallSkjemaErrors={setOpphoerSkyldesDoedsfallSkjemaErrors}
        />
      )}

      {!doedsfallIEtteroppgjoersaaret && (
        <FastsettFaktiskInntekt
          erRedigerbar={erRedigerbar}
          setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
        />
      )}

      {!!beregnetEtteroppgjoerResultat && !doedsfallIEtteroppgjoersaaret && (
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

      {doedsfallIEtteroppgjoersaaret && (
        <Alert size="small" variant="info">
          Siden bruker er død i etteroppgjørsåret, skal etteroppgjøret ferdigstilles uten brev og endringer.
        </Alert>
      )}

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
        <HStack width="100%" justify="center">
          {ferdigstillUtenBrev ? (
            <FerdigstillEtteroppgjoerForbehandlingUtenBrev />
          ) : (
            <Button as={Link} to={`/etteroppgjoer/${forbehandling.id}/${EtteroppjoerForbehandlingSteg.BREV}`}>
              Gå til brev
            </Button>
          )}
        </HStack>
      </Box>
    </VStack>
  )
}
