import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'

export const FaktiskInntektVisning = () => {
  const { faktiskInntekt } = useEtteroppgjoer()

  return !!faktiskInntekt ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Lønnsinntekt</Label>
        <BodyShort>{`${new Intl.NumberFormat('nb').format(faktiskInntekt.loennsinntekt)} kr`}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Avtalefestet pensjon</Label>
        <BodyShort>{`${new Intl.NumberFormat('nb').format(faktiskInntekt.afp)} kr`}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Næringsinntekt</Label>
        <BodyShort>{`${new Intl.NumberFormat('nb').format(faktiskInntekt.naeringsinntekt)} kr`}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Inntekt fra utland</Label>
        <BodyShort>{`${new Intl.NumberFormat('nb').format(faktiskInntekt.utlandsinntekt)} kr`}</BodyShort>
      </VStack>

      <SumAvFaktiskInntekt faktiskInntekt={faktiskInntekt} />

      <VStack gap="2" maxWidth="30rem">
        <Label>Spesifikasjon av inntekt</Label>
        <BodyShort>{faktiskInntekt.spesifikasjon}</BodyShort>
      </VStack>
    </VStack>
  ) : (
    <Heading size="small">Faktisk inntekt er ikke fastsatt</Heading>
  )
}
