import { FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { Box, Heading } from '@navikt/ds-react'

export const SumAvFaktiskInntekt = ({ faktiskInntekt }: { faktiskInntekt: FaktiskInntekt }) => {
  const sumAvFaktiskInntektStringBulder = (faktiskInntekt: FaktiskInntekt) => {
    let inntekt = 0

    if (isNaN(faktiskInntekt.loennsinntekt)) inntekt += 0
    else inntekt += faktiskInntekt.loennsinntekt

    if (isNaN(faktiskInntekt.afp)) inntekt += 0
    else inntekt += faktiskInntekt.afp

    if (isNaN(faktiskInntekt.naeringsinntekt)) inntekt += 0
    else inntekt += faktiskInntekt.naeringsinntekt

    if (isNaN(faktiskInntekt.utland)) inntekt += 0
    else inntekt += faktiskInntekt.utland

    return `${new Intl.NumberFormat('nb').format(inntekt)} kr`
  }

  return (
    <Box background="surface-subtle" padding="6" borderRadius="xlarge" minWidth="25rem" maxWidth="fit-content">
      <Heading size="medium">Sum = {sumAvFaktiskInntektStringBulder(faktiskInntekt)}</Heading>
    </Box>
  )
}
