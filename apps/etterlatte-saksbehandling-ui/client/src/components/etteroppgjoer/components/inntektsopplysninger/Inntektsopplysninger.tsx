import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { Alert, BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { OpplysningerFraSkatteetaten } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraSkatteetaten'
import { BrukeroppgittInntektForInnvilgedePerioder } from '~components/etteroppgjoer/components/inntektsopplysninger/BrukeroppgittInntektForInnvilgedePerioder'
import { OpplysningerFraAInntektSummert } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraAInntektSummert'
import { EtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { formaterDato } from '~utils/formatering/dato'
import React from 'react'
import { OppdaterInntektsopplysninger } from './OppdaterInntektsopplysninger'

interface Props {
  forbehandling: EtteroppgjoerForbehandling
  erRedigerbar: boolean
}

export const Inntektsopplysninger = ({ forbehandling, erRedigerbar }: Props) => {
  const { opplysninger } = useEtteroppgjoerForbehandling()

  const mottattSkatteOppgjoer = forbehandling.mottattSkatteoppgjoer

  return (
    <>
      <Box maxWidth="60rem">
        {mottattSkatteOppgjoer ? (
          <BodyShort>
            <b>Skatteoppgjør mottatt:</b> {formaterDato(forbehandling.opprettet)}
          </BodyShort>
        ) : (
          <BodyShort>
            <b>Ikke mottatt skatteoppgjoer:</b> Bruker har ikke skatteoppgjør for etteroppgjørsåret, dette kan være
            fordi de ikke er kildeskattepliktig eller ikke har registrert kildeskatt i tide, eller de ikke hadde noen
            utbetalinger i etteroppgjørsåret.
          </BodyShort>
        )}
      </Box>
      <Box
        paddingInline="space-24"
        paddingBlock="space-16"
        background="accent-soft"
        borderColor="accent"
        borderWidth="0 0 0 4"
      >
        <VStack gap="space-32">
          <Heading size="large" level="2">
            Inntektsopplysninger
          </Heading>

          <OppdaterInntektsopplysninger forbehandling={forbehandling} erRedigerbar={erRedigerbar} />

          {opplysninger.summertPgi && (
            <OpplysningerFraSkatteetaten inntektFraSkatteetatenSummert={opplysninger.summertPgi} />
          )}

          {opplysninger.summertAInntekt && (
            <OpplysningerFraAInntektSummert
              inntekter={opplysninger.summertAInntekt}
              avkorting={opplysninger.tidligereAvkorting}
            />
          )}

          {opplysninger.tidligereAvkorting ? (
            <BrukeroppgittInntektForInnvilgedePerioder
              avkortingGrunnlag={opplysninger.tidligereAvkorting.avkortingGrunnlag}
            />
          ) : (
            <Alert variant="error">Kunne ikke hente eksisterende avkorting i saken.</Alert>
          )}
        </VStack>
      </Box>
    </>
  )
}
