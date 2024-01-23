import React, { ReactNode } from 'react'
import styled from 'styled-components'
import { Alert } from '@navikt/ds-react'

export const ApiErrorAlert = ({ children }: { children: string }): ReactNode => {
  return <ErrorAlert variant="error">{children}</ErrorAlert>
}

const ErrorAlert = styled(Alert)`
  margin: 2rem auto;
  max-width: fit-content;
`
