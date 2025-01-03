import { BodyShort, Box } from '@navikt/ds-react'
import React from 'react'
import { BodyShortProps } from '@navikt/ds-react/src/typography/BodyShort'
import { OverridableComponent } from '@navikt/ds-react/src/util/types'

export const TekstMedBeholdtWhitespace: OverridableComponent<BodyShortProps, HTMLParagraphElement> = (
  props: BodyShortProps
) => {
  return (
    <Box maxWidth="42.5rem">
      <BodyShort style={{ whiteSpace: 'pre-wrap' }} {...props} />
    </Box>
  )
}
