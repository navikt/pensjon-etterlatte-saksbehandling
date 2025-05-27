import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'

export const EndringFraBrukerVisning = () => {
  const { behandling } = useEtteroppgjoer()

  return !!behandling.harMottattNyInformasjon ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Har mottatt ny informasjon</Label>
        <BodyShort>{behandling.harMottattNyInformasjon ? 'Ja' : 'Nei'}</BodyShort>
      </VStack>
      {!!behandling.endringErTilUgunstForBruker && (
        <VStack gap="2">
          <Label>Endring er til ugunst</Label>
          <BodyShort>{behandling.endringErTilUgunstForBruker ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
      )}
      {!!behandling.beskrivelseAvUgunst && (
        <VStack gap="2" maxWidth="30rem">
          <Label>Beskrivelse av ugunst</Label>
          <BodyShort>{behandling.beskrivelseAvUgunst}</BodyShort>
        </VStack>
      )}
    </VStack>
  ) : (
    <Heading size="small">Spørsmål om endring fra bruker er ikke besvart</Heading>
  )
}
