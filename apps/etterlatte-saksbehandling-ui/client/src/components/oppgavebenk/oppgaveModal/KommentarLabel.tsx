import React from 'react'
import { HelpText, HStack } from '@navikt/ds-react'

export const KommentarLabel = () => (
  <HStack gap="1">
    Kommentar
    <HelpText>
      Legg til kommentar hvis du avslutter oppgaven. Det er ikke nødvendig dersom du oppretter revurdering.
    </HelpText>
  </HStack>
)
