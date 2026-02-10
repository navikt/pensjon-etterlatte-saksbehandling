import React, { ReactNode } from 'react'
import { HelpText, HStack } from '@navikt/ds-react'

export const FeilutbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HStack gap="space-2">
      Medfører revurderingen en feilutbetaling?
      <HelpText placement="top">
        Hvis ja: Vurder om det skal sendes varsel om eventuell tilbakekreving (TK) av feilutbetalingen (FU). Velger du
        &quot;ja, det skal sendes varsel&quot; blir varselet vedlagt revurderingsvedtaket. I noen tilfeller skal ikke FU
        kreves tilbakebetalt fordi beløpet er under 4 rettsgebyr, jf. folketrygdloven § 22-15 sjette ledd. Vurder nøye.
        Dette valget gir tekst i revurderingsvedtaket om at FU ikke blir krevd tilbakebetalt.
      </HelpText>
    </HStack>
  )
}
