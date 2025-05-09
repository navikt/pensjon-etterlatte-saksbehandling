import { FaktiskInntekt } from '~shared/types/EtteroppgjoerForbehandling'
import { Label, VStack } from '@navikt/ds-react'

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

    return `${new Intl.NumberFormat('nb').format(inntekt)} kr`
  }

  return (
    <VStack gap="2" padding="2" minWidth="25rem" maxWidth="fit-content">
      <Label>Sum</Label>
      <Label>= {sumAvFaktiskInntektStringBulder(faktiskInntekt)}</Label>
    </VStack>
  )
}
