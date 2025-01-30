import React, { ReactNode } from 'react'
import { HelpText, HStack } from '@navikt/ds-react'

export const EtterbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HStack gap="2">
      Skal det etterbetales?
      <HelpText placement="top">
        Velg ja hvis ytelsen er innvilget tilbake i tid og det blir utbetalt mer enn ett månedsbeløp. Da skal du
        registrere perioden fra innvilgelsesmåned (eventuelt første virkningstidspunkt i endringen/økningen av stønaden)
        til og med nåværende måned dersom normal utbetalingsdato er passert. Det vil si at inneværende måned skal være
        med som etterbetaling dersom dette er hensiktsmessig med tanke på når stønaden blir utbetalt. Simuleringen
        ovenfor viser nåværende måned som etterbetaling dersom normal utbetalingsdato er passert.
      </HelpText>
    </HStack>
  )
}
