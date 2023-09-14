import styled from 'styled-components'
import { BodyShort } from '@navikt/ds-react'

export const Innhold = styled.div`
  padding: 2em 2em 2em 4em;
`

export const VurderingWrapper = styled.div`
  margin-bottom: 2rem;

  width: 30rem;
`

export const Feilmelding = styled(BodyShort)`
  color: var(--a-text-danger);
`
