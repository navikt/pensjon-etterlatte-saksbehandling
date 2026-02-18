import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { Alert, BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { OpplysningerFraSkatteetaten } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraSkatteetaten'
import { BrukeroppgittInntektForInnvilgedePerioder } from '~components/etteroppgjoer/components/inntektsopplysninger/BrukeroppgittInntektForInnvilgedePerioder'
import { OpplysningerFraAInntektSummert } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraAInntektSummert'
import { EtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { formaterDato } from '~utils/formatering/dato'
import React from 'react'

export const Inntektsopplysninger = ({ forbehandling }: { forbehandling: EtteroppgjoerForbehandling }) => {
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
        paddingInline="6"
        paddingBlock="4"
        background="surface-action-subtle"
        borderColor="border-action"
        borderWidth="0 0 0 4"
      >
        <VStack gap="8">
          <Heading size="large" level="2">
            Inntektsopplysninger
          </Heading>

          {opplysninger.summertPgi && (
            <OpplysningerFraSkatteetaten inntektFraSkatteetatenSummert={opplysninger.summertPgi} />
          )}

          {opplysninger.summertAInntekt && <OpplysningerFraAInntektSummert inntekter={opplysninger.summertAInntekt} />}

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
