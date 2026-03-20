import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { JaNei } from '~shared/types/ISvar'

export const InformasjonFraBrukerVisning = () => {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  return !!forbehandling.harMottattNyInformasjon ? (
    <VStack gap="space-16">
      <VStack gap="space-8">
        <Label>Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?</Label>
        <BodyShort>{forbehandling.harMottattNyInformasjon === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
      </VStack>
      {!!forbehandling.endringErTilUgunstForBruker && (
        <VStack gap="space-8">
          <Label>Er endringen til ugunst for bruker fordi det er Nav som har oppdaget feilen?</Label>
          <BodyShort>{forbehandling.endringErTilUgunstForBruker === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
      )}
      {!!forbehandling.beskrivelseAvUgunst && (
        <VStack gap="space-8" maxWidth="30rem">
          <Label>Beskriv hvorfor endringen er til ugunst for bruker</Label>
          <BodyShort>{forbehandling.beskrivelseAvUgunst}</BodyShort>
        </VStack>
      )}
    </VStack>
  ) : (
    <Heading size="small">Spørsmål om endring fra bruker er ikke besvart</Heading>
  )
}
