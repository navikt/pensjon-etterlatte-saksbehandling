import { BodyShort } from '@navikt/ds-react'
import styled from 'styled-components'

const BodyShortSomViserNewlines = styled(BodyShort)`
  white-space: pre-line;
`

export const BodyShortMedNewlines = ({ children }: { children: string }) => {
  return <BodyShortSomViserNewlines>{children}</BodyShortSomViserNewlines>
}
