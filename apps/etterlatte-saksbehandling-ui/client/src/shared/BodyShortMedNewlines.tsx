import { BodyShort } from '@navikt/ds-react'
import styled from 'styled-components'
import React from 'react'
import { BodyShortProps } from '@navikt/ds-react/src/typography/BodyShort'
import { OverridableComponent } from '@navikt/ds-react/src/util/types'

const BodyShortSomViserNewlines = styled(BodyShort)`
  white-space: pre-line;
`

export const BodyShortMedNewlines: OverridableComponent<BodyShortProps, HTMLParagraphElement> = (
  props: BodyShortProps
) => {
  return <BodyShortSomViserNewlines {...props} />
}
