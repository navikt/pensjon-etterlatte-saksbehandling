import styled from 'styled-components'
import { VStack } from '@navikt/ds-react'

// TODO: Fjerne når Aksel har fått på plass width som prop
export const SmalVStack = styled(VStack)`
  width: 30rem;
`

export const VurderingWrapper = styled.div`
  margin-bottom: 2rem;
  width: 30rem;
`

export const BredVurderingWrapper = styled.div`
  margin-bottom: 2rem;
  width: 50rem;
`
