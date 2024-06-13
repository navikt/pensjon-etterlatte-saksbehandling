import React, { ReactNode } from 'react'
import { HelpText, HStack } from '@navikt/ds-react'

export const EtterbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HStack gap="2">
      Skal det etterbetales?
      <HelpText placement="top">
        Velg ja hvis ytelsen er innvilget tilbake i tid og det blir utbetalt mer enn ett månedsbeløp. Da skal du
        registrere perioden fra innvilgelsesmåned til og med måneden som er klar for utbetaling. Vedlegg om
        etterbetaling skal da bli med i brevet.
      </HelpText>
    </HStack>
  )
}
