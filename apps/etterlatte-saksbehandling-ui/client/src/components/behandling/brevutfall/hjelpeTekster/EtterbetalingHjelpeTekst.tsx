import React, { ReactNode } from 'react'
import { HjelpeTekst } from '~shared/components/hjelpeTekst/HjelpeTekst'
import { HelpText } from '@navikt/ds-react'

export const EtterbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HjelpeTekst>
      Skal det etterbetales?
      <HelpText strategy="fixed">
        Velg ja hvis ytelsen er innvilget tilbake i tid og det blir utbetalt mer enn ett månedsbeløp. Da skal du
        registrere perioden fra innvilgelsesmåned til og med måneden som er klar for utbetaling. Vedlegg om
        etterbetaling skal da bli med i brevet.
      </HelpText>
    </HjelpeTekst>
  )
}
