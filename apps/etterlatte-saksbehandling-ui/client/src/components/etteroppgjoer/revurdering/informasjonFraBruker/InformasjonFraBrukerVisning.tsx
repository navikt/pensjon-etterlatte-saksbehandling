import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { JaNei } from '~shared/types/ISvar'

export const InformasjonFraBrukerVisning = () => {
  const { behandling } = useEtteroppgjoer()

  return !!behandling.harMottattNyInformasjon ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Har mottatt ny informasjon fra bruker</Label>
        <BodyShort>{behandling.harMottattNyInformasjon === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
      </VStack>
      {!!behandling.endringErTilUgunstForBruker && (
        <VStack gap="2">
          <Label>Endring er til ugunst for bruker</Label>
          <BodyShort>{behandling.endringErTilUgunstForBruker === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
      )}
      {!!behandling.beskrivelseAvUgunst && (
        <VStack gap="2" maxWidth="30rem">
          <Label>Beskrivelse av hvorfor endringen er til ugunst for bruker</Label>
          <BodyShort>{behandling.beskrivelseAvUgunst}</BodyShort>
        </VStack>
      )}
    </VStack>
  ) : (
    <Heading size="small">Spørsmål om endring fra bruker er ikke besvart</Heading>
  )
}
