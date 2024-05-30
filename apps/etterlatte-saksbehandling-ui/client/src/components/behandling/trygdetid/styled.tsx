import styled from 'styled-components'
import { Textarea } from '@navikt/ds-react'

export const FormWrapper = styled.div`
  display: flex;
  gap: 1rem;
  margin-right: 1em;
`

export const FormKnapper = styled.div`
  margin-top: 1rem;
  margin-right: 1em;
  gap: 1rem;
  button {
    margin-right: 1em;
  }
`

export const Begrunnelse = styled(Textarea)`
  width: 15rem;
`
