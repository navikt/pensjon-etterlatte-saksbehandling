import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { JaNei } from '~shared/types/ISvar'

export const InformasjonFraBrukerVisning = () => {
  const { behandling } = useEtteroppgjoerForbehandling()

  return !!behandling.harMottattNyInformasjon ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?</Label>
        <BodyShort>{behandling.harMottattNyInformasjon === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
      </VStack>
      {!!behandling.endringErTilUgunstForBruker && (
        <VStack gap="2">
          <Label>Er endringen til ugunst for bruker?</Label>
          <BodyShort>{behandling.endringErTilUgunstForBruker === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
      )}
      {!!behandling.beskrivelseAvUgunst && (
        <VStack gap="2" maxWidth="30rem">
          <Label>Beskriv hvorfor endringen er til ugunst for bruker</Label>
          <BodyShort>{behandling.beskrivelseAvUgunst}</BodyShort>
        </VStack>
      )}
    </VStack>
  ) : (
    <Heading size="small">Spørsmål om endring fra bruker er ikke besvart</Heading>
  )
}
