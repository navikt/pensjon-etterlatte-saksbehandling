import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, Heading, Label, VStack } from '@navikt/ds-react'
import { OpplysningerFraSkatteetaten } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/inntektsopplysninger/OpplysningerFraSkatteetaten'
import { OpplysningerFraAInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/inntektsopplysninger/OpplysningerFraAInntekt'
import { BrukeroppgittInntektForInnvilgedePerioder } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/inntektsopplysninger/BrukeroppgittInntektForInnvilgedePerioder'

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
        <VStack gap="4">
          <Heading size="large" level="2">
            Inntektsopplysninger
          </Heading>
          <VStack gap="1">
            <Heading size="small">Pensjonsgivende inntekt (PGI) for 2024</Heading>
            <BodyShort>Inntekten er ekskludert omstillingsstønaden</BodyShort>
            <Label>2000 kr</Label>
          </VStack>
        </VStack>
        <OpplysningerFraSkatteetaten pensjonsgivendeInntektFraSkatteetaten={etteroppgjoer.opplysninger.skatt} />
        <OpplysningerFraAInntekt ainntekt={etteroppgjoer.opplysninger.ainntekt} />
        <BrukeroppgittInntektForInnvilgedePerioder
          avkortingGrunnlag={etteroppgjoer.opplysninger.tidligereAvkorting.avkortingGrunnlag}
        />
      </VStack>
    </Box>
  )
}
