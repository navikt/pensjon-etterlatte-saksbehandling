import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Box, Heading, VStack } from '@navikt/ds-react'
import { OpplysningerFraSkatteetaten } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraSkatteetaten'
import { OpplysningerFraAInntekt } from '~components/etteroppgjoer/components/inntektsopplysninger/OpplysningerFraAInntekt'
import { BrukeroppgittInntektForInnvilgedePerioder } from '~components/etteroppgjoer/components/inntektsopplysninger/BrukeroppgittInntektForInnvilgedePerioder'

export const Inntektsopplysninger = () => {
  const etteroppgjoer = useEtteroppgjoer()

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
        <OpplysningerFraSkatteetaten pensjonsgivendeInntektFraSkatteetaten={etteroppgjoer.opplysninger.skatt} />
        <OpplysningerFraAInntekt ainntekt={etteroppgjoer.opplysninger.ainntekt} />
        <BrukeroppgittInntektForInnvilgedePerioder
          avkortingGrunnlag={etteroppgjoer.opplysninger.tidligereAvkorting.avkortingGrunnlag}
        />
      </VStack>
    </Box>
  )
}
