import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { JaNei } from '~shared/types/ISvar'

export const InformasjonFraBrukerVisning = () => {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  return !!forbehandling.harMottattNyInformasjon ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?</Label>
        <BodyShort>{forbehandling.harMottattNyInformasjon === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
      </VStack>
      {!!forbehandling.endringErTilUgunstForBruker && (
        <VStack gap="2">
          <Label>Er endringen til ugunst for bruker?</Label>
          <BodyShort>{forbehandling.endringErTilUgunstForBruker === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
      )}
      {!!forbehandling.beskrivelseAvUgunst && (
        <VStack gap="2" maxWidth="30rem">
          <Label>Beskriv hvorfor endringen er til ugunst for bruker</Label>
          <BodyShort>{forbehandling.beskrivelseAvUgunst}</BodyShort>
        </VStack>
      )}
    </VStack>
  ) : (
    <Heading size="small">Spørsmål om endring fra bruker er ikke besvart</Heading>
  )
}
