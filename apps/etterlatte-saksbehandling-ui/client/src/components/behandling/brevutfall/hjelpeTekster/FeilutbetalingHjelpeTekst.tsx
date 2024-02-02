import React, { ReactNode } from 'react'
import { HjelpeTekst } from '~shared/components/hjelpeTekst/HjelpeTekst'
import { HelpText } from '@navikt/ds-react'

export const FeilutbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HjelpeTekst>
      Medfører revurderingen en feilutbetaling?
      <HelpText strategy="fixed">todo: passende hjelpetekst</HelpText>
    </HjelpeTekst>
  )
}
