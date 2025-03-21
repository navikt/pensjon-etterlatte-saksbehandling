import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { OpplysningerFraSkatteetaten } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/inntektsopplysninger/OpplysningerFraSkatteetaten'

export const Inntektsopplysninger = () => {
  const etteroppgjoer = useEtteroppgjoer()

  return (
    <Box
      paddingInline="6"
      paddingBlock="4"
      background="surface-action-subtle"
      borderColor="border-action"
      borderWidth="0 0 0 4"
      maxWidth="80rem"
    >
      <VStack gap="8">
        <VStack gap="4">
          <Heading size="large" level="2">
            Inntektsopplysninger
          </Heading>
          <BodyShort>
            {/* TODO: legg inn faktisk data her */}
            <b>Pensjonsgivende inntekt for {etteroppgjoer.behandling.aar}:</b> 2000 kr
          </BodyShort>
        </VStack>
        <OpplysningerFraSkatteetaten pensjonsgivendeInntektFraSkatteetaten={etteroppgjoer.opplysninger.skatt} />
      </VStack>
    </Box>
  )
}
