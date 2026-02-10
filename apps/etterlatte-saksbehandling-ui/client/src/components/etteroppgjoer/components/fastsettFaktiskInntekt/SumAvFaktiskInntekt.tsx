import { FaktiskInntekt } from '~shared/types/EtteroppgjoerForbehandling'
import { Label, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'

export const SumAvFaktiskInntekt = ({ faktiskInntekt }: { faktiskInntekt: FaktiskInntekt }) => {
  const sumAvFaktiskInntektStringBulder = (faktiskInntekt: FaktiskInntekt) => {
    let inntekt = 0

    if (isNaN(faktiskInntekt.loennsinntekt)) inntekt += 0
    else inntekt += faktiskInntekt.loennsinntekt

    if (isNaN(faktiskInntekt.afp)) inntekt += 0
    else inntekt += faktiskInntekt.afp

    if (isNaN(faktiskInntekt.naeringsinntekt)) inntekt += 0
    else inntekt += faktiskInntekt.naeringsinntekt

    if (isNaN(faktiskInntekt.utlandsinntekt)) inntekt += 0
    else inntekt += faktiskInntekt.utlandsinntekt

    return NOK(inntekt)
  }

  return (
    <VStack gap="space-2" padding="space-2" minWidth="25rem" maxWidth="fit-content">
      <Label>Sum</Label>
      <Label>= {sumAvFaktiskInntektStringBulder(faktiskInntekt)}</Label>
    </VStack>
  )
}
