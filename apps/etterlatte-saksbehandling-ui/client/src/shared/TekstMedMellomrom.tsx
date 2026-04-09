import { BodyShort, Box, type BodyShortProps, type OverridableComponent } from '@navikt/ds-react'
import React from 'react'

export const TekstMedMellomrom: OverridableComponent<BodyShortProps, HTMLParagraphElement> = (
  props: BodyShortProps
) => {
  return (
    <Box maxWidth="42.5rem">
      <BodyShort style={{ whiteSpace: 'pre-wrap' }} {...props} />
    </Box>
  )
}
