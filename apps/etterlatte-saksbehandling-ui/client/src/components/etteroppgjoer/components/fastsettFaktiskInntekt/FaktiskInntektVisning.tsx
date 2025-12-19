import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { NOK } from '~utils/formatering/formatering'
import { TekstMedMellomrom } from '~shared/TekstMedMellomrom'

export const FaktiskInntektVisning = () => {
  const { faktiskInntekt } = useEtteroppgjoerForbehandling()

  return !!faktiskInntekt ? (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Lønnsinntekt</Label>
        <BodyShort>{NOK(faktiskInntekt.loennsinntekt)}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Avtalefestet pensjon</Label>
        <BodyShort>{NOK(faktiskInntekt.afp)}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Næringsinntekt</Label>
        <BodyShort>{NOK(faktiskInntekt.naeringsinntekt)}</BodyShort>
      </VStack>
      <VStack gap="2">
        <Label>Inntekt fra utland</Label>
        <BodyShort>{NOK(faktiskInntekt.utlandsinntekt)}</BodyShort>
      </VStack>

      <SumAvFaktiskInntekt faktiskInntekt={faktiskInntekt} />

      <VStack gap="2" maxWidth="30rem">
        <Label>Spesifikasjon av inntekt</Label>
        <TekstMedMellomrom>{faktiskInntekt.spesifikasjon}</TekstMedMellomrom>
      </VStack>
    </VStack>
  ) : (
    <Heading size="small">Faktisk inntekt er ikke fastsatt</Heading>
  )
}
