import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { Alert, Box, Heading, VStack } from '@navikt/ds-react'
import { OpplysningerFraSkatteetaten } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraSkatteetaten'
import { BrukeroppgittInntektForInnvilgedePerioder } from '~components/etteroppgjoer/components/inntektsopplysninger/BrukeroppgittInntektForInnvilgedePerioder'
import { OpplysningerFraAInntektSummert } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraAInntektSummert'

export const Inntektsopplysninger = () => {
  const { opplysninger } = useEtteroppgjoerForbehandling()

  return (
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
        <OpplysningerFraSkatteetaten inntektFraSkatteetatenSummert={opplysninger.skatt} />

        {opplysninger.summerteInntekter && (
          <OpplysningerFraAInntektSummert inntekter={opplysninger.summerteInntekter} />
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
  )
}
