import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { Link } from 'react-router-dom'
import { EtteroppjoerForbehandlingSteg } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { BrevutfallAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/BrevutfallAvForbehandling'

export const EtteroppgjoerForbehandlingOversikt = () => {
  const etteroppgjoer = useEtteroppgjoer()

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {etteroppgjoer.behandling.aar}
      </Heading>
      <BodyShort>
        <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
      </BodyShort>
      <Inntektsopplysninger />
      <FastsettFaktiskInntekt />

      {!!etteroppgjoer.beregnetEtteroppgjoerResultat && (
        <VStack gap="4">
          <ResultatAvForbehandling />
          <BrevutfallAvForbehandling />
        </VStack>
      )}

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
        <HStack width="100%" justify="center">
          <div>
            <Button
              as={Link}
              to={`/etteroppgjoer/${etteroppgjoer.behandling.id}/${EtteroppjoerForbehandlingSteg.BREV}`}
            >
              Gå til brev
            </Button>
          </div>
        </HStack>
      </Box>
    </VStack>
  )
}
