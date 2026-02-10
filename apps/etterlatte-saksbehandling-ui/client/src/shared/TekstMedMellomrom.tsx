import { BodyShort, Box } from '@navikt/ds-react'
import React from 'react'
import { BodyShortProps } from '@navikt/ds-react/src/typography/BodyShort'

export const TekstMedMellomrom = (props: BodyShortProps) => {
  return (
    <Box maxWidth="42.5rem">
      <BodyShort style={{ whiteSpace: 'pre-wrap' }} {...props} />
    </Box>
  )
}
