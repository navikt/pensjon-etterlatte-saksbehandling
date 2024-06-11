import React, { ReactNode } from 'react'
import { HelpText, HStack } from '@navikt/ds-react'

export const LavEllerIngenInntektHjelpeTekst = (): ReactNode => {
  return (
    <HStack gap="2">
      Gi omstillingsstønad til 67 år etter unntaksregel for bruker født tom 1963?
      <HelpText placement="top">
        Hvis bruker er født i 1963 eller tidligere, og har hatt lav inntekt siste fem år før dødsfallet, kan bruker få
        omstillingsstønad frem til man fyller 67 år. Se ftrl. § 17-5 tredje ledd.
      </HelpText>
    </HStack>
  )
}
