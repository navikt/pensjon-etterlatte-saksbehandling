import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'

export const FaktiskInntektVisning = () => {
  const { faktiskInntekt } = useEtteroppgjoer()

  return !!faktiskInntekt ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Lønnsinntekt</Label>
        <BodyShort>{new Intl.NumberFormat('nb').format(faktiskInntekt.loennsinntekt)}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Avtalefestet pensjon</Label>
        <BodyShort>{new Intl.NumberFormat('nb').format(faktiskInntekt.afp)}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Næringsinntekt</Label>
        <BodyShort>{new Intl.NumberFormat('nb').format(faktiskInntekt.naeringsinntekt)}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Inntekt fra utland</Label>
        <BodyShort>{new Intl.NumberFormat('nb').format(faktiskInntekt.utland)}</BodyShort>
      </VStack>

      <SumAvFaktiskInntekt faktiskInntekt={faktiskInntekt} />

      <VStack gap="2" maxWidth="30rem">
        <Label>Spesifikasjon av inntekt</Label>
        <Label>{faktiskInntekt.spesifikasjon}</Label>
      </VStack>
    </VStack>
  ) : (
    <Heading size="small">Faktisk inntekt er ikke fastsatt</Heading>
  )
}
