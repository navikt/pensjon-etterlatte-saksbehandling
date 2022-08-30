import styled from 'styled-components'
import { ErrorMessage } from '@navikt/ds-react'

interface Props {
  error?: string
}

export const ErrorResponse = ({ error = 'Det skjedde en feil' }: Props) => {
  return (
    <ErrorContainer>
      <ErrorMessage>{error}</ErrorMessage>
    </ErrorContainer>
  )
}

const ErrorContainer = styled.div`
  padding: 2rem;
`
